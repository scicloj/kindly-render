(ns scicloj.kindly-render.notes.resource-hiccup
  "Creates <script src=...> tags suitable to put in the document head"
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [scicloj.kindly-render.shared.util :as util]))

(defn filename [url]
  (last (str/split url #"/")))

(defn resource-cache
  "Given a URL, looks for a resource with the same filename"
  [url]
  (io/resource (filename url)))

(defn slurp-resource
  "Like slurp, but can find copies of remote files in a resources cache.
  Prefers a file if it exists, otherwise looks in the cache, and finally just tries to slurp the url."
  [url]
  (let [f (io/file url)]
    (if (.exists f)
      (slurp f)
      (slurp (or (resource-cache url) url)))))

(defn relative [url root]
  (str root
       (when-not (str/ends-with? root "/") "/")
       (filename url)))

(defmulti resource-hiccup :type)

(defmethod resource-hiccup :js
  [{:keys [src package async]}]
  (cond (not package) [:script {:type "text/javascript" :async async :src src}]
        (string? package) [:script {:type "text/javascript" :async async :src (relative src package)}]
        :else-embed [:script (slurp-resource src)]))

(defmethod resource-hiccup :css
  [{:keys [href package]}]
  (cond (not package) [:link {:type "text/css" :rel "stylesheet" :href href}]
        (string? package) [:link {:type "text/css" :rel "stylesheet" :href (relative href package)}]
        :else-embed [:style (slurp-resource href)]))

(defmethod resource-hiccup :scittle
  [{:keys [forms]}]
  (util/scittle forms))
