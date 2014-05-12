(defproject overtone-server "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [overtone "0.9.1"]
                 [jfugue "4.0.3"]
                 ]
  :main ^:skip-aot overtone-server.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
