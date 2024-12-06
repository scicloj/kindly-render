(ns scicloj.kindly-render.entry.markdowns
  (:require [scicloj.kindly-render.note.to-markdown :as to-markdown]
            [scicloj.kindly-render.entry.comments :as comments]
            [scicloj.kindly-render.shared.walk :as walk]))

;; Markdown is sensitive to whitespace (especially newlines).
;; fragments like blocks must be separated by a blank line.
;; These markdown producing functions return strings with no trailing newline,
;; which are combined with double newline.

(defn code-and-value
  "Transforms a note into markdown strings"
  [{:as note :keys [code comment?]}]
  (if comment?
    [(comments/inline-markdown code)]
    (let [note (walk/advise-deps note)
          {:keys [out err exception kindly/options]} note
          {:keys [hide-code hide-value]} options
          show-code (and code (not hide-code))
          show-value (and (contains? note :value) (not hide-value))]
      (cond-> []
              show-code (conj (to-markdown/code-block code))
              out (conj (to-markdown/message out "stdout"))
              err (conj (to-markdown/message err "stderr"))
              show-value (conj (to-markdown/render note))
              exception (conj (to-markdown/message (ex-message exception) "exception"))))))

(defn with-markdowns [{:as notebook :keys [js notes] :or {js true}}]
  (binding [walk/*js* js
            walk/*deps* (atom #{})]
    (assoc notebook :markdowns (doall (mapcat code-and-value notes))
                    :deps (into @walk/*deps* (walk/optional-deps notebook)))))
