(ns scicloj.kindly-render.notes.to-html-page
  (:require [hiccup.page :as page]
            [scicloj.kindly-render.entry.hiccups :as hiccups]
            [scicloj.kindly-render.notes.resources :as resources]
            [scicloj.kindly-render.notes.to-hiccup-page :as to-hiccup-page]))

;; TODO: this ns (and hiccup.page) do very little, maybe move them into page or something

(defn page [notebook]
  (-> (to-hiccup-page/page notebook)
      (page/html5)))

(defn render-notebook
  "Given a prepared notebook, returns an HTML string page"
  [notebook]
  (-> (hiccups/with-hiccups notebook)
      (resources/with-resource-hiccups)
      (page)))
