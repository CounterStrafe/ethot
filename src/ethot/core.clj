(ns ethot.core
  (:require [discljord.connections :as dconn]
            [discljord.messaging :as dmess]
            [discljord.events :as devent]
            [clojure.core.async :as async]
            [clojure.string :as str]
            [clj-http.client :as hclient]
            [config.core :refer [env]])
  (:gen-class))

(def state (atom {}))

(def apik (:toornament-api-key env))

(defn -main
  [& args]
  (println (str "TOORNAMENT API KEY: " apik)))
