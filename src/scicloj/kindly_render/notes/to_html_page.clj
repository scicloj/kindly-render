(ns scicloj.kindly-render.notes.to-html-page
  (:require [hiccup.page :as page]
            [scicloj.kindly-render.entry.hiccups :as hiccups]
            [scicloj.kindly-render.notes.to-hiccup-page :as to-hiccup-page]))

(defn page
  "Given a rendered notebook, returns a HTML string page"
  [notebook]
  (-> (to-hiccup-page/page notebook)
      (page/html5)))

(defn render-notebook
  "Given a notebook, renders and returns an HTML string page"
  [notebook]
  (-> (hiccups/with-hiccups notebook)
      (page)))
