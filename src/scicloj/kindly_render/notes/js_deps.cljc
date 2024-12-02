(ns scicloj.kindly-render.notes.js-deps
  (:require [scicloj.kindly.v4.api :as kindly]
            [scicloj.kindly-render.shared.walk :as walk]))

;; TODO: hiccup without js may still require CSS (likely different CSS if so)
;; ... in which case we'd need to make a different set of dependencies.

(def js-deps
  "Resources that do not correspond to a kind, but are useful on their own"
  {:d3          {:js ["https://cdn.jsdelivr.net/npm/d3@7"
                      "https://cdn.jsdelivr.net/npm/d3-require@1"]}
   :jquery      {:js ["https://code.jquery.com/jquery-3.6.0.min.js"
                      "https://code.jquery.com/ui/1.13.1/jquery-ui.min.js"]}
   ;; it might be better for users to provide the urls?
   :three-d-mol {:js ["https://cdnjs.cloudflare.com/ajax/libs/3Dmol/1.5.3/3Dmol.min.js"]}
   :leaflet     {;; fetching Leaflet from the web
                 ;; to avoid fetching the images locally,
                 ;; which would need a bit more care.
                 :js  ["https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"
                       "https://cdn.jsdelivr.net/npm/leaflet-providers@2.0.0/leaflet-providers.min.js"]
                 :css ["https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"]}})

(def kind-deps
  "Resources required for js visualization of the corresponding kind (see kindly kinds)"
  {:vega         {:js ["https://cdn.jsdelivr.net/npm/vega@5.25.0"
                       "https://cdn.jsdelivr.net/npm/vega-lite@5.16.3"
                       "https://cdn.jsdelivr.net/npm/vega-embed@6.22.2"]}
   :datatables   {:deps #{:jquery}
                  :js   ["https://cdn.datatables.net/1.13.6/js/jquery.dataTables.min.js"]
                  :css  ["https://cdn.datatables.net/1.13.6/css/jquery.dataTables.min.css"]}
   :echarts      {:js ["https://cdn.jsdelivr.net/npm/echarts@5.4.1/dist/echarts.min.js"]}
   :cytoscape    {:js ["https://cdnjs.cloudflare.com/ajax/libs/cytoscape/3.23.0/cytoscape.min.js"]}
   :plotly       {:js ["https://cdnjs.cloudflare.com/ajax/libs/plotly.js/2.20.0/plotly.min.js"]}
   ;; Tex is a bit different because it may be required by markdown (not by kind)
   :tex          {:js  ["https://cdn.jsdelivr.net/npm/katex@0.16.10/dist/katex.min.js"]
                  ;; fetching the KaTeX css from the web
                  ;; to avoid fetching the fonts locally,
                  ;; which would need a bit more care
                  ;; (see https://katex.org/docs/font.html)
                  :css ["https://cdn.jsdelivr.net/npm/katex@0.16.10/dist/katex.min.css"]}
   :reagent      {:deps    #{:scittle}
                  :js      ["https://unpkg.com/react@18/umd/react.production.min.js"
                            "https://unpkg.com/react-dom@18/umd/react-dom.production.min.js"
                            "https://daslu.github.io/scittle/js/scittle.reagent.js"]
                  :scittle ['(require '[reagent.core :as r :refer [atom]]
                                      '[reagent.dom :as dom])]}
   :scittle      {:js ["https://daslu.github.io/scittle/js/scittle.js"
                       "https://daslu.github.io/scittle/js/scittle.cljs-ajax.js"]}
   #_#_:tmdjs {:js ["https://daslu.github.io/scittle/js/scittle.tmdjs.js"]}
   #_#_:emmy {:js ["https://daslu.github.io/scittle/js/scittle.emmy.js"]}
   :emmy-viewers {:js  ["https://daslu.github.io/scittle/js/scittle.emmy.js"
                        "https://daslu.github.io/scittle/js/scittle.emmy-viewers.js"]
                  :css ["https://unpkg.com/mafs@0.18.8/core.css"
                        "https://unpkg.com/mafs@0.18.8/font.css"
                        "https://unpkg.com/mathbox@2.3.1/build/mathbox.css"
                        "https://unpkg.com/mathlive@0.85.1/dist/mathlive-static.css"
                        "https://unpkg.com/mathlive@0.85.1/dist/mathlive-fonts.css"]}
   #_#_:mathbox {:js ["https://daslu.github.io/scittle/js/scittle.mathbox.js"]}
   #_#_:portal {:js [portal/url]}
   #_#_:htmlwidgets-ggplotly {:js  {:from-local-copy-of-repo
                                    [{:gh-repo       "scicloj/ggplotly-deps"
                                      :relative-path "lib"
                                      :paths         ["htmlwidgets-1.6.2/htmlwidgets.js"
                                                      "plotly-binding-4.10.4.9000/plotly.js"
                                                      "typedarray-0.1/typedarray.min.js"
                                                      "jquery-3.5.1/jquery.min.js"
                                                      "crosstalk-1.2.1/js/crosstalk.min.js"
                                                      "plotly-main-2.11.1/plotly-latest.min.js"]}]}
                              :css {:from-local-copy-of-repo
                                    [{:gh-repo       "scicloj/ggplotly-deps"
                                      :relative-path "lib"
                                      :paths         ["crosstalk-1.2.1/css/crosstalk.min.css"
                                                      "plotly-htmlwidgets-css-2.11.1/plotly-htmlwidgets.css"]}]}}
   :highcharts   {:js ["https://code.highcharts.com/highcharts.js"]}})

(defn resolve-dep
  "Missing deps are ignored because some kinds do not require dependencies"
  [dep]
  (if (keyword? dep)
    (let [k (keyword (name dep))]
      (if (contains? kindly/known-kinds (keyword "kind" (name dep)))
        ;; look up kind dependency
        (get kind-deps k)
        ;; look up javascript dependency
        (or (get js-deps dep)
            (throw (ex-info (str "Unknown dep requested: " dep)
                            {:id  ::unknown-dep
                             :dep dep})))))
    ;; else a map like {:js [...], :css [...], :scittle [...]} with custom resources
    (if (and (map? dep)
             (or (contains? dep :js)
                 (contains? dep :css)
                 (contains? dep :scittle)))
      dep
      (throw (ex-info (str "Invalid dep supplied: " (pr-str dep))
                      {:id  ::invalid-dep
                       :dep dep})))))

(defn resolve-deps-tree
  "Traverses the :deps of deps, putting them ahead.
  This is a topological sort."
  [deps]
  (when-let [deps (seq (keep resolve-dep deps))]
    (-> (resolve-deps-tree (mapcat :deps deps))
        (concat deps))))

(defn notebook-deps
  "Finds all deps from global options and kind usage.
  Notes must already have advice."
  [{:keys [kindly/options]}]
  ;; global options and deps discovered while rendering
  (let [{:keys [deps]} options]
    (-> (set (cond (map? deps) #{deps}
                   (sequential? deps) deps
                   (keyword? deps) #{deps}))
        (into @walk/*deps*))))

(defn deps-resources
  "Given a sequence of deps, resolves them to a single map of js, css, and scittle resources.
  A `dep` is either keywords like `:highcharts`, or maps of resources `{:js [...] :css [...] :scittle [...]}`.
  Returns `{:js [...] :css [...] :scittle [...]}`
  the contents of the vectors are ordered by dependency."
  [deps]
  (apply merge-with into (resolve-deps-tree deps)))

(defn with-resources
  "Adds `:resources` to a notebook, containing `{:js [...] :css [...] :scittle [...]}`.
  The contents of the vectors are ordered by dependency."
  [notebook]
  (assoc notebook :resources (-> (notebook-deps notebook)
                                 (deps-resources))))