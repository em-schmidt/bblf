(ns bblf.runtime
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [taoensso.timbre :as log]))

(def lambda-timeout 900000)

(defn- get-runtime-event
  [runtime-api]
  (http/get (str "http://" runtime-api "/runtime/invocation/next") {:timeout lambda-timeout}))

(defn- send-runtime-response
  [runtime-api request-id body]
  (http/post (str "http://" runtime-api "/runtime/invocation/" request-id "/response") 
             {:headers {"Content-type" "application/json"}
              :body (json/generate-string body)}))

(defn- send-runtime-error
  [runtime-api request-id body]
  (http/post (str "http://" runtime-api "/runtime/invocation/" request-id "/error") 
             {:headers {"Content-type" "application/json"}
              :body (json/generate-string body)}))

(defn- call-handler
  [f req]
  (log/info "calling handler" {:handler f
                               :req req})
  true)

(defn -main [_]
  (let [handler (System/getenv "_HANDLER")
        runtime-api (str (System/getenv "AWS_LAMBDA_RUNTIME_API") "/2018-06-01")]
      (log/info "initializing runtime handler loop"{:handler handler
                                                    :runtime-api runtime-api})
      (loop [req (get-runtime-event runtime-api)]
        (let [request-id (-> req :headers (get "lambda-runtime-aws-request-id"))
              status (-> req :status)]
          (log/info "got request" {:request-id request-id
                                   :status status})
          (try (send-runtime-response 
                 runtime-api
                 request-id 
                 (call-handler handler req))
               (catch Exception e
                 (send-runtime-error runtime-api request-id {:id request-id :error (.getMessage e) :stacktrace (mapv str (.getStackTrace e))}))))
        (recur (get-runtime-event runtime-api)))))

(comment)
  
