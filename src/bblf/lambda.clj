(ns bblf.lambda
  (:require [taoensso.timbre :as log]
            [babashka.fs :as fs]
            [clojure.java.io :as io]
            [com.grzm.awyeah.client.api :as aws]))

(defn lf-bucket
  "check and return function bucket"
  [bucket-name]
  (log/info "get lambda bucket")
  (let [s3 (aws/client {:api :s3})]
    (aws/invoke s3 {:op :HeadBucket
                    :request {:Bucket bucket-name}})))

(defn put-s3-artifact
  "puts a given file into s3 bucket path"
  [artifact-path {:keys [fn-bucket fn-name]}]
  (if (fs/exists? artifact-path)
    (let [s3 (aws/client {:api :s3})
          s3-key (str fn-name "/function.zip")]
      (aws/invoke s3 {:op :PutObject 
                      :request {:Bucket fn-bucket
                                :Key s3-key
                                :Body (io/input-stream artifact-path)}})
      (log/info "put artifact in bucket"  {:localpath artifact-path
                                           :S3Bucket fn-bucket
                                           :S3Key s3-key})
      {:S3Bucket fn-bucket
       :S3Key s3-key})
    (throw (ex-info "artifact not found" {:path artifact-path}))))

(defn delete-s3-artifact
  "removes a given key from s3"
  [{:keys [S3Bucket S3Key]}]
  (let [s3 (aws/client {:api :s3})]
    (log/info "delete artifact from s3")
    (aws/invoke s3 {:op :DeleteObject
                    :request {:Bucket S3Bucket
                              :Key S3Key}})))

(defn create-lf
  "create lambda function"
  [sourcefile {:keys [fn-name fn-desc fn-role-arn] :as opts}]
  (log/info "create lambda" {:name fn-name
                             :source sourcefile})
  (let [lambda (aws/client {:api :lambda})
        s3-artifact (put-s3-artifact sourcefile opts)]
    (aws/invoke lambda {:op :CreateFunction
                        :request {:FunctionName fn-name
                                  :Runtime "provided.al2" :Role fn-role-arn
                                  :Handler "index.handler" :Code s3-artifact
                                  :Description fn-desc :Timeout 3 :MemorySize 128}})))

(defn update-lf
  "update lambda function"
  []
  (log/info "update lambda")
  (let [lambda (aws/client {:api :lambda})]
    (aws/invoke lambda {:op :Invoke})
    false))

(defn call-lf
  "call lambda function"
  [{:keys [FunctionName] :as opts}]
  (log/info "call lambda" opts)
  (let [lambda (aws/client {:api :lambda})]
    (-> (aws/invoke lambda {:op :Invoke
                            :request {:FunctionName FunctionName}})
        :Payload
        slurp)))

(defn delete-lf
  "delete function"
  [{:keys [FunctionName] :as opts}]
  (log/info "delete lambda" opts)
  (let [lambda (aws/client {:api :lambda})
        request {:FunctionName FunctionName}]
    (aws/invoke lambda {:op :DeleteFunction
                        :request request})))

(defn list-fns
  "list functions"
  []
  (let [lambda (aws/client {:api :lambda})]
    (->> (aws/invoke lambda {:op :ListFunctions})
         :Functions
         (mapv :FunctionName))))

(comment

  (require '[portal.api :as p])
  (def p (p/open))
  (add-tap #'p/submit)

  (lf-bucket "bblf-fns")

  ;; given config, put object in appropriate s3 path
  ;; config needs, bucket name, function name

  (def opts {:fn-name "test"
             :fn-desc "test description"
             :fn-bucket "bblf-fns"
             :fn-role-arn "arn:aws:iam::325274606117:role/lambda_basic_execution"})

  ;; list lambda functions
  (list-fns)

  ;; create and delete s3 artifacts
  (-> (put-s3-artifact "target/function.zip" opts)
      delete-s3-artifact)

  ;; create and delete lambda function
  (-> (create-lf "target/function.zip" opts)
      delete-lf)

  (create-lf "target/function.zip" opts)

  (delete-lf {:FunctionName (:fn-name opts)})

    ;; call function
  (call-lf {:FunctionName "test"}))

