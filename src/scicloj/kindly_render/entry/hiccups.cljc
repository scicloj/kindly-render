(ns scicloj.kindly-render.entry.hiccups
  (:require [scicloj.kindly-render.note.to-hiccup :as to-hiccup]
            [scicloj.kindly-render.note.to-hiccup :as to-hiccup-js]
            [scicloj.kindly-render.entry.comments :as comments]
            [scicloj.kindly-render.shared.from-markdown :as from-markdown]
            [scicloj.kindly-render.shared.walk :as walk]))

(defn code-and-value
  "Transforms a note into hiccup elements"
  [{:as note :keys [code out err exception comment?]}]
  (if comment?
    [(-> (comments/inline-markdown code)
         (from-markdown/to-hiccup))]
    (let [{{:keys [hide-code hide-value]} :kindly/options} (walk/derefing-advise note)]
      (cond-> []
              (and code (not hide-code)) (conj (to-hiccup/code-block code))
              out (conj (to-hiccup/message out "Stdout"))
              err (conj (to-hiccup/message err "Stderr"))
              (and (not hide-value)) (conj (to-hiccup-js/render note))
              exception (conj (to-hiccup/message (ex-message exception) "Exception"))))))
