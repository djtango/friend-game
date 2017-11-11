(ns friend-game.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [reagent.core :as reagent]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! put! chan]]
            [taoensso.sente :as sente :refer (cb-success?)]
            ))

(enable-console-print!)

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/chsk"
                                  {:type :auto})]
  (def chsk chsk)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def chsk-state state))

(defn put-inc []
  (http/put "http://localhost:9001/counter/inc"))

(defn go-wrapper [http-fn on-success]
  (go (let [response (<! (http-fn))]
       (-> response
           :body
           on-success))))


;;;; Sente event handlers

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id)

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (println "ev-msg" (keys ev-msg) event)
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event]}]
  (println "Unhandled event:" event))

(defmethod -event-msg-handler :chsk/state
  [{:as ev-msg :keys [?data]}]
  (if-let [[old-state-map new-state-map] ?data]
    (if (:first-open? new-state-map)
      (println "Channel socket successfully established!:" new-state-map)
      (println "Channel socket state change:"              new-state-map))
    (println "No data supplied in response.")))

(defmethod -event-msg-handler :chsk/recv
  [{:as ev-msg :keys [?data]}]
  (let [{[_ msg] :event} ev-msg]
   (println "Push event from server: %s" ?data)
   (event-msg-handler {:id (first msg)
                       :event msg})))

(defmethod -event-msg-handler :chsk/handshake
  [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (println "Handshake: %s" ?data)))

(def broadcast-counter (reagent/atom 0))
(defmethod -event-msg-handler :friend-game/broadcast-counter
  [{:as ev-msg :keys [?data event]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data
        [_ {:keys [msg counter]}] event]
    (println msg)
    (println counter)
    (reset! broadcast-counter counter)
    (println "broadcast-counter: " @broadcast-counter)))
;;
;; TODO Add your (defmethod -event-msg-handler <event-id> [ev-msg] <body>)s here...

;;;; Sente event router (our `event-msg-handler` loop)

(defonce router_ (atom nil))
(defn  stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router_
          (sente/start-client-chsk-router! ch-chsk
                                           event-msg-handler)))

;;;; UI events

(defn start-broadcast-on-click [ev]
  (println "Start broadcast")
  (chsk-send! [:friend-game/start-broadcast]
              2000
              (fn [server-resp]
                (println "Messaged received from server:" server-resp))))

(defn- on-login [resp]
  (println "Login Response:" resp)
  (let [login-successful? true]
    (if-not login-successful?
      (println "Login failed.")
      (do (println "Login successful.")
        (sente/chsk-reconnect! chsk)))))

(defn login [uid]
  (sente/ajax-lite "/login"
                   {:method :post,
                    :headers {:X-CSRF-Token (:csrf-token @chsk-state)},
                    :params {:user-id (str uid)}}
                   on-login))


(defn index []
  (let [counter (reagent/atom 0)]
    (fn []
      [:div
       [:h1 "Teh Frend Game 2"]
       [:button {:on-click (fn [_] (go-wrapper put-inc #(reset! counter %)))} "Click Me"]
       [:button {:on-click start-broadcast-on-click}
        "Start broadcast"]
       [:button {:on-click (fn [_] (login 1))}
        "Login"]
       [:p "Count:" @counter]
       [:p "Broadcast counter: " @broadcast-counter]])))

(defn start! [] (start-router!))

(defonce _start-once (start!))

(reagent/render [index] (.getElementById js/document "app"))
