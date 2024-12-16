(ns scicloj.kindly-render.notes.js-deps
  (:require [scicloj.kindly.v4.api :as kindly]
            [scicloj.kindly-render.shared.walk :as walk]))

;; TODO: hiccup without js may still require CSS (likely different CSS if so)
;; ... in which case we'd need to make a different set of dependencies.

;; TODO: kindly.css - should it be mandatory or not? if so it should be on CDN somewhere.


;; TODO: use gh-pages published location
;; should these be folded into kindly.css?
(def clay-root "https://raw.githubusercontent.com/scicloj/clay/refs/heads/main/resources/")
(def clay-css ["bootstrap-generated-by-quarto.min.css"
               "bootstrap-toc-customization.css"
               "bootswatch-cosmo-bootstrap.min.css"
               "bootswatch-spacelab-bootstrap-adapted-bg-light.min.css"
               "bootswatch-spacelab-bootstrap.min.css"
               "code.css"
               "loader.css"
               "main.css"
               "md-main.css"
               "table.css"])
(def clay-resources
  {:css (vec (for [css clay-css]
               (str clay-root css)))})

(def js-deps
  "Resources that do not correspond to a kind, but are useful on their own"
  ;; TODO: host kindly css in gh-pages
  {:kindly      {:css ["https://raw.githubusercontent.com/scicloj/kindly-render/refs/heads/main/resources/kindly.css"]}
   :clay        clay-resources
   :bootstrap   {:css ["https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"]}
   :d3          {:js ["https://cdn.jsdelivr.net/npm/d3@7"
                      "https://cdn.jsdelivr.net/npm/d3-require@1"]}
   ;; TODO: make sure this gets brought in for kind/table with datatable options
   :datatables  {:deps #{:jquery}
                 :js   ["https://cdn.datatables.net/1.13.6/js/jquery.dataTables.min.js"]
                 :css  ["https://cdn.datatables.net/1.13.6/css/jquery.dataTables.min.css"]}
   :react       {:js ["https://unpkg.com/react@18/umd/react.production.min.js"
                      "https://unpkg.com/react-dom@18/umd/react-dom.production.min.js"]}
   #_#_:tmdjs {:js ["https://daslu.github.io/scittle/js/scittle.tmdjs.js"]}
   #_#_:emmy {:js ["https://daslu.github.io/scittle/js/scittle.emmy.js"]}
   #_#_:mathbox {:js ["https://daslu.github.io/scittle/js/scittle.mathbox.js"]}
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
  #:kind{:vega         {:js ["https://cdn.jsdelivr.net/npm/vega@5.25.0"
                             "https://cdn.jsdelivr.net/npm/vega-lite@5.16.3"
                             "https://cdn.jsdelivr.net/npm/vega-embed@6.22.2"]}
         :echarts      {:js ["https://cdn.jsdelivr.net/npm/echarts@5.4.1/dist/echarts.min.js"]}
         :cytoscape    {:js ["https://cdnjs.cloudflare.com/ajax/libs/cytoscape/3.23.0/cytoscape.min.js"]}
         :plotly       {:js ["https://cdnjs.cloudflare.com/ajax/libs/plotly.js/2.20.0/plotly.min.js"]}
         ;; Tex is a bit different because it may be required by markdown (not by kind)
         ;; TODO: make sure that we detect it in markdown
         :tex          {:js  ["https://cdn.jsdelivr.net/npm/katex@0.16.10/dist/katex.min.js"]
                        ;; fetching the KaTeX css from the web
                        ;; to avoid fetching the fonts locally,
                        ;; which would need a bit more care
                        ;; (see https://katex.org/docs/font.html)
                        :css ["https://cdn.jsdelivr.net/npm/katex@0.16.10/dist/katex.min.css"]}
         :reagent      {:deps    #{:kind/scittle :react}
                        :scittle ['(require '[reagent.core :as r :refer [atom]]
                                            '[reagent.dom :as dom])]}
         ;; TODO: can we manage scittle in a better way, with versioning, or contribute upstream?
         ;; optional plugins require a custom build of scittle
         ;; this build of scittle depends on react, which should not be the case
         :scittle      {:deps #{:react}
                        :js   ["https://daslu.github.io/scittle/js/scittle.js"
                               "https://daslu.github.io/scittle/js/scittle.cljs-ajax.js"
                               "https://daslu.github.io/scittle/js/scittle.reagent.js"]}
         :emmy-viewers {:deps #{:kind/scittle}
                        :js   ["https://daslu.github.io/scittle/js/scittle.emmy.js"
                               "https://daslu.github.io/scittle/js/scittle.emmy-viewers.js"]
                        :css  ["https://unpkg.com/mafs@0.18.8/core.css"
                               "https://unpkg.com/mafs@0.18.8/font.css"
                               "https://unpkg.com/mathbox@2.3.1/build/mathbox.css"
                               "https://unpkg.com/mathlive@0.85.1/dist/mathlive-static.css"
                               "https://unpkg.com/mathlive@0.85.1/dist/mathlive-fonts.css"]}
         ;; TODO: no remote url exists, publish, or fix upstream
         #_#_:portal {:js [portal/url]}
         ;; TODO: use a github deeplink url
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
  [dep options]
  (if (keyword? dep)
    (if (contains? kindly/known-kinds dep)
      ;; look up kind dependency
      (or (get-in options [:kind-deps dep])
          (get kind-deps dep))
      ;; look up javascript dependency
      (or (get-in options [:js-deps dep])
          (get js-deps dep)
          (throw (ex-info (str "Unknown dep requested: " dep)
                          {:id  ::unknown-dep
                           :dep dep}))))
    ;; else a map like {:js [...], :css [...], :scittle [...]} with custom resources
    (if (and (map? dep)
             (or (contains? dep :js)
                 (contains? dep :css)
                 (contains? dep :scittle)))
      dep
      (throw (ex-info (str "Invalid dep supplied: " (pr-str dep))
                      {:id  ::invalid-dep
                       :dep dep})))))

(defn deps-of-depmaps
  "Collects the deps of deps"
  [depmaps]
  (reduce into #{} (keep :deps depmaps)))

(defn resolve-deps-tree
  "Accepts a sequence of deps (could be keywords or maps).
  Traverses the :deps of deps, putting them ahead.
  This is a topological sort.
  Returns a sequence of maps."
  [deps options]
  (when-let [depmaps (seq (keep #(resolve-dep % options) deps))]
    (-> (resolve-deps-tree (deps-of-depmaps depmaps) options)
        (concat depmaps)
        (distinct))))

(defn notebook-depmaps
  "Returns a sequence of dep maps shaped like `{:js [...] :css [...] :scittle [...]}` ordered by dependency.
  Deps may come from kindly/options of the notebook, kinds of the notes, and kindly/options of notes.
  Properties :package, :placement, and :async may be applied to a dep map, or an individual dep.
  If `package` is a string, it is a relative root path.
  If `package` is `:embed`, fills the script content directly.
  `placement` can be `:head` or `:body` which is where in the document it will be appended."
  [{:as notebook :keys [kindly/options notes]}]
  (let [deps (walk/union-into (walk/optional-deps notebook)
                              (keep :deps notes))]
    (resolve-deps-tree deps options)))
