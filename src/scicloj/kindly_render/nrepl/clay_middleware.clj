;; TODO: move to the clay project
(ns scicloj.kindly-render.nrepl.clay-middleware
  (:require [scicloj.clay.v2.api :as clay]
            [hiccup.core :as hiccup]))

(defn clay-render [note]
  (when-let [hiccups (seq (clay/make-hiccup {:single-form       (:form note)
                                             :source-path       (:file (meta (:value note)))
                                             :inline-js-and-css true}))]
    {:html (hiccup/html hiccups)}))
