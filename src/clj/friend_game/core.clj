(ns friend-game.core
  (:require [compojure.core :as compojure :refer [GET PUT POST]]
            [org.httpkit.server :as httpkit]
            [hiccup.page :as hiccup]
            [ring.middleware.resource]
            [ring.middleware.params]
            [ring.middleware.keyword-params]
            [ring.middleware.defaults]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :as sente.server-adapters]
            [clojure.core.async :as async :refer (<! go-loop)]
            ))

(defonce server (atom nil))

(let [{:keys [ch-recv send-fn connected-uids ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (sente.server-adapters/get-sch-adapter) {})]
  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def connected-uids connected-uids))

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

(add-watch connected-uids :connected-uids
           (fn [_ _ old-uids new-uids]
             (when (not= old-uids new-uids)
               (println "Change in connected-uids:" new-uids))))

(defn login-handler [req]
  (let [{:keys [session params]} req
        {:keys [user-id]} params]
    (println "Login request:" params)
    {:status 200,
     :session (assoc session :uid user-id),
     :body (hiccup/html5 [:p (str "hello " user-id)])}))

(compojure/defroutes routes
  (GET "/" [] index)
  (PUT "/counter/inc" [] (fn [res]
                           (swap! counter inc)
                           {:status 200
                            :headers {"Content-Type" "application/json"}
                            :body (str @counter)}))
  (GET "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post req))
  (POST "/login" req (login-handler req)))



(def app
  (-> routes
      (ring.middleware.resource/wrap-resource "public")
      ring.middleware.keyword-params/wrap-keyword-params
      ring.middleware.params/wrap-params))

(defn stop-server! []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn broadcast-counter! []
  (let [broadcast! (fn [i]
                     (let [uids (:any @connected-uids)]
                       (println (str "Broadcasting to" (count uids) "users"))
                       (doseq [uid uids]
                         (chsk-send! uid
                                     [:friend-game/broadcast-counter
                                      {:msg "hello world"
                                       :counter i}]))))]
    (go-loop [i 0]
             (println "tick")
             (broadcast! i)
      (<! (async/timeout 2000))
      (recur (inc i)))))

(defmulti -event-msg-handler :id)
(defn event-msg-handler [{:as ev-msg :keys [id ?data event]}]
  (println "Message received from client:" ev-msg)
  (-event-msg-handler ev-msg))
(defmethod -event-msg-handler :friend-game/start-broadcast
  [{:as ev-msg :keys [?reply-fn]}]
  (broadcast-counter!)
  (?reply-fn "Broadcast initiated"))

(defonce router_ (atom nil))
(defn stop-router! [] (when-let [stop-fn @router_] (stop-fn)))

(defn start-router! []
  (stop-router!)
  (reset! router_
          (sente/start-server-chsk-router!
            ch-chsk
            event-msg-handler)))

(defn -main [& args]
  (let [port (Integer/parseInt (or (first args) "9001"))]
    (start-router!)
    (reset! server (httpkit/run-server #'app {:port port}))))
