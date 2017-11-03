(defproject friend-game "0.1.0-SNAPSHOT"
  :description "An app for all your raiding needs"
  :url "https://www.github.com/djtango/friend-game"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [;; CLJ
                 [org.clojure/clojure "1.9.0-beta4"]
                 [midje "1.9.0-alpha6"]
                 [ring "1.6.3"]
                 [compojure "1.6.0"]
                 [http-kit "2.2.0"]
                 [hiccup "1.0.5"]
                 [figwheel-sidecar "0.5.9"]

                 ;; CLJS
                 [org.clojure/clojurescript "1.9.946"]
                 [cljs-http "0.1.44"]
                 [reagent "0.8.0-alpha2"]]

  :figwheel {:ring-handler friend-game.core/app}
  :clean-targets ^{:protect false} ["resources/public/cljs" "target"]

  :source-paths ["src/clj"]
  :plugins [[lein-midje "3.1.1"]
            [lein-figwheel "0.5.14"]
            [lein-cljsbuild "1.1.7"]]

  :repl-options {:init-ns friend-game.core
                 :init (do
                         (require '[midje.repl :as repl])
                         (repl/autotest)
                         (-main))}

  :cljsbuild {:builds [{:id           "dev"
                        :source-paths ["src/cljs"]
                        :figwheel     true
                        :compiler     {:main                 friend-game.core
                                       :output-to            "resources/public/js/app.js"
                                       :output-dir           "resources/public/cljs"
                                       :asset-path           "cljs"
                                       :source-map-timestamp true}}

                       {:id           "prod"
                        :source-paths ["src/cljs"]
                        :compiler     {:main                 friend-game.core
                                       :output-to            "resources/public/js/app.js"
                                       :optimizations        :advanced
                                       :closure-defines      {goog.DEBUG false}
                                       :pretty-print         false}}]})
