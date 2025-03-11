(ns scicloj.kindly-render.notes.resources
  "A resource is some javascript, css, or scittle code.
  Resources are brought in to satisfy dependencies.
  Resources may be appended to head or appended to body.
  Javascript and css may be embedded, at a relative location, or at a cdn location."
  (:require [clojure.string :as str]
            [scicloj.kindly-render.notes.js-deps :as js-deps]
            [scicloj.kindly-render.notes.resource-hiccup :as resource-hiccup]
            [scicloj.kindly-render.notes.resource-loader :as resource-loader]))

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
              (or (:placement resource) :head))
            resources))

(defn resource-hiccups
  [resources]
  (-> (resources-by-placement resources)
      (update-vals #(map resource-hiccup/resource-hiccup %))))

(defn depmaps-resources [depmaps options]
  (let [props (select-keys options [:placement :package :async])]
    (-> (mapcat #(deps-resources % props) depmaps)
        (distinct))))

(defn with-resource-hiccups
  "Adds hiccups organized by placement (:head or :body) for resource dependencies.
  Will respect global options for placement, package, async.
  Options may be overwritten per dep map, or per dep."
  [{:as notebook :keys [kindly/options]}]
  (let [depmaps (js-deps/notebook-depmaps notebook)]
    (assoc notebook
      :resource-hiccups (-> (depmaps-resources depmaps options)
                            (resource-hiccups)))))

(defn in-element-deps [{:as note :keys [kind kindly/options hiccup deps]}]
  (println "HI" deps)
  (if (and (:in-element-deps options true) kind (vector? hiccup))
    (let [depmaps (js-deps/resolve-deps-tree deps options)
          resources (depmaps-resources depmaps options)
          ;; TODO: what about css?
          loaders (->> (filter (comp #{:js} :type) resources)
                       (map resource-loader/resource-loader))]
      (prn "Loaders" loaders)
      ;; TODO: could put the loaders inside the same script tag
      ;; TODO: a global function would be nice if we can have it
      (assoc note :hiccup (cons loaders (list hiccup))))
    note))
