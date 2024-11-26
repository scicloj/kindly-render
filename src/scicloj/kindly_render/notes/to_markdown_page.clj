(ns scicloj.kindly-render.notes.to-markdown-page
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [hiccup.core :as hiccup]
            [hiccup.page :as page]
            [scicloj.kindly-render.note.to-hiccup-js :as to-hiccup-js]
            [scicloj.kindly-render.note.to-markdown :as to-markdown]
            [scicloj.kindly-render.entry.markdowns :as markdowns]))

;; TODO: DRY and move to a css file
(def styles
  "<style>
.sourceCode:has(.printedClojure) {
  background-color: transparent;
  border-style: none;
}

.kind_map {
  background:            lightgreen;
  display:               grid;
  grid-template-columns: repeat(2, auto);
  justify-content:       center;
  text-align:            right;
  border: solid 1px black;
  border-radius: 10px;
}

.kind_vector {
  background:            lightblue;
  display:               grid;
  grid-template-columns: repeat(1, auto);
  align-items:           center;
  justify-content:       center;
  text-align:            center;
  border:                solid 2px black;
  padding:               10px;
}

.kind_set {
  background:            lightyellow;
  display:               grid;
  grid-template-columns: repeat(auto-fit, minmax(auto, max-content));
  align-items:           center;
  justify-content:       center;
  text-align:            center;
  border:                solid 1px black;
}

.kind_seq {
  background:            bisque;
  display:               grid;
  grid-template-columns: repeat(auto-fit, minmax(auto, max-content));
  align-items:           center;
  justify-content:       center;
  text-align:            center;
  border:                solid 1px black;
}
</style>")

;; TODO: should kindly specify a way to provide front-matter?
(defn page-setup [notes]
  (let [[{:keys [form]}] notes
        {:keys [front-matter]} (meta form)]
    (str
      (when front-matter
        (str "---" \newline
             (str/trim-newline (json/write-str front-matter)) \newline
             "---" \newline \newline))
      styles \newline \newline
      ;; TODO: for a book these should just go in _quarto.yml as include-in-header
      ;; But for a standalone markdown file we need them
      ;; How do we tell the difference?
      (hiccup/html (page/include-css "style.css")) \newline
      (hiccup/html (apply page/include-js (to-hiccup-js/include-js))) \newline
      ;; TODO: this could should only exist when user needs it, and it was a dependency
      ;; either detected, or requested, or they could just add it as hiccup??
      (hiccup/html
        (to-hiccup-js/scittle '[(require '[reagent.core :as r]
                                         '[reagent.dom :as dom]
                                         '[clojure.str :as str])])))))

(defn render-notebook
  "Creates a markdown file from a notebook"
  [{:keys [notes js] :or {js true}}]
  (binding [to-markdown/*js* js
            to-hiccup-js/*deps* (atom #{})]
    ;; rendering must happen before page-setup to gather dependencies (maybe not for a book though?)
    (let [note-strs (mapcat markdowns/code-and-value notes)]
      (str (page-setup notes) \newline \newline
           (str/join (str \newline \newline) note-strs) \newline))))
