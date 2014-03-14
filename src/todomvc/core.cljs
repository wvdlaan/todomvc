(ns todomvc.core
  (:require [goog.events :as e]
            [cljs.core.async :refer [<! put! chan]]
            [todomvc.render :as render]
            [todomvc.data :as data])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn state-to-string
  "Turn state into a pretty string"
  [{:keys [all-done? current-filter next-id items]}]
  (str "{:all-done? " all-done? "\n"
       " :current-filter " current-filter "\n"
       " :next-id " next-id "\n"
       " :items ["
       (apply str (interpose "\n         " items))
       "]}"))

(defn pp-state
  "Print state in browser console"
  [state]
  (.log js/console (state-to-string state)))

(defn pp-transaction
  "Print transaction in browser console"
  [transaction]
  (.log js/console (str "\n(transact state " transaction ") =>")))

(defn load-app
  "Return a map containing the initial application"
  []
  {:state (atom (data/initial-state))
   :channel (chan)
   :render-pending? (atom false)
   :transact data/transact})

(defn init-updates
  "For every value coming from channel;
   - call transact to update the application state
   - trigger a render"
  [{:keys [state channel transact] :as app}]
  (go (while true
        (let [transaction (<! channel)]
          (swap! state transact transaction)
          (pp-transaction transaction)
          (pp-state @state)
          ;; render after each state change
          (render/request-render app)))))

(defn ^:export main
  "Application entry point"
  []
  (let [app (load-app)]
    (init-updates app)
    (pp-state @(:state app))
    ;; initial render
    (render/request-render app)
    ;; hook for development/debugging
    (def app-hook app)))
