(ns dev
  (:require [clojure.java.io :as io]
            [scicloj.kindly-render.notes.js-deps :as js-deps]
            [scicloj.kindly-render.notes.resources :as resources]))

(defn fill-cache []
  (doseq [[k {:keys [js css]}] (merge js-deps/js-deps js-deps/kind-deps)
          url (concat js css)]
    (spit (io/file "cache" (resources/filename url)) (slurp url))))

(comment
  (fill-cache))
