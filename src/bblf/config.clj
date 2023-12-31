(ns bblf.config
  (:require [babashka.fs :as fs]
            [clojure.string :as str]))

(defn default-bb-version
  "latest or current?"
  []
  (System/getProperty "babashka.version"))

(defn default-bb-arch
  "linux-intel"
  []
  (or "linux-amd64-static"
      "linux-aarch64-static"))

(defn default-fn-name
  "default function name"
  []
  (or
    (fs/file-name (fs/cwd))
    "fallback"))

(defn default-bucket-name
  "default bucket name"
  []
  "bblf-fns")

(defn default-role-arn
  "default role arn"
  []
  "arn:aws:iam::325274606117:role/lambda_basic_execution")

(defn default-config
  []
  {:babashka
   {:version (default-bb-version)
    :arch (default-bb-arch)}
   :lambda
   {:FunctionName (default-fn-name)
    :Bucket (default-bucket-name)
    :RoleArn (default-role-arn)}})

(defn get-config
  "get config from file"
  [{:keys [config-file]}]
  (if (fs/exists? config-file) 
      (read-string (slurp "bblf.edn"))
      (default-config)))

(defn- flatten-config
  ([m]
   (flatten-config m []))
  ([m path]
   (->> (map (fn [[k v]]
               (if (and (map? v) (not-empty v))
                 (flatten-config v (conj path k))
                 [(->> (conj path k)
                       (map name)
                       (str/join ".")
                       keyword) v])) m)
        (into {}))))

(defn display-config
 "display config for cli"
 [opts]
 (->> (get-config opts)
      flatten-config 
      (run! (fn [[k v]]
              (println (name k) "=" v)))))

(defn- config-val-to-map
  "translate from cli config keys in dot separated nodes to a nested map structure"
  [k v]
  (let [keys (str/split (str k) #"\.")
        keys (map keyword keys)]
   (assoc-in {} keys v)))

(defn write-config-file
 "write config to file"
 [{:keys [config-file config]}]
 (spit config-file (pr-str config)))

(defn update-config
  "update config with value from cli"
  [{:keys [config-key config-val config-file]}]
  (let [config (get-config config-file)
        config-key (map keyword (str/split (str config-key) #"\."))
        new-config (assoc-in config config-key config-val)]
    (write-config-file
      {:config-file config-file
       :config new-config}))) 

(comment
  (write-config-file {:config-file "bblf.edn"})

  (get-config "bblf.edn")

  (update-config {:config-file "bblf.edn" :config-key "babashka.version" :config-val "YUM"})

  (config-val-to-map "babaska.version" "YUM")

  (display-config nil))

