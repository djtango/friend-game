(ns friend-game.core
  (:require [reagent.core :as reagent]))

(enable-console-print!)

(defn index []
  [:div
   [:h1 "Teh Frend Game"]])

(reagent/render [index] (.getElementById js/document "app"))
