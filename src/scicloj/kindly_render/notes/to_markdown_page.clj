(ns scicloj.kindly-render.notes.to-markdown-page
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [hiccup.core :as hiccup]
            [scicloj.kindly-render.entry.markdowns :as markdowns]
            [scicloj.kindly-render.notes.resources :as resources]))

(defn page [{:as notebook :keys [resource-hiccups markdowns kindly/options]}]
  (let [{:keys [front-matter]} options]
    (str
      (when front-matter
        (str "---" \newline
             (str/trim-newline (json/write-str front-matter)) \newline
             "---" \newline \newline))
      ;; TODO: for a book these should just go in _quarto.yml as include-in-header
      (hiccup/html resource-hiccups) \newline
      (str/join (str \newline \newline) markdowns) \newline)))

(defn render-notebook
  "Given a notebook, returns a markdown page as a string"
  [notebook]
  (-> (markdowns/with-markdowns notebook)
      (resources/with-resource-hiccups)
      (page)))
