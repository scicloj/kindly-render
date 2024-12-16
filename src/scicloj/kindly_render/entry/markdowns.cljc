(ns scicloj.kindly-render.entry.markdowns
  (:require [scicloj.kindly-render.note.to-markdown :as to-markdown]
            [scicloj.kindly-render.entry.comments :as comments]
            [scicloj.kindly-render.shared.walk :as walk]
            [scicloj.kindly-render.notes.resources :as resources]))

;; Markdown is sensitive to whitespace (especially newlines).
;; fragments like blocks must be separated by a blank line.
;; These markdown producing functions return strings with no trailing newline,
;; which are combined with double newline.

(defn value-markdown [note]
  (if (walk/show? note)
    (to-markdown/render note)
    note))

(defn extra-markdown [{:as note :keys [code out err exception kindly/options]}]
  (let [{:keys [hide-code]} options
        show-code (and code (not hide-code))]
    (cond-> note
            show-code (assoc :code-md (to-markdown/code-block code (walk/js? note)))
            out (assoc :out-md (to-markdown/message out "stdout"))
            err (assoc :err-md (to-markdown/message err "stderr"))
            exception (assoc :ex-md (to-markdown/message (ex-message exception) "exception")))))

(defn comment-hiccup
  "Converts inline comments to (markdown) hiccup"
  [{:as note :keys [code]}]
  (->> (comments/inline-markdown code)
       (assoc note :comment-md)))

(defn note-markdowns
  "Attaches code, comment, output, exception, and value hiccup to note when applicable"
  [{:as note :keys [comment?]}]
  (if comment?
    (comment-hiccup note)
    (-> (walk/advise-with-deps note)
        (value-markdown)
        (extra-markdown))))

(defn with-markdowns [notebook]
  "Updates the notes of a notebook with markdown for display, and adds resource hiccups"
  (-> (update notebook :notes walk/index-notes)
      (update :notes #(map note-markdowns %))
      (resources/with-resource-hiccups)))
