(ns environ.core
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [immutant.wildfly :as wildfly]))

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

(defn- read-env-file []
  (let [env-file (io/file ".lein-env")]
    (if (.exists env-file)
      (into {} (for [[k v] (read-string (slurp env-file))]
                 [(sanitize k) v])))))

(defn- read-wildfly-context-env-file [context-path]
  (let [path     (str "/Users/edvorg/.environ/" context-path "/.lein-env")
        env-file (io/file path)]
    (if (.exists env-file)
      (into {} (for [[k v] (read-string (slurp env-file))]
                 [(sanitize k) v])))))

(defonce ^{:doc "A map of environment variables."}
  env
  (merge
   (read-wildfly-context-env-file (wildfly/context-path))
   (read-env-file)
   (read-system-env)
   (read-system-props)))
