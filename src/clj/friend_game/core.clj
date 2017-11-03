(ns friend-game.core
  (:require [compojure.core :as compojure :refer [GET]]
            [org.httpkit.server :as httpkit]))

(defonce server (atom nil))

(compojure/defroutes app
  (GET "/" [] "<h1> Welcome to FriendGame</h1>"))

(defn stop-server! []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn -main [& args]
  (let [port (Integer/parseInt (or (first args) "9001"))]
    (reset! server (httpkit/run-server #'app {:port port}))))
