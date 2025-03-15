(ns scicloj.kindly-render.notes.resource-loader
  "Creates dynamic loaders that will insert <script src=...> if not already loaded"
  (:require [clojure.string :as str]
            [scicloj.kindly-render.shared.util :as util]
            [scicloj.kindly-render.notes.resource-hiccup :as resource-hiccup]))

(defmulti resource-loader :type)

(defmethod resource-loader :js
  [{:keys [src package]}]
  [:script
   (str/replace
     "var loadedScripts = loadedScripts ?? {};  !loadedScripts[SRC] ? (loadedScripts[SRC] = true, document.head.appendChild(Object.assign(document.createElement('script'), { src: SRC, onload: () => console.log(`Script SRC loaded.`) }))) : console.log(`Script SRC is already loaded.`);"
     "SRC" (str \' (if (string? package)
                     (resource-hiccup/relative src package)
                     src)
                \'))])

(defmethod resource-loader :css
  [{:keys [href package]}]
  [:script
   (str/replace
     "var loadedScripts = loadedScripts ?? {}; !loadedScripts[HREF] ? (loadedScripts[HREF] = true, document.head.appendChild(Object.assign(document.createElement('link'), { type: 'text/css', :rel 'stylesheet', href: HREF, onload: () => console.log(`CSS HREF loaded.`) }))) : console.log(`CSS HREF is already loaded.`);"
     "HREF" (str \' (if (string? package)
                      (resource-hiccup/relative href package)
                      href)
                 \'))])

(defmethod resource-loader :scittle
  [{:keys [forms]}]
  (util/scittle forms))
