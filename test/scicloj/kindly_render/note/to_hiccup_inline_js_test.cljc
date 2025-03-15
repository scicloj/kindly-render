(ns scicloj.kindly-render.note.to-hiccup-inline-js-test
  (:require
   [clojure.string :as str] ;[reagent.core]
   [clojure.test :refer [deftest is]]
   [scicloj.kindly-advice.v1.api :as ka]
   [scicloj.kindly-render.note.to-hiccup :as to-hiccup]
   [scicloj.kindly-render.note.to-hiccup-inline-js :as to-hiccup-inline-js]
   [scicloj.kindly-render.note.to-hiccup-js :as to-hiccup-js]
   [scicloj.kindly-render.shared.walk :as walk]
   [scicloj.kindly.v4.kind :as kind]
   [scicloj.metamorph.ml.rdatasets :as rdatasets]
   [scicloj.tableplot.v1.plotly :as plotly]
   [tablecloth.api :as tc]))


(def raw-image
  (->  "https://upload.wikimedia.org/wikipedia/commons/e/eb/Ash_Tree_-_geograph.org.uk_-_590710.jpg"
       (java.net.URL.)
       (javax.imageio.ImageIO/read)))


(def image
  (kind/image raw-image))


(defn- multi-nth
  [v indexes]
  (reduce (fn [coll idx]
            (nth coll idx))
          v
          indexes))

(def people-as-maps
  (->> (range 29)
       (mapv (fn [_]
               {:preferred-language (["clojure" "clojurescript" "babashka"]
                                     (rand-int 3))
                :age (rand-int 100)}))))

(def people-as-vectors
  (->> people-as-maps
       (mapv (juxt :preferred-language :age))))

(def people-as-dataset
  (tc/dataset people-as-maps))




(def vl-spec
  {:encoding
   {:y {:field "y", :type "quantitative"},
    :size {:value 400},
    :x {:field "x", :type "quantitative"}},
   :mark {:type "circle", :tooltip true},
   :width 400,
   :background "floralwhite",
   :height 100,
   :data {:values "x,y\n1,1\n2,-4\n3,9\n", :format {:type "csv"}}})

(def cs
  (kind/cytoscape
   {:elements {:nodes [{:data {:id "a" :parent "b"} :position {:x 215 :y 85}}
                       {:data {:id "b"}}
                       {:data {:id "c" :parent "b"} :position {:x 300 :y 85}}
                       {:data {:id "d"} :position {:x 215 :y 175}}
                       {:data {:id "e"}}
                       {:data {:id "f" :parent "e"} :position {:x 300 :y 175}}]
               :edges [{:data {:id "ad" :source "a" :target "d"}}
                       {:data {:id "eb" :source "e" :target "b"}}]}
    :style [{:selector "node"
             :css {:content "data(id)"
                   :text-valign "center"
                   :text-halign "center"}}
            {:selector "parent"
             :css {:text-valign "top"
                   :text-halign "center"}}
            {:selector "edge"
             :css {:curve-style "bezier"
                   :target-arrow-shape "triangle"}}]
    :layout {:name "preset"
             :padding 5}}))

(def vega-spec
  {:$schema "https://vega.github.io/schema/vega/v5.json"
   :width 400
   :height 200
   :padding 5
   :data {:name "table"
          :values [{:category :A :amount 28}
                   {:category :B :amount 55}
                   {:category :C :amount 43}
                   {:category :D :amount 91}
                   {:category :E :amount 81}
                   {:category :F :amount 53}
                   {:category :G :amount 19}
                   {:category :H :amount 87}]}
   :signals [{:name :tooltip
              :value {}
              :on [{:events "rect:mouseover"
                    :update :datum}
                   {:events "rect:mouseout"
                    :update "{}"}]}]
   :scales [{:name :xscale
             :type :band
             :domain {:data :table
                      :field :category}
             :range :width
             :padding 0.05
             :round true}
            {:name :yscale
             :domain {:data :table
                      :field :amount}
             :nice true
             :range :height}]
   :axes [{:orient :bottom :scale :xscale}
          {:orient :left :scale :yscale}]
   :marks {:type :rect
           :from {:data :table}
           :encode {:enter {:x {:scale :xscale
                                :field :category}
                            :width {:scale :xscale
                                    :band 1}
                            :y {:scale :yscale
                                :field :amount}
                            :y2 {:scale :yscale
                                 :value 0}}
                    :update {:fill
                             {:value :steelblue}}
                    :hover {:fill
                            {:value :red}}}}})

(def plotly-data
  (let [n 20
        walk (fn [bias]
               (->> (repeatedly n #(-> (rand)
                                       (- 0.5)
                                       (+ bias)))
                    (reductions +)))]
    {:data [{:x (walk 1)
             :y (walk -1)
             :z (map #(* % %)
                     (walk 2))
             :type :scatter3d
             :mode :lines+markers
             :opacity 0.2
             :line {:width 10}
             :marker {:size 20
                      :colorscale :Viridis}}]}))

;; (def people-as-maps
;;   (->> (range 29)
;;        (mapv (fn [_]
;;                {:preferred-language (["clojure" "clojurescript" "babashka"]
;;                                      (rand-int 3))
;;                 :age (rand-int 100)}))))

;; (def people-as-vectors
;;   (->> people-as-maps
;;        (mapv (juxt :preferred-language :age))))

;; (def people-as-dataset
;;   (tc/dataset people-as-maps))

;; (defn fetch-dataset [dataset-name]
;;   (-> dataset-name
;;       (->> (format "https://vincentarelbundock.github.io/Rdatasets/csv/%s.csv"))
;;       (tc/dataset {:key-fn (fn [k]
;;                              (-> k
;;                                  str/lower-case
;;                                  (str/replace #"\." "-")
;;                                  keyword))})
;;       (tc/set-dataset-name dataset-name)))

;; (def iris
;;   (fetch-dataset "datasets/iris"))



(deftest kind-vega-lite-works
  (is (= 
       true
       (str/includes?
        (->
         (to-hiccup-inline-js/render {:value (kind/vega-lite vl-spec)})
         :hiccup
         (nth 2)
         (nth 2)
         second)
        "vega")
       )))

;; ;(to-hiccup-inline-js/render {:form })


(deftest kind-vega-works

    (is (= true
           (str/includes?
            (->
             (to-hiccup-inline-js/render {:value (kind/vega vega-spec)})
             :hiccup
             (nth 2)
             (nth 2)
             second)
            "vega"))

        ))



(deftest kind-plotly-works
  (is (= true
         (str/includes?
          (->
           (to-hiccup-inline-js/render {:value (kind/plotly plotly-data)})
           :hiccup
           (nth 2)
           first
           second)
          "Plotly.newPlot")))

  (is (= true
         (str/starts-with?
          (->
           (to-hiccup-inline-js/render {:value (kind/plotly plotly-data {:style {:width 100
                                                                                :height 100}})})
           :hiccup
           (nth 2)
           first
           second)
          "  \n  var"))))

(deftest kind-reagent-works
  (is (= true 
         (str/includes?
          (->
           (to-hiccup-inline-js/render {:value
                                        (kind/reagent
                                         ['(fn [numbers]
                                             [:p {:style {:background "#d4ebe9"}}
                                              (pr-str (map inc numbers))])
                                          (vec (range 10))])})
           :hiccup
           (nth 2)
           (nth 4))
          "reagent.dom/render"))))

(deftest kind-reagent-supports-deps
  (is (= true
         (str/includes?
          (->
           (to-hiccup-inline-js/render {:value
                                        (kind/reagent
                                         ['(fn []
                                             [:div {:style {:height "200px"}
                                                    :ref (fn [el]
                                                           (let [m (-> js/L
                                                                       (.map el)
                                                                       (.setView (clj->js [51.505 -0.09])
                                                                                 13))]
                                                             (-> js/L
                                                                 .-tileLayer
                                                                 (.provider "OpenStreetMap.Mapnik")
                                                                 (.addTo m))
                                                             (-> js/L
                                                                 (.marker (clj->js [51.5 -0.09]))
                                                                 (.addTo m)
                                                                 (.bindPopup "A pretty CSS popup.<br> Easily customizable.")
                                                                 (.openPopup))))}])]
    ;; Note we need to mention the dependency:
                                         {:html/deps [:leaflet]})})
           :hiccup
           (nth 2)
           (nth 5))
          "leaflet.js"))))

(deftest kind-tex-works
  (is 
   (str/includes?
    (->
     (to-hiccup-inline-js/render {:value (kind/tex "x^2 + y^2 = z^2")})
     :hiccup
     (multi-nth []))
    "katex")))



(deftest kind-portal-works
  (is 
   (str/includes?
    (->
     (to-hiccup-inline-js/render { :value 
                                  (kind/portal
                                   {:x (range 3)})}
                                 )
     :hiccup
     (nth 2)
     first
     second)
    ":portal.viewer/inspector")))
       
       
(deftest kind-scittle-works
  (is 
   (str/includes?
    (->
     (to-hiccup-inline-js/render {:value (kind/scittle '(.log js/console "hello"))})
     :hiccup
     (nth 3))
    ".log js/console")) 

  (is (= [:script {:type "application/x-scittle", :class "kind-scittle"} "(print \"hello\")\n"]
         (->
          (to-hiccup-inline-js/render {:value (kind/scittle '(print "hello"))})
          :hiccup
          (nth 3))))

  (is (= [:script "scittle.core.eval_script_tags()"]
         (->
          (to-hiccup-inline-js/render {:value (kind/scittle '(print "hello"))})
          :hiccup
          (nth 4)))) )



(deftest kind-cytoscape-works
  (is 
   (str/includes?
    (->
     (to-hiccup-inline-js/render {:value ^:kind/cytoscape cs})
     :hiccup
     (nth 2)
     first
     second) "cytoscape")))



(deftest tableplot-works

  (is
   (str/starts-with?
    (->
     (to-hiccup-inline-js/render {:value
                        (-> (rdatasets/datasets-iris)
                            (plotly/layer-point
                             {:=x :sepal-length
                              :=y :sepal-width}))})
     :hiccup
     (nth 2)
     first

     second)
    "  \n  var clojupyter_loaded_marker")))




(deftest kind-md-works

  (is (=
       [:div {:class "kind-md"} [:h1 {:id "123"} "123"]]
       (:hiccup
        (to-hiccup-inline-js/render {:value (kind/md "# 123")}))))

  (is (= [:div {:class "kind-md"} [:h1 {:id "123"} "123"]]

         (:hiccup (to-hiccup-inline-js/render {:value (do (def m (kind/md "# 123")) m)}))))

  (is (=
       [:em "Isn't it??"]

       (->
        (to-hiccup-inline-js/render {:value
                           (kind/md

                            "
* This is [markdown](https://www.markdownguide.org/).
  * *Isn't it??*
    * Here is **some more** markdown.
")})
        :hiccup
        (multi-nth [2 1 2 1 1 0])))))



(deftest html-returns-html
  (is (= "<div style='height:40px; width:40px; background:purple'></div>"
         (->
          (to-hiccup-inline-js/render {:value (kind/html
                                     "<div style='height:40px; width:40px; background:purple'></div>")})
          :hiccup))))


(deftest kind-table-works

  (is (= :table
         (->
          (to-hiccup-inline-js/render {:value (kind/table {:column-names [:a :b] :row-vectors [[1 2]]})})
          :hiccup
          first)))


  (let [hiccup
        (:hiccup
         (to-hiccup-inline-js/render {:value
                            (kind/table
                             {:column-names [:preferred-language :age]
                              :row-vectors (take 5 people-as-vectors)})
                            }))]


    (is (= [:thead [:tr ":preferred-language" ":age"]] (-> hiccup (nth 2))))
    (is (= 6 (-> hiccup  (nth 3) count))))



  (is (= :table
         (->
          (to-hiccup-inline-js/render {:value (-> people-as-dataset
                                        (kind/table))})
          :hiccup
          (nth 3)
          first)))

       ;https://github.com/scicloj/kindly-render/issues/29
       ;https://github.com/scicloj/kindly-render/issues/30
       ;https://github.com/scicloj/kindly-render/issues/38

       ;; (k/kind-eval '(kind/table (take 5 people-as-vectors)))

       ;; (k/kind-eval '(kind/table (take 5 people-as-maps)))


       ;; (k/kind-eval '(kind/table {:x (range 6)
       ;;                            :y [:A :B :C :A :B :C]}))

       ;; (k/kind-eval '(-> people-as-maps
       ;;                   tc/dataset
       ;;                   (kind/table {:use-datatables true})))

       ;; (k/kind-eval '(-> people-as-dataset
       ;;                   (kind/table {:use-datatables true})))


       ;; (k/kind-eval '(-> people-as-dataset
       ;;                   (kind/table {:element/max-height "300px"})))

       ;; (k/kind-eval '(-> people-as-maps
       ;;                   tc/dataset
       ;;                   (kind/table {:use-datatables true})))

       ;; (k/kind-eval '(-> people-as-dataset
       ;;                   (kind/table {:use-datatables true})))

       ;; (k/kind-eval '(-> people-as-dataset
       ;;                   (kind/table {:use-datatables true
       ;;                                :datatables {:scrollY 200}})))
  )


(deftest kind-hidden-returns-nothing
  (is (= nil
         (->
          (to-hiccup-inline-js/render {:value (kind/hidden "(+ 1 1)")})
          :hiccup))))



(deftest nil-return-nil
  (is (= ""
         (:hiccup
          (to-hiccup-inline-js/render {:value nil})))))

(deftest kind-map-works
  (is (=
       [:div
        {:class "kind-map"}
        [:div {:style {:border "1px solid grey", :padding "2px"}} ":a"]
        [:div {:style {:border "1px solid grey", :padding "2px"}} "1"]]
       (:hiccup
        (to-hiccup-inline-js/render {:value (kind/map {:a 1})
                                     })))))

(to-hiccup-inline-js/render {:form '(kind/seq [1 2 3])})

(deftest kind-image-works
  (is
   (str/starts-with?
    (-> (to-hiccup-inline-js/render {:value image})
        :hiccup
        second
        :src)
    "data:image/png;base64,")))


(deftest nested-image-rendering-is-supported
  (is
   (str/starts-with?
    (->
     (to-hiccup-inline-js/render {:value
                        (kind/hiccup [:div.clay-limit-image-width
                                      raw-image])
                        :render-fn to-hiccup-js/render})
     :hiccup
     (nth 2)
     second
     :src)

    "data:image/png;base64,"))

  (is
   (str/starts-with?
    (->
     (to-hiccup-inline-js/render {:value
                        [raw-image raw-image]
                        :render-fn to-hiccup-js/render})
     :hiccup
     (nth 2)
     (nth 2)
     second
     :src)
    "data:image/png;base64,")))



(deftest kind-var-works

  (is (= "#'user/a"
         (->
          (to-hiccup-inline-js/render {:value (kind/var '(def a 1))})
          :hiccup))))



(deftest kind-pprint-works
  (is (=
       "{0 1,\n 2 3,\n 4 5,\n 6 7,\n 8 9,\n 10 11,\n 12 13,\n 14 15,\n 16 17,\n 18 19,\n 20 21,\n 22 23,\n 24 25,\n 26 27,\n 28 29}\n"

       (->
        (to-hiccup-inline-js/render {:value (->> (range 30)
                                       (apply array-map)
                                       kind/pprint)})
        :hiccup
        (nth 2)
        second

        (nth 2)))))


(deftest kind-video-is-working
  (is (= [:iframe {:src "https://www.youtube.com/embed/DAQnvAgBma8",
                   :allowfullscreen true,
                   :class "kind-video"}]
         (:hiccup (to-hiccup-inline-js/render {:value (kind/video
                                             {:youtube-id "DAQnvAgBma8"})})))))

(deftest kind-fn-works-as-expected

  (is (= "3"
         (->
          (to-hiccup-inline-js/render {:value
                                       (kind/fn
                                         {:kindly/f (fn [{:keys [x y]}]
                                                      (+ x y))
                                          :x 1
                                          :y 2})
                                       })
          :hiccup)))


  (is (= [:p "_unnamed [3 2]:"]
         (->
          (to-hiccup-inline-js/render {:value
                                       (kind/fn
                                         {:x (range 3)
                                          :y (repeatedly 3 rand)}
                                         {:kindly/f tc/dataset})
                                       })
          :hiccup
          (nth 2))))


  (is (= "3"
         (->
          (to-hiccup-inline-js/render {:value
                                       (kind/fn
                                         [+ 1 2])
                                       })

          :hiccup)))



  (is (= [:p "_unnamed [3 2]:"]
         (->
          (to-hiccup-inline-js/render {:value
                                       (kind/fn
                                         {:kindly/f tc/dataset
                                          :x (range 3)
                                          :y (repeatedly 3 rand)})
                                       })

          :hiccup
          (nth 2)))))


(deftest options-are-not-checked
  (is (= ""
         (->
          (to-hiccup-inline-js/render {:value (kind/html "" {:invalid-option 1})})
          :hiccup))))


(deftest simple-kinds-with-form

  (is (= "2"
         (->
          (to-hiccup-inline-js/render {:form '(+ 1 1)})
          :hiccup))))

(deftest fragment-as-seq
  (is (= [:div {:style {:border "1px solid grey", :padding "2px"}} "2"]
         (-> 
          (to-hiccup-inline-js/render {:value (kind/fragment (range 3))})
          :hiccup
          (nth 4)))))
