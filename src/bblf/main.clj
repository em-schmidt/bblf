(ns bblf.main
  (:require [babashka.fs :as fs]
            [cli-matic.core :refer [run-cmd]]))

(defn clean
  [_]
  (let [path "target"]
    (fs/delete-tree path))) 

(def cli-config
  {:command "bblf"
   :description "BaBashka Lambda Function utilities"
   :subcommands
   [{
     :command "clean"
     :description "make clean"
     :runs clean}]})

(defn -main
  [& args]
  (run-cmd args cli-config))
