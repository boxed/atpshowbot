(defproject atpshowbot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.3.4"
  :dependencies [
                 [org.clojure/clojure "1.5.1"]
                 [ring/ring-jetty-adapter "1.1.6"]
                 [irclj "0.5.0-alpha4"]
                 [midje "1.6.0"]
                 [org.clojure/algo.generic "0.1.2"]
                 [compojure "1.1.8"]
                 [org.clojure/clojurescript "0.0-2173"]
                 [reagent "0.4.2"]
                 ]
  :plugins [[lein-cljsbuild "1.0.2"]]
  :hooks [leiningen.cljsbuild]

    :profiles {:prod {:cljsbuild
                    {:builds
                     {:client {:compiler
                               {:optimizations :simple ;:advanced
                                :preamble ^:replace ["reagent/react.min.js"]
                                :pretty-print false}}}}}
             :srcmap {:cljsbuild
                      {:builds
                       {:client {:compiler
                                 {:source-map "resources/client.js.map"
                                  :source-map-path "client"}}}}}}
  :source-paths ["src"]
  :cljsbuild {:builds {:client {:source-paths ["src"]
                                :compiler
                                {:preamble ["reagent/react.js"]
                                 :output-dir "resources/client"
                                 :output-to "resources/client.js"
                                 ;:source-map "resources/client.js.map"
                                 :pretty-print true}}}}
  :uberjar-name "atpshowbot.jar"
  :main ^:skip-aot atpshowbot.core
  )
