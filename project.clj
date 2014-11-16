(defproject todomvc "0.1.0-SNAPSHOT"
  :description "Walkthrough of web UI dev with quiescent & light table"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2227"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [quiescent "0.1.4"]
                 [com.facebook/react "0.11.2"]]

  :plugins [[lein-cljsbuild "1.0.3"]]

  :cljsbuild {
              :builds
              {:dev {:source-paths ["src"]
                     :compiler
                     {:output-to "todomvc.js"
                      :output-dir "out"
                      :optimizations :none
                      :source-map true}}
               :prod {:source-paths ["src"]
                      :compiler
                      {:output-to "todomvc-prod.js"
                       :optimizations :advanced
                       :preamble ["react/react.min.js"]
                       :externs ["react/externs/react.js"]
                       :pretty-print false
                       :closure-warnings {:non-standard-jsdoc :off}}}
               }})
