(ns scicloj.kindly-render.notes.resources
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [scicloj.kindly-render.note.to-hiccup-js :as to-hiccup-js]
            [scicloj.kindly-render.notes.js-deps :as js-deps]))

(defn filename [url]
  (last (str/split url #"/")))

(defn inline [url]
  (slurp (if-let [r (io/resource (filename url))] r url)))

;; relative to page or site? or both? TODO: copy! (or leave to clay)
(defn relative [url]
  (filename url))

(defn js-script [location url]
  (case location
    :relative [:script {:type "text/javascript" :src (relative url)}]
    :inline [:script {:type "text/javascript"} (inline url)]
    #_:cdn [:script {:type "text/javascript" :src url}]))

(defn js-scripts [location urls]
  (mapv #(js-script location %) urls))

(defn css-link [location url]
  (case location
    :relative [:link {:type "text/css" :href (relative url) :rel "stylesheet"}]
    :inline [:link {:type "text/css" :rel "stylesheet"} (inline url)]
    #_:cdn [:link {:type "text/css" :href url :rel "stylesheet"}]))

(defn css-links [location urls]
  (mapv #(css-link location %) urls))

(defn resource-hiccups [{:keys [location js css scittle]}]
  ;; TODO: style.css should only be added by users if they want it? or should be in deps???
  ;; TODO: maybe organize as :head :body :before :after???
  (sequence cat
            [[(css-link location "kindly.css")
              (css-link location "style.css")]
             (css-links location css)
             (js-scripts location js)
             (when (seq scittle)
               [(to-hiccup-js/scittle scittle)])]))

(defn with-resource-hiccups
  "Should be called after deps have been discovered by rendering the notes in the notebook"
  [notebook]
  (let [{:as notebook :keys [resources]} (js-deps/with-resources notebook)]
    (assoc notebook :resource-hiccups (resource-hiccups resources))))
