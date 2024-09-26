(ns scicloj.kindly-render.shared.util
  (:require [clojure.string :as str]
            #?(:clj [clojure.data.json :as json])))

(defn json-str [value]
  #?(:clj  (json/write-str value)
     :cljs (js/JSON.stringify (clj->js value))))

;; TODO: shouldn't need this (something upstream should have unwrapped it already)
(defn kind-str [value]
  (str/join \newline (if (vector? value) value [value])))
