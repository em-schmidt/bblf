(ns bblf.main
  (:require [bblf.tools :as tools]
            [cli-matic.core :refer [run-cmd]]))

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
     :description "make clean"
     :runs tools/build}]})

(defn -main
  [& args]
  (run-cmd args cli-config))
