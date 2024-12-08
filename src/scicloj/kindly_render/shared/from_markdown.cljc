(ns scicloj.kindly-render.shared.from-markdown
  (:require [nextjournal.markdown :as md]
            [nextjournal.markdown.transform :as mdt]
            [scicloj.kindly-render.shared.util :as util]))

(def mdctx
  "NextJournal Markdown produces fragments (called :plain) which are rendered as :<>
  These are better represented as a sequence, which hiccup will treat as a fragment."
  (assoc mdt/default-hiccup-renderers
    :plain (fn [ctx {:keys [text content]}]
             (or text (map #(mdt/->hiccup ctx %) content)))))

(defn to-hiccup [value]
  (mdt/->hiccup mdctx (md/parse (util/kind-str value))))
