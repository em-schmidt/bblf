(ns bblf.tools
  (:require [babashka.http-client :as http]
            [babashka.fs :as fs]
            [babashka.process :as p]
            [bblf.lambda :as lambda]
            [clojure.java.io :as io]
            [clojure.string :as str]
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


(defn- pod-arch
  [arch]
  (cond (= arch "amd64") "x86_64"
        (= arch "arm64") "aarch64"
        :else arch))

(defn fetch-pods
  [dest-dir pods arch]
  (let [[_ arch _] (str/split arch #"-")
        arch       (pod-arch arch)
        pod-dir    (str dest-dir "/pods")
        extra-env {"BABASHKA_PODS_OS_NAME" "Linux"
                   "BABASHKA_PODS_OS_ARCH" arch
                   "BABASHKA_PODS_DIR" pod-dir}]
    (log/info "fetch pods" {:pods pods
                            :env extra-env})
    (p/shell {:extra-env extra-env} "bb prepare")))

(defn copy-source
  "copy the source for packaging"
  [src-dirs dest-dir]
  (let [dest-dir (str dest-dir)
        src-dirs (or src-dirs ["src"])]
    (log/trace "copying source" {:src-dirs src-dirs
                                 :dest-dir dest-dir})
    (run! #(fs/copy-tree % (fs/path dest-dir %)) src-dirs)))

(defn prepare-uberjar
  "package bb uberjar"
  [sources dest-dir]
  (let [project-edns ["deps.edn" "bb.edn"]
        jarfile "lambda.jar"]
    (log/info "perparing uberjar" {:jarfile jarfile})
    (fs/with-temp-dir [build-temp {}]
      (let [temp-path (str build-temp)]
        (copy-source sources temp-path)
        (log/trace "copy project deps config" {:project-edns project-edns
                                               :dest temp-path})
        (run! #(fs/copy % temp-path) project-edns)
        (p/shell (str "bb uberjar " jarfile) {:dir temp-path})
        (fs/move (str temp-path "/lambda.jar") dest-dir)))))

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
      (let [dir (str tempdir)
            deps (-> (slurp "deps.edn")
                     read-string)
            bb (-> (slurp "bb.edn")
                   read-string)]
        (fetch-babashka dir bb-version bb-arch)
        (fetch-pods dir (:pods bb) bb-arch)
        (prepare-uberjar (:paths deps) dir)
        (spit (str dir "/bootstrap") (slurp (io/resource "bootstrap")))
        (fs/set-posix-file-permissions (str dir "/bootstrap") "rwxr-xr-x"))

      (fs/zip
       (str targetpath "/function.zip")
       [(str tempdir)]
       {:root (str tempdir)}))))

(defn list-fns
  [_]
  (run! println (lambda/list-fns)))

(defn create-lf
  [opts]
  (prn (lambda/create-lf "target/function.zip" opts)))

(defn delete-lf
  [opts]
  (prn (lambda/delete-lf opts)))

(defn call-lf
  [opts]
  (prn (lambda/call-lf opts)))

