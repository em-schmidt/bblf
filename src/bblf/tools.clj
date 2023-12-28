(ns bblf.tools
  (:require [babashka.http-client :as http]
            [babashka.deps :refer [clojure]]
            [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]))

(defn clean
  [_]
  (let [path "target"]
    (log/info "clean" {:path path})
    (fs/delete-tree path)))

(defn- bb-url
  "format the github release url for babashka"
  [version arch]
  (format
   "https://github.com/babashka/babashka/releases/download/v%s/babashka-%s-%s.tar.gz"
   version
   version
   arch))

(defn untar-stream
  [stream dest-dir]
  (let [cmd (str "tar -C " dest-dir " -xz")]
    (p/check (p/process
              {:in stream :out :string}
              cmd))))

(defn fetch-babashka
  [dest-dir version arch]
  (log/info "fetch-babashka"
            {:source-url (bb-url version arch)
             :dest-dir dest-dir
             :version version
             :arch arch})
  (let [response (http/get (bb-url version arch) {:as :stream})]
    (if (= 200 (:status response))
      (untar-stream (:body response) dest-dir)
      (log/error "error fetching" {:response response}))))

(defn fetch-deps
  "fetch dependencies"
  [dest-dir deps]
  (log/info "fetch dependencies" {:dest-dir dest-dir
                                  :deps deps})
  (let [m2-dir (fs/create-dir (fs/canonicalize (fs/path dest-dir "m2")))
        gitlib-dir (fs/create-dir (fs/canonicalize (fs/path dest-dir "gitlibs")))
        deps (into deps {:mvn/local-repo (str m2-dir)})]
    (clojure ["-Sdeps" deps "-Spath"]
             {:dir (str dest-dir)
              :env (assoc (into {} (System/getenv))
                          "GITLIBS" (str gitlib-dir))})))

;;(defn fetch-pods
;;  "fetch any required pods"
;;  [dest-dir pods]  false)

(defn build
  [{:keys [bb-arch bb-version]}]
  ;; download babashka, dependencies,pods, etc to temp dir
  ;; arrange and zip up downloaded files
  ;; place zip file in target dir
  (let [target "target"
        targetpath (fs/canonicalize (fs/path target))]
    (if (fs/exists? targetpath)
      (log/trace "path exists" targetpath)
      (fs/create-dir targetpath))
    (fs/with-temp-dir [tempdir {}]
      (fetch-babashka (str tempdir) bb-version bb-arch)
      (spit (str tempdir "/bootstrap") (slurp (io/resource "bootstrap")))
      (fetch-deps (str tempdir)
         {:deps (-> (slurp "deps.edn")
                    read-string
                   :deps)})
      ;;(fetch-pods (str tempdir) {})

      (fs/zip
       (str targetpath "/function.zip")
       [(str tempdir)]
       {:root (str tempdir)}))))

(comment
  (clean nil)
  (fs/cwd)
  (build {:bb-arch "linux-amd64-static"
          :bb-version "1.3.186"}))

