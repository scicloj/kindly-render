(ns scicloj.kindly-render.entry.hiccups
  (:require [scicloj.kindly-render.note.to-hiccup :as to-hiccup]
            [scicloj.kindly-render.note.to-hiccup :as to-hiccup-js]
            [scicloj.kindly-render.entry.comments :as comments]
            [scicloj.kindly-render.shared.from-markdown :as from-markdown]
            [scicloj.kindly-render.shared.walk :as walk]))

(defn code-and-value
  "Transforms a note into hiccup elements"
  [{:as note :keys [code comment?]}]
  (if comment?
    [(-> (comments/inline-markdown code)
         (from-markdown/to-hiccup))]
    (let [note (walk/derefing-advise note)
          {:keys [out err exception kindly/options]} note
          {:keys [hide-code hide-value]} options
          show-code (and code (not hide-code))
          show-value (and (contains? note :value) (not hide-value))]
      (cond-> []
              show-code (conj (to-hiccup/code-block code))
              out (conj (to-hiccup/message out "Stdout"))
              err (conj (to-hiccup/message err "Stderr"))
              show-value (conj (to-hiccup-js/render note))
              exception (conj (to-hiccup/message (ex-message exception) "Exception"))))))
