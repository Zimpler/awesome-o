(defproject awesome-o "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://awesome-o.herokuapp.com"
  :license {:name "FIXME: choose"
            :url "http://example.com/FIXME"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [environ "1.0.0"]
                 [http-kit "2.3.0"]
                 [cheshire "5.8.1"]
                 [instaparse "1.4.1"]
                 [com.taoensso/carmine "2.11.1"]
                 [clj-time "0.10.0"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [ring/ring-codec "1.1.2"]
                 [uswitch/lambada "0.1.2"]]
  :min-lein-version "2.0.0"
  :plugins [[lein-environ "1.0.0"]
            [lein-auto "0.1.2"]]
  :main awesome-o.web
  :uberjar-name "awesome-o-standalone.jar"
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.11"]]}
             :production {:env {:production false}}
             :uberjar {:aot :all}})
