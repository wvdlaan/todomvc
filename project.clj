(defproject todomvc "0.1.0-SNAPSHOT"
  :description "Walkthrough of web UI dev with quiescent & light table"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2173"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [quiescent "0.1.1"]]

  :plugins [[lein-cljsbuild "1.0.2"]]

  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "todomvc"
              :source-paths ["src"]
              :compiler {
                :output-to "todomvc.js"
                :output-dir "out"
                :optimizations :none
                :source-map true}}]})
