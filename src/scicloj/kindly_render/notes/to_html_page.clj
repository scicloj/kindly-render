(ns scicloj.kindly-render.notes.to-html-page
  (:require [hiccup.page :as page]
            [scicloj.kindly-render.entry.hiccups :as hiccups]
            [scicloj.kindly-render.notes.to-hiccup-page :as to-hiccup-page]
            [scicloj.kindly.v4.api :as kindly]))

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

(defn render-note
  "Given a note, renders a notebook of just that note"
  ([note] (render-note note nil))
  ([note options]
   (let [notebook (hiccups/with-hiccups {:notes          [note]
                                         :kindly/options (kindly/deep-merge {:deps #{:kindly :clay}
                                                                             ;;:package :embed
                                                                             }
                                                                            options)})
         kind (get-in notebook [:notes 0 :kind])
         hiccups (to-hiccup-page/hiccups notebook)]
     (when (and kind (seq hiccups))
       {:html (page notebook)
        :kind (pr-str kind)}))))
