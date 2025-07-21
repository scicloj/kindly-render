(ns scicloj.kindly-render.note.to-hiccup-inline-js
   "used by clojupyter to render kinds"

   (:require
    [clojure.string :as str]
    [malli.core :as m]
    [malli.error :as me]
    [scicloj.kind-portal.v1.api :as kind-portal]
    [scicloj.kindly-render.note.to-hiccup :as to-hiccup]
    [scicloj.kindly-render.note.to-hiccup-js :as to-hiccup-js]
    [scicloj.kindly-render.notes.js-deps :as js-deps]
    [scicloj.kindly-render.shared.recursives :as recursives]
    [scicloj.kindly-render.shared.util :as util]
    [scicloj.kindly-render.shared.walk :as walk])
   (:import
    [java.security MessageDigest]))

 (defn- pr-str-with-meta [value]
   (binding [*print-meta* true]
     (pr-str value)))


 (defn- malli-schema-for [kind]
   (m/schema
    (case kind
      :kind/fn  [:map {:closed true}  [:kindly/f {:optional true} fn?]]
      :kind/reagent  [:map {:closed true}  [:html/deps {:optional true} [:sequential keyword?]]]
      [:map {:closed true}])))



 (defn- validate-options [note]
   (-> (malli-schema-for
        (:kind note))
       (m/validate (:kindly/options note))))

 (defn md5 [string]
   (let [digest (.digest (MessageDigest/getInstance "MD5") (.getBytes string "UTF-8"))]
     (apply str (map (partial format "%02x") digest))))


 (defn require-js
   "Generates a Hiccup representation of a `<script>` tag that dynamically loads a JavaScript library from
   a given URL and ensures that a specific JavaScript object provided by the library is available before
   executing a rendering command. This is used to include external JavaScript libraries in a Jupyter notebook environment.

   **Parameters:**

   - `url` (String): The URL of the JavaScript library to load.
   - `js-object` (String): The name of the JavaScript object that the library defines (e.g., `'Plotly'`, `'Highcharts'`).
   - `render-cmd` (String): The JavaScript code to execute after the library has been loaded and the object is available.

   **Returns:**

   - A Hiccup vector representing a `<script>` tag containing JavaScript code that loads the library and executes `render-cmd` once the library is loaded."
   [url render-cmd]
   (let [url-md5 (md5 url)
         render-cmd (str/replace render-cmd "XXXXX" url-md5)]
     [:script
      (format
       "
  var clojupyter_loaded_marker_%s;

  var currentScript_%s = document.currentScript;
  if (typeof clojupyter_loaded_marker_%s === 'undefined') {
      clojupyter_loadScript_%s = src => new Promise(resolve => {
      clojupyter_script_%s = document.createElement('script');
      clojupyter_script_%s.src = src;
      clojupyter_script_%s.async = false;
      clojupyter_script_%s.addEventListener('load', resolve);
      document.head.appendChild(clojupyter_script_%s);
      });

     clojupyter_promise_%s=clojupyter_loadScript_%s('%s');

     Promise.all([clojupyter_promise_%s]).then(() => {
       console.log('%s loaded');
       clojupyter_loaded_marker_%s = true;
       %s
        })

     } else {
       console.log('%s already loaded');
       %s
     };


 "
       url-md5
       url-md5
       url-md5
       url-md5
       url-md5
       url-md5
       url-md5
       url-md5
       url-md5
       url-md5
       url-md5 url
       url-md5
       url-md5
       url-md5
       render-cmd
       url-md5
       render-cmd)]))


 (defn resolve-deps-tree [kinds options]
   (case (first kinds)
     :kind/reagent [{:js
                     ["https://cdn.jsdelivr.net/npm/scittle@0.6.22/dist/scittle.js"
                      "https://unpkg.com/react@18/umd/react.production.min.js"
                      "https://unpkg.com/react-dom@18/umd/react-dom.production.min.js"
                      "https://cdn.jsdelivr.net/npm/d3-require@1"
                      "https://cdn.jsdelivr.net/npm/scittle@0.6.22/dist/scittle.reagent.js"]}]
     :kind/scittle [{:js ["https://cdn.jsdelivr.net/npm/scittle@0.6.22/dist/scittle.js"
                          "https://cdn.jsdelivr.net/npm/scittle@0.6.22/dist/scittle.cljs-ajax.js"
                          "https://cdn.jsdelivr.net/npm/scittle@0.6.22/dist/scittle.reagent.js"]}]
     (js-deps/resolve-deps-tree kinds options)))


 (defn render-error-if-invalid-options [note]
   (if
    true ;(validate-options note)
     nil
     [:div
      {:style "color:red"}
      (format
       "invalid options '%s' for kind %s : %s"
       (:kindly/options note)
       (:kind note)
       (me/humanize (m/explain
                     (malli-schema-for (:kind note))
                     (:kindly/options note))))]))

 (defn render-with-js [note render-cmd]
   (let [js-deps
         (->> (resolve-deps-tree
               (concat
                (-> note :kindly/options :html/deps)
                [(:kind note)])
               {})
              (map :js)
              flatten
              (remove nil?))]

     (concat
      (map-indexed
       #(require-js %2
                    "")
       (drop-last js-deps))

      [(require-js (last js-deps)
                   render-cmd)])))

 (defn highcharts->hiccup
   "Converts Highcharts chart data into a Hiccup vector that can render the chart within a Jupyter notebook using the Highcharts library. It sets up a `<div>` container and includes the necessary scripts to render the chart.

   **Parameters:**

   - `value` (Map): The Highcharts configuration object representing the chart to render.

   **Returns:**

   - A Hiccup vector containing a `<div>` with specified dimensions and a script that initializes the Highcharts chart with the provided configuration."
   [note]
   [:div {:style {:height "500px"
                  :width "500px"}}
    (render-with-js note
                    (format "Highcharts.chart(currentScript_XXXXX.parentElement, %s);"
                            (util/json-str (:value note))))])

 (defn plotly->hiccup
   "Converts Plotly chart data into a Hiccup vector that can render the chart within a Jupyter notebook using the Plotly library.

   **Parameters:**

   - `value` (Map): The Plotly configuration object representing the chart to render.

   **Returns:**

   - A Hiccup vector containing a `<div>` with specified dimensions and a script that initializes the Plotly chart with the provided configuration."
   [note]

   [:div {:style {:height "500px"
                  :width "500px"}}
    (render-with-js
     note
     (format "Plotly.newPlot(currentScript_XXXXX.parentElement, %s);"
             (util/json-str (:value note))))])

 (defn vega->hiccup [note]
   [:div
    (render-with-js
     note
     (format "vegaEmbed(currentScript_XXXXX.parentElement, %s);"
             (util/json-str (:value note))))])

 (defn cytoscape>hiccup
   "Converts Cytoscape graph data into a Hiccup vector that can render the graph within a Jupyter notebook using the Cytoscape.js library.

   **Parameters:**

   - `value` (Map): The Cytoscape.js configuration object representing the graph to render.

   **Returns:**

   - A Hiccup vector containing a `<div>` with specified dimensions and a script that initializes the Cytoscape graph with the provided configuration."
   [note]
   [:div {:style {:height "500px"
                  :width "500px"}}
    (render-with-js
     note
     (format "
                            value = %s;
                            value['container'] = currentScript_XXXXX.parentElement;
                            cytoscape(value);"
             (util/json-str (:value note))))])

 (defn echarts->hiccup
   "Converts ECharts chart data into a Hiccup vector that can render the chart within a Jupyter notebook using the ECharts library.

   **Parameters:**

   - `value` (Map): The ECharts configuration object representing the chart to render.

   **Returns:**

   - A Hiccup vector containing a `<div>` with specified dimensions and a script that initializes the ECharts chart with the provided configuration."
   [note]
   [:div {:style {:height "500px"
                  :width "500px"}}
    (render-with-js note
                    (format "
                                    var myChart = echarts.init(currentScript_XXXXX.parentElement);
                                    myChart.setOption(%s);"
                            (util/json-str (:value note))))])

 (defn tex->hiccup [note]
   [:div
    (render-with-js note
                    (format
                     "katex.render(%s, currentScript_XXXXX.parentElement, {throwOnError: false});"
                     (util/json-str (format "$%s$" (first (:value note))))))])



 (defn scittle->hiccup-2 [note]
   (concat
    (render-with-js note "" )
    [(->
      (to-hiccup-js/render {:value (:value note)})
      :hiccup)]
    [[:script "scittle.core.eval_script_tags()"]]))


 (defn reagent->hiccup [note]
   (let [id (gensym)]
     [:div


      (render-with-js
       note
       (format "scittle.core.eval_string('(require (quote [reagent.dom]))(reagent.dom/render %s (js/document.getElementById \"%s\"))')"
               (str (:value note))
               (str id)))
      [:div {:id (str id)}]]))

 (defn portal->hiccup [note]
   [:div
    (render-with-js
     note
     (->> {:value note}
          kind-portal/prepare
          pr-str-with-meta
          pr-str
          (format "portal_api.embed().renderOutputItem(
                  {'mime': 'x-application/edn',
                   'text': (() => %s)}
                  , currentScript_XXXXX.parentElement);")))])


 (defn render-js
   "Renders JavaScript-based visualizations by converting the visualization data into Hiccup format and preparing it for display in Clojupyter.

   **Parameters:**

   - `note` (Map): The note containing the visualization data.
   - `value` (Any): The data to render.
   - `->hiccup-fn` (Function): A function that takes `value` and returns a Hiccup vector.

   **Returns:**

   - The `note` map augmented with `:clojupyter` containing the rendered HTML, and `:hiccup` containing the Hiccup representation."
   [note ->hiccup-fn]
   (assoc note :hiccup (->hiccup-fn note)))


 (defmulti render-advice :kind)

 (defn render
   "Used to dispatch rendering to the appropriate `render-advice` method based on the `:kind` of the note.

   **Parameters:**

   - `note` (Map): The note containing the data and metadata to render.

   **Returns:**

   - The result of applying the appropriate `render-advice` method to the note."
   [note]

   (let
    [advised-note (walk/advise-render-style note render-advice)
     error-hiccup-or-nil (render-error-if-invalid-options advised-note)]
     (if error-hiccup-or-nil
       (assoc note
              :hiccup error-hiccup-or-nil)
       advised-note)))

 (defmethod render-advice :default [note]
   (to-hiccup/render-advice note))

 (defmethod render-advice :kind/plotly [note]
   (render-js note  plotly->hiccup))

 (defmethod render-advice :kind/cytoscape [note]
   (render-js note  cytoscape>hiccup))

 (defmethod render-advice :kind/highcharts [note]
   (render-js note   highcharts->hiccup))

 (defmethod render-advice :kind/echarts [note]
   (render-js note  echarts->hiccup))

 (defmethod render-advice :kind/scittle [note]
   (render-js note  scittle->hiccup-2))

 (defmethod render-advice :kind/reagent [note]
   (render-js note  reagent->hiccup))

 (defmethod render-advice :kind/portal [note]
   (render-js note portal->hiccup))

 (defmethod render-advice :kind/vega-lite [note]
   (render-js
    (assoc note :kind :kind/vega)
    vega->hiccup))

 (defmethod render-advice :kind/vega [note]
   (render-js note  vega->hiccup))

 (defmethod render-advice :kind/tex [note]
   (render-js note tex->hiccup))

 (defmethod render-advice :kind/code [note]
   (to-hiccup/render note))

 (defmethod render-advice :kind/hidden [note]
   (to-hiccup/render note))

 (defmethod render-advice :kind/md [note]
   (to-hiccup/render note))

 (defmethod render-advice :kind/html [note]
   (to-hiccup/render note))

 (defmethod render-advice :kind/pprint [note]
   (to-hiccup/render note))

 (defmethod render-advice :kind/image [note]
   (to-hiccup/render note))


 (defmethod render-advice :kind/vector [note]
   (walk/render-data-recursively note render))

 (defmethod render-advice :kind/map [note]
   (walk/render-data-recursively note render))

 (defmethod render-advice :kind/set [note]
   (walk/render-data-recursively note render))

 (defmethod render-advice :kind/seq [note]
   (walk/render-data-recursively note render))

 (defmethod render-advice :kind/hiccup [note]
   (walk/render-hiccup-recursively note render))

 (defmethod render-advice :kind/table [note]
   (walk/render-table-recursively note render))

 (defmethod render-advice :kind/video [note]
   (to-hiccup/render note))

(defmethod render-advice :kind/var [note]
  (to-hiccup/render note))

 (defmethod render-advice :kind/fn [note]
   (recursives/render-kind-fn note render))

(defmethod render-advice :kind/dataset [note]
  (to-hiccup/render note))

(defmethod render-advice :kind/fragment [note]
  (walk/render-fragment-recursively note render))
