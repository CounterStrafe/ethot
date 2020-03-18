(ns ethot.core
  (:require [discljord.connections :as dconn]
            [discljord.messaging :as dmess]
            [discljord.events :as devent]
            [clojure.core.async :as async]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [clj-http.client :as hclient]
            [ethot.ebot :as ebot]
            [ethot.toornament :as toornament])
  (:gen-class))

(def state (atom {}))

(defn start-tournament
  [name]
  (println "\n;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;")
  (println ";              Step 1: Log into eBot Admin Page              ;")
  (println ";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\n")

  (let [ebot-cookies (:cookies (ebot/ebot-login))]
    (println (hclient/get (str ebot/ebot-url "/admin.php/guard/login")
                          {:connection-manager ebot/ebot-cm :cookies ebot-cookies}))

    (println "\n;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;")
    (println ";         Step 2: Get the Tournament from Toornament         ;")
    (println ";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\n")

    (def tournament (toornament/get-tournament name))
    (def tournament-id (get tournament "id"))
    (pp/pprint tournament)

    (println "\n;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;")
    (println ";    Step 3: Get the Importable Matches from Toornament      ;")
    (println ";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;\n")

    (def matches (toornament/importable-matches tournament-id))
    (pp/pprint matches)))

(defn -main
  [& args]
  (start-tournament "Test"))
