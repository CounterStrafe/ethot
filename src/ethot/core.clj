(ns ethot.core
  (:require [discljord.connections :as dconn]
            [discljord.messaging :as dmess]
            [discljord.events :as devent]
            [clojure.core.async :as async]
            [clojure.string :as str]
            [clj-http.client :as hclient]
            [ethot.ebot :as ebot]
            [ethot.toornament :as toornament])
  (:gen-class))

(def state (atom {}))

(defn start-tournament
  [name]
  (let [tournament (toornament/get-tournament name)
        ebot-cookies (:cookies (ebot/ebot-login))]
    (println "\n;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;")
    (println ";         Step 1: Get the Tournament from Toornament         ;")
    (println ";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\n")

    (println tournament)

    (println "\n;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;")
    (println ";     Step 2: Get the Tournament Stages from Toornament      ;")
    (println ";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\n")

    (println (toornament/stages (get tournament "id")))

    (println "\n;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;")
    (println ";              Step 3: Log into eBot Admin Page              ;")
    (println ";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\n")

    (println (hclient/get (str ebot/ebot-url "/admin.php/guard/login") {:connection-manager ebot/ebot-cm :cookies ebot-cookies}))))

(defn -main
  [& args]
  (start-tournament "Test"))
