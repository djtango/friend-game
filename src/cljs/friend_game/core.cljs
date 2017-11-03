(ns friend-game.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(enable-console-print!)

(defn put-inc []
  (http/put "http://localhost:9001/counter/inc"))

(defn go-wrapper [http-fn on-success]
  (go (let [response (<! (http-fn))]
       (-> response
           :body
           on-success))))

(defn index []
  (let [counter (reagent/atom 0)]
    (fn []
      [:div
       [:h1 "Teh Frend Game"]
       [:button {:on-click (fn [_]
                             (go-wrapper put-inc #(reset! counter %)))} "Click Me"]
       [:p "Count:" @counter]])))

(reagent/render [index] (.getElementById js/document "app"))
