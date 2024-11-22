(ns scicloj.kindly-render.notes.to-html-page
  (:require [clojure.string :as str]
            [hiccup.page :as page]
            [scicloj.kindly-render.note.to-hiccup-js :as to-hiccup-js]
            [scicloj.kindly-render.shared.from-markdown :as from-markdown]))

(defn message [s channel]
  [:blockquote
   [:strong channel]
   [:pre [:code s]]])

(defn comment-hiccup [code]
  (->> (str/split-lines code)
       #! clojure comments can be shebang style, useful for babashka
       (map #(str/replace-first % "#!" "    #!"))
       ;; inline comments remove the semicolons
       (map #(str/replace-first % #"^(;+)\s?" ""))
       ;; keep titles separated in markdown
       (map #(str/replace-first % #"^#" "\n#"))
       (str/join \newline)
       (from-markdown/to-hiccup)))

(defn render-note-items [{:as note :keys [code out err exception comment?]}]
  (cond-> []
          (and code (not comment?)) (conj [:pre {:class "clojure"} [:code code]])
          out (conj (message out "Stdout"))
          err (conj (message err "Stderr"))
          ;; TODO: should comment-hiccup be moved into render?
          comment? (conj (comment-hiccup code))
          (not comment?) (conj (to-hiccup-js/render note))
          exception (conj (message (ex-message exception) "Exception"))))

(defn page [elements]
  (page/html5
    (cond->
      [:head
       (page/include-css "style.css")
       (apply page/include-js (to-hiccup-js/include-js))]
      (:scittle-reagent @to-hiccup-js/*deps*)
      (conj (to-hiccup-js/scittle '[(require '[reagent.core :as r :refer [atom]]
                                             '[reagent.dom :as dom])])))
    (into [:body] elements)))

(defn render-notebook
  "Creates a markdown file from a notebook"
  [{:keys [notes]}]
  (-> (mapcat render-note-items notes)
      (page)))
