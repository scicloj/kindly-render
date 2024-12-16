(ns scicloj.kindly-render.entry.hiccups
  (:require [scicloj.kindly-render.entry.comments :as comments]
            [scicloj.kindly-render.note.to-hiccup :as to-hiccup]
            [scicloj.kindly-render.note.to-hiccup-js :as to-hiccup-js]
            [scicloj.kindly-render.notes.resources :as resources]
            [scicloj.kindly-render.shared.from-markdown :as from-markdown]
            [scicloj.kindly-render.shared.walk :as walk]))

(defn value-hiccup
  "Adds `:hiccup` to note if a value was rendered"
  [note]
  (if (walk/show? note)
    (if (walk/js? note)
      (to-hiccup-js/render note)
      (to-hiccup/render note))
    note))

(defn extra-hiccups
  "Adds hiccup for code, output, and exceptions"
  [{:as note :keys [code out err exception kindly/options]}]
  (let [{:keys [hide-code]} options
        show-code (and code (not hide-code))]
    (cond-> note
            show-code (assoc :code-hiccup (to-hiccup/code-block code))
            out (assoc :out-hiccup (to-hiccup/message out "Stdout"))
            err (assoc :err-hiccup (to-hiccup/message err "Stderr"))
            exception (assoc :ex-hiccup (to-hiccup/message (ex-message exception) "Exception")))))

(defn comment-hiccup
  "Converts inline comments to (markdown) hiccup"
  [{:as note :keys [code]}]
  (->> (comments/inline-markdown code)
       (from-markdown/to-hiccup)
       (assoc note :comment-hiccup)))

(defn note-hiccups
  "Attaches code, comment, output, exception, and value hiccup to note when applicable"
  [{:as note :keys [comment?]}]
  (if comment?
    (comment-hiccup note)
    (-> (walk/advise-with-deps note)
        (value-hiccup)
        (extra-hiccups))))

(defn with-hiccups
  "Updates the notes of a notebook with hiccup for display, and adds resource hiccups"
  [notebook]
  (-> (update notebook :notes walk/index-notes)
      (update :notes #(map note-hiccups %))
      (resources/with-resource-hiccups)))
