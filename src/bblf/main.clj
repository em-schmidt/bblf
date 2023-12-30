(ns bblf.main
  (:require [bblf.tools :as tools]
            [cli-matic.core :refer [run-cmd]]))

;; TODO: - [ ] config handling
;; TODO: - [ ] lambda deploy cli 

(defn default-bb-version 
  "latest or current?"
  []
  (System/getProperty "babashka.version"))

(defn default-bb-arch
  "linux-intel"
  []
  "linux-amd64-static")

(defn default-fn-name
 "default function name"
 []
 "test")

(defn default-bucket-name
 "default bucket name"
 []
 "bblf-fns")

(defn default-role-arn
  "default role arn"
   []
   "arn:aws:iam::325274606117:role/lambda_basic_execution")

(def cli-config
  {:command "bblf"
   :description "BaBashka Lambda Function utilities"
   :subcommands
   [{
     :command "clean"
     :description "make clean"
     :runs tools/clean}
    {
     :command "build"
     :description "build lambda zipfile artifact"
     :opts [{:option "bb-version" :short "v" :type :string  :default (default-bb-version)}
            {:option "bb-arch" :short "a" :type :string  :default (default-bb-arch)}]
     :runs tools/build}
    {
     :command "lambda"
     :description "manage lambda functions"
     :subcommands
     [{
       :command "list"
       :description "list lambda functions"
       :runs tools/list-fns}
      {
       :command "create"
       :description "create lambda function"
       :opts [{:option "FunctionName" :short "f" :type :string  :default (default-fn-name)}
              {:option "Bucket" :short "b" :type :string :default (default-bucket-name)}
              {:option "RoleArn" :short "r" :type :string :default (default-role-arn)}]
       :runs tools/create-lf}
      {
       :command "delete"
       :description "delete lambda function"
       :opts [{:option "FunctionName" :short "f" :type :string  :default (default-fn-name)}]
       :runs tools/delete-lf}
      {
       :command "call"
       :description "call lambda function"
       :opts [{:option "FunctionName" :short "f" :type :string  :default (default-fn-name)}]
       :runs tools/call-lf}]}]})

(defn -main
  [& args]
  (run-cmd args cli-config))

