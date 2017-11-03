(defproject raid-helper "0.1.0-SNAPSHOT"
  :description "An app for all your raiding needs"
  :url "https://www.github.com/djtango/raid-helper"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-beta4"]
                 [midje "1.9.0-alpha6"]
                 [org.clojure/clojurescript "1.9.946"]]

  :source-paths ["src/clj"]
  :plugins [[lein-midje "3.1.1"]]
  :repl-options {:init-ns raid-helper.core
                 :init (do
                         (require '[midje.repl :as repl])
                         (repl/autotest))})
