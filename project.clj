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
                 ]

  :uberjar-name "atpshowbot.jar"
  :main ^:skip-aot atpshowbot.core
  )
