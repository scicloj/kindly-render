(ns scicloj.kindly-render.notes.to-hiccup-page
  (:require [scicloj.kindly-render.entry.hiccups :as hiccups]
            [scicloj.kindly-render.notes.resources :as resources]))

(defn page [{:as notebook :keys [resource-hiccups hiccups]}]
  (let [{:keys [head body]} resource-hiccups]
    (list [:head head]
          [:body hiccups body])))

(defn render-notebook
  "Returns an edn string representation of a notebook as a hiccup page"
  [notebook]
  (-> (hiccups/with-hiccups notebook)
      (resources/with-resource-hiccups)
      (page)
      (pr-str)))
