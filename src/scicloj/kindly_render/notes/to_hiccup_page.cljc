(ns scicloj.kindly-render.notes.to-hiccup-page
  (:require [scicloj.kindly-render.entry.hiccups :as hiccups]
            [scicloj.kindly-render.notes.resources :as resources]))

;; TODO: sometimes user may want them placed in other positions, quarto handles this with include-before...etc
(defn page [{:as notebook :keys [resource-hiccups hiccups]}]
  (list [:head resource-hiccups]
        [:body hiccups]))

(defn render-notebook
  "Given a prepared notebook, returns an edn string representation of a notebook as a hiccup page"
  [notebook]
  (-> (hiccups/with-hiccups notebook)
      (resources/with-resource-hiccups)
      (page)
      (pr-str)))
