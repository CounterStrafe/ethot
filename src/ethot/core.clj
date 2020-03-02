(ns ethot.core
  (:require [discljord.connections :as c]
            [discljord.messaging :as m]
            [discljord.events :as e]
            [clojure.core.async :as a]
            [clojure.string :as s]
            [clojure.string :as str])
  (:gen-class))

(defn -main
  [& args]
  (println "hello"))
