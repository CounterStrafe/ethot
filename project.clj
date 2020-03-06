(defproject ethot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/data.json "1.0.0"]
                 [org.suskalo/discljord "0.2.5"]
                 [com.taoensso/carmine "2.19.1"]
                 [clj-http "3.10.0"]
                 [hickory "0.7.1"]
                 [yogthos/config "1.1.7"]]
  ;; configuration will be read from the dev-config.edn file
  :jvm-opts ["-Dconfig=secrets.edn"]
  :main ethot.core
  :aot [ethot.core]
  :repl-options {:init-ns ethot.core})
