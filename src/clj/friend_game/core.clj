(ns friend-game.core
  (:require [compojure.core :as compojure :refer [GET PUT]]
            [org.httpkit.server :as httpkit]
            [hiccup.page :as hiccup]
            [ring.middleware.resource]))

(defonce server (atom nil))

(def index
  (hiccup/html5
       [:head
        [:meta {:charset "utf-8"}]
        [:meta {:name "apple-mobile-web-app-capable" :content "yes"}]
        [:meta {:name "apple-mobile-web-app-title" :content "FORECAST"}]
        [:link {:rel "apple-touch-icon" :href "/img/icons/touch-icon-iphone.png"}]
        [:link {:rel "apple-touch-icon" :sizes "152x152" :href "/img/icons/touch-icon-ipad.png"}]
        [:link {:rel "apple-touch-icon" :sizes "180x180" :href "/img/icons/touch-icon-iphone-retina.png"}]
        [:link {:rel "apple-touch-icon" :sizes "167x167" :href "/img/icons/touch-icon-ipad-retina.png"}]
        [:title "Friend-Game | Destiny 2 Raid Companion"]]
       [:body
        [:div#app]
        (hiccup/include-js "/js/app.js")]))

(def counter (atom 0))

(compojure/defroutes routes
  (GET "/" [] index)
  (PUT "/counter/inc" [] (fn [res]
                           (swap! counter inc)
                           {:status 200
                            :headers {"Content-Type" "application/json"}
                            :body (str @counter)})))

(def app
  (-> routes
      (ring.middleware.resource/wrap-resource "public")))

(defn stop-server! []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn -main [& args]
  (let [port (Integer/parseInt (or (first args) "9001"))]
    (reset! server (httpkit/run-server #'app {:port port}))))
