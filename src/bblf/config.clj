(ns bblf.config
  (:require [babashka.fs :as fs]
            [clojure.string :as str]))

;; TODO: write config file
;; TODO: update config values

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
  [config-file]
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
 [_]
 (->> (get-config "bblf.edn")
      flatten-config 
      (run! (fn [[k v]]
              (println k "=" v)))))

(comment
  (get-config "bblf.edn")

  (display-config nil))


