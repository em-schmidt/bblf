(ns bblf.runtime
  (:require [babashka.http-client :as http]
            [clojure.string :as str]
            [cheshire.core :as json]
            [taoensso.timbre :as log]))

(def lambda-timeout 900000)

(defn- get-runtime-event
  [runtime-api]
  (http/get (str "http://" runtime-api "/runtime/invocation/next") {:timeout lambda-timeout}))

(defn- send-runtime-response
  [runtime-api request-id response-type body]
  (http/post (str "http://" runtime-api "/runtime/invocation/" request-id "/" response-type) 
             {:headers {"Content-type" "application/json"}
              :body (json/generate-string body)}))

(defn- get-handler
  [f]
  (let [[hns hfn] (str/split f #"/")]
    (log/info "finding handler" {:function hfn
                                 :ns hns})
    (let [fname (or hfn "-main")
          fqfn (symbol (str hns "/" fname))]
      (require (symbol hns))
      (resolve fqfn))))

(defn -main [_]
  (let [handler (System/getenv "_HANDLER")
        runtime-api (str (System/getenv "AWS_LAMBDA_RUNTIME_API") "/2018-06-01")]
      (log/info "initializing runtime handler loop"{:handler handler
                                                    :runtime-api runtime-api})
      (let [handler-fn (get-handler handler)]
        (loop [req (get-runtime-event runtime-api)]
         (let [request-id (-> req :headers (get "lambda-runtime-aws-request-id"))
               status (-> req :status)]
           (log/info "got request" {:request-id request-id
                                    :status status})
           (try (send-runtime-response 
                  runtime-api
                  request-id
                  "response"
                  (handler-fn req))
                (catch Exception e
                  (send-runtime-response runtime-api request-id "error" {:id request-id :error (ex-message e) :data (ex-data e)})))) 
         (recur (get-runtime-event runtime-api))))))

