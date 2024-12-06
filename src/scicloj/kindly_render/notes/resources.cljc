(ns scicloj.kindly-render.notes.resources
  "A resource is some javascript, css, or scittle code.
  Resources are brought in to satisfy dependencies.
  Resources may be appended to head or appended to body.
  Javascript and css may be embedded, at a relative location, or at a cdn location."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [scicloj.kindly-render.note.to-hiccup-js :as to-hiccup-js]
            [scicloj.kindly-render.notes.js-deps :as js-deps]))

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
        :else-embed [:script {:type "text/javascript"} (slurp-resource src)]))

(defmethod resource-hiccup :css
  [{:keys [href package async] :or {async true}}]
  (cond (not package) [:link {:type "text/css" :rel "stylesheet" :async async :href href}]
        (string? package) [:link {:type "text/css" :rel "stylesheet" :async async :href (relative href package)}]
        :else-embed [:link {:type "text/css" :rel "stylesheet"} (slurp-resource href)]))

(defmethod resource-hiccup :scittle
  [{:keys [forms]}]
  (to-hiccup-js/scittle forms))

(defn dep-resource
  "Deps may just be labels like a kind, or a map with additional properties like placement.
  We represent each resource as a map."
  [dep type props]
  (merge {:type type}
         props
         (if (map? dep)
           dep
           (case type
             :js {:src dep}
             :css {:href dep}
             ;; TODO: could collect scittle into one tag
             :scittle {:forms [dep]}))))

(defn deps-resources
  "Once the deps are established, we treat them as abstract resources and dispatch rendering on type.
  Returns a flat sequence of resources."
  [depm global-props]
  (let [props (merge global-props
                     (select-keys [:placement :package :async] depm))]
    (for [type [:css :js :scittle]
          ;; placement here may be a map of type to placement key, or just a placement key
          :let [props (if (map? (:placement props))
                        (update props :placement get type)
                        props)]
          dep (get depm type)]
      (dep-resource dep type props))))

(defn resources-by-placement
  "Returns a map of resources organized placement (head or body)"
  [resources]
  (group-by (fn get-placement [resource]
              (or (:placement resource)
                  :head))
            resources))

(defn resource-hiccups
  [resources]
  (-> (resources-by-placement resources)
      (update-vals #(doall (map resource-hiccup %)))))

(defn with-resource-hiccups
  "Adds hiccups organized by placement (:head or :body) for resource dependencies.
  Will respect global options for placement, package, async.
  Options may be overwritten per dep map, or per dep."
  [{:as notebook :keys [kindly/options]}]
  (let [props (select-keys options [:placement :package :async])
        depms (js-deps/notebook-depms notebook)
        resources (-> (mapcat #(deps-resources % props) depms)
                      (distinct))]
    (assoc notebook :resource-hiccups (resource-hiccups resources))))
