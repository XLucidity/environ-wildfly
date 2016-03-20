(ns environ.core
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import org.projectodd.wunderboss.WunderBoss))

(defn- keywordize [s]
  (-> (str/lower-case s)
      (str/replace "_" "-")
      (str/replace "." "-")
      (keyword)))

(defn- sanitize [k]
  (let [s (keywordize (name k))]
    (if-not (= k s) (println "Warning: environ key " k " has been corrected to " s))
    s))

(defn- read-system-env []
  (->> (System/getenv)
       (map (fn [[k v]] [(keywordize k) v]))
       (into {})))

(defn- read-system-props []
  (->> (System/getProperties)
       (map (fn [[k v]] [(keywordize k) v]))
       (into {})))

(defn- read-local-env-file []
  (let [env-file (io/file ".lein-env")]
    (if (.exists env-file)
      (into {} (for [[k v] (read-string (slurp env-file))]
                 [(sanitize k) v])))))

(defn- read-wildfly-context-env-file [home context]
  (let [path     (str home "/.environ/" context "/.lein-env")
        env-file (io/file path)]
    (if (.exists env-file)
      (into {} (for [[k v] (read-string (slurp env-file))]
                 [(sanitize k) v])))))

(defonce ^{:doc "A map of environment variables."}
  env
  (let [{:keys [home]
         :as   system-env}  (read-system-env)
        deployment-name     (-> (WunderBoss/options) (get "deployment-name"))
        context             (if deployment-name
                              (clojure.string/replace deployment-name #"(.*)\.war$" "$1")
                              nil)
        wildfly-context-env (if context
                              (read-wildfly-context-env-file home context)
                              {})
        local-env           (read-local-env-file)
        system-props        (read-system-props)]
    (merge
     wildfly-context-env
     local-env
     system-env
     system-props)))
