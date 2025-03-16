(ns scicloj.kindly-render.shared.util
  (:require [clojure.string :as str]
            #?(:clj [scicloj.kindly-render.shared.jso :as json])))

(defn json-str [value]
  #?(:clj  (json/write-json-str value)
     :cljs (js/JSON.stringify (clj->js value))))

(defn kind-str [value]
  (str/join \newline (if (vector? value) value [value])))

(defn multi-nth
  [v indexes]
  (reduce (fn [coll idx]
            (nth coll idx))
          v
          indexes))
