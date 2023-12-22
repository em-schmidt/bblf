(ns bblf.tools
  (:require [babashka.http-client :as http]
            [babashka.fs :as fs]
            [babashka.process :as p]
            [taoensso.timbre :as log]))

(defn clean
  [_]
  (let [path "target"]
    (fs/delete-tree path)
    (log/info "cleaned" {:path path})))

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
    ()
   (log/info "in untar" {:cmd cmd
                         :stream stream
                         :dest-dir dest-dir}
    @(p/process
        {:in stream :out :string}
        cmd)
    (log/info "done with tar"))))

(defn fetch-babashka
  [dest-dir version arch]
  (log/info "fetch-babashka" 
            {:source-url (bb-url version arch)
             :dest-dir dest-dir
             :version version
             :arch arch})
  (let [response (http/get (bb-url version arch) {:as :stream})]
    (if (= 200 (:status response))
      (try (untar-stream (:body response) dest-dir)
           (catch Exception e
            (log/error "error untarring" {:exception e})))
      (log/error "error fetching" {:response response}))))

(defn build
  [_]
  ;; download babashka, dependencies,pods, etc to temp dir
  ;; arrange and zip up downloaded files
  ;; place zip file in target dir

  ;;(let bb-dest (str (fs/canonicalize (fs/create-dir "target"))))
  (let [target "target"
        targetpath (fs/canonicalize (fs/path target))]
      (if (fs/exists? targetpath)
        (log/info "path exists" targetpath)
        (fs/create-dir targetpath))
      (fs/with-temp-dir [tempdir {}]
        (log/info "build dirs" {:target target :tempdir tempdir})
        (log/info "the tempdir" {:tempdir tempdir})
        (fetch-babashka (str tempdir) "1.2.174" "linux-aarch64-static")
        (map #(println (str %)) (fs/list-dir tempdir)))))


(defn main []
  (build nil))

(comment
  (-> (http/get "https://www.google.com/")
      :body)

  (clean nil)
  (fs/cwd)
  (build nil))

