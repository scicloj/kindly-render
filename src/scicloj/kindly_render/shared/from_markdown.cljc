(ns scicloj.kindly-render.shared.from-markdown
  (:require [nextjournal.markdown :as md]
            [nextjournal.markdown.transform :as mdt]
            [scicloj.kindly-render.shared.util :as util]))

;; TODO: this might not be the best way to render markdown, but for now it seems good enough
;; Note that either hiccup or a string is fine
;; TODO: is there a nice way to be able to render markdown by adding an adapter?
;; because we don't want flexmark or nextjournal dependencies in this project
;; yes, conditionally require them.

(defn to-hiccup [value]
  (mdt/->hiccup (md/parse (util/kind-str value))))
