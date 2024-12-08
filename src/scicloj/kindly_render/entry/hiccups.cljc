(ns scicloj.kindly-render.entry.hiccups
  (:require [scicloj.kindly-render.note.to-hiccup :as to-hiccup]
            [scicloj.kindly-render.note.to-hiccup-js :as to-hiccup-js]
            [scicloj.kindly-render.entry.comments :as comments]
            [scicloj.kindly-render.shared.from-markdown :as from-markdown]
            [scicloj.kindly-render.shared.walk :as walk]))

(defn code-and-value
  "Transforms a note into hiccup elements"
  [{:as note :keys [code comment?]}]
  (if comment?
    [(-> (comments/inline-markdown code)
         (from-markdown/to-hiccup))]
    (let [note (walk/advise-deps note)
          {:keys [out err exception kindly/options]} note
          {:keys [hide-code hide-value]} options
          show-code (and code (not hide-code))
          show-value (and (contains? note :value) (not hide-value))]
      (cond-> []
              show-code (conj (to-hiccup/code-block code))
              out (conj (to-hiccup/message out "Stdout"))
              err (conj (to-hiccup/message err "Stderr"))
              show-value (conj (if walk/*js*
                                 (to-hiccup-js/render note)
                                 (to-hiccup/render note)))
              exception (conj (to-hiccup/message (ex-message exception) "Exception"))))))

(defn with-hiccups
  "Adds `:hiccups` and `:deps` to a notebook.
  `:hiccups` represent the note code and visualization.
  `:deps` indicate that resources are required to produce the visualizations."
  [{:as notebook :keys [js notes] :or {js true}}]
  (binding [walk/*js* js
            walk/*deps* (atom #{})]
    (assoc notebook :hiccups (doall (mapcat code-and-value notes))
                    :deps (into @walk/*deps* (walk/optional-deps notebook)))))
