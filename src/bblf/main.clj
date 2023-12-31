(ns bblf.main
  (:require [bblf.config :as config]
            [bblf.tools :as tools]
            [cli-matic.core :refer [run-cmd]]))

;; TODO: - [ ] config handling

(def config (config/default-config))

(def cli-config
  {:command "bblf"
   :description "BaBashka Lambda Function utilities"
   :subcommands
   [{:command "clean"
     :description "make clean"
     :runs tools/clean}
    {:command "build"
     :description "build lambda zipfile artifact"
     :opts [{:option "bb-version" :short "v" :type :string  :default (:version (:babashka config))}
            {:option "bb-arch" :short "a" :type :string  :default (:arch (:babashka config))}]
     :runs tools/build}
    {:command "config"
     :description "manage config"
     :subcommands
     [{:command "show"
       :description "display current config"
       :runs config/display-config}]}
    {:command "lambda"
     :description "manage lambda functions"
     :subcommands
     [{:command "list"
       :description "list lambda functions"
       :runs tools/list-fns}
      {:command "create"
       :description "create lambda function"
       :opts [{:option "FunctionName" :short "f" :type :string  :default (:FunctionName (:lambda config))}
              {:option "Bucket" :short "b" :type :string :default (:Bucket (:lambda config))}
              {:option "RoleArn" :short "r" :type :string :default (:RoleArn (:lambda config))}]
       :runs tools/create-lf}
      {:command "delete"
       :description "delete lambda function"
       :opts [{:option "FunctionName" :short "f" :type :string  :default (:FunctionName (:lambda config))}]
       :runs tools/delete-lf}
      {:command "call"
       :description "call lambda function"
       :opts [{:option "FunctionName" :short "f" :type :string  :default (:FunctionName (:lambda config))}]
       :runs tools/call-lf}]}]})

(defn -main
  [& args]
  (run-cmd args cli-config))

