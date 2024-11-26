(ns scicloj.kindly-render.entry.markdowns
  (:require [scicloj.kindly-render.note.to-markdown :as to-markdown]
            [scicloj.kindly-render.entry.comments :as comments]))

;; Markdown is sensitive to whitespace (especially newlines).
;; fragments like blocks must be separated by a blank line.
;; These markdown producing functions return strings with no trailing newline,
;; which are combined with double newline.

(defn code-and-value
  "Transforms a note into markdown strings"
  [{:as note :keys [code out err exception comment?]}]
  (if comment?
    [(comments/inline-markdown code)]
    (cond-> []
            code (conj (to-markdown/code-block code))
            out (conj (to-markdown/message out "stdout"))
            err (conj (to-markdown/message err "stderr"))
            (contains? note :value) (conj (to-markdown/render note))
            exception (conj (to-markdown/message (ex-message exception) "exception")))))
