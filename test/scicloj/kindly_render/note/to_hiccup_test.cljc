(ns scicloj.kindly-render.note.to-hiccup-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [scicloj.kindly-render.note.to-hiccup :as to-hiccup]
   [scicloj.kindly.v4.kind :as kind]
   [scicloj.metamorph.ml.rdatasets :as rdatasets]
   [scicloj.kindly-render.note.to-hiccup-inline-js :as to-hiccup-inline-js]
   [scicloj.kindly-render.note.to-hiccup-js :as to-hiccup-js]
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


(deftest kind_video
  (is (=
       [:iframe {:src "https://www.youtube.com/embed/DAQnvAgBma8", :allowfullscreen true, :class "kind-video"}]
       (->
        (to-hiccup/render {:value (kind/video
                                   {:youtube-id "DAQnvAgBma8"})})
        :hiccup))))

(deftest kind-pprint
  (is (=
       [:blockquote
        {:class "kind-pprint"}
        [:pre [:code {:class "sourceCode language-clojure printed-clojure"} "{:a \"test\"}\n"]]]


       (->
        (to-hiccup/render {:value (kind/pprint
                                   {:a "test"})})
        :hiccup))))


(deftest tableplot-works

  (is
   (str/starts-with?
    (->
     (to-hiccup/render {:value
                        (-> (rdatasets/datasets-iris)
                            (plotly/layer-point
                             {:=x :sepal-length
                              :=y :sepal-width}))
                        :render-fn to-hiccup-inline-js/render})
     :hiccup
     (nth 2)
     first

     second)
    "  \n  var clojupyter_loaded_marker")))




(deftest kind-md-works

  (is (=
       [:div {:class "kind-md"} [:h1 {:id "123"} "123"]]
       (:hiccup
        (to-hiccup/render {:value (kind/md "# 123")}))))

  (is (= [:div {:class "kind-md"} [:h1 {:id "123"} "123"]]

         (:hiccup (to-hiccup/render {:value (do (def m (kind/md "# 123")) m)}))))

  (is (=
       [:em "Isn't it??"]

       (->
        (to-hiccup/render {:value
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
          (to-hiccup/render {:value (kind/html
                                     "<div style='height:40px; width:40px; background:purple'></div>")})
          :hiccup))))


(deftest kind-table-works

  (is (= :table
         (->
          (to-hiccup/render {:value (kind/table {:column-names [:a :b] :row-vectors [[1 2]]})
                             :render-fn to-hiccup-js/render})
          :hiccup
          first)))


  (let [hiccup
        (:hiccup
         (to-hiccup/render {:value
                            (kind/table
                             {:column-names [:preferred-language :age]
                              :row-vectors (take 5 people-as-vectors)})
                            :render-fn to-hiccup-js/render}))]


    (is (= [:thead [:tr ":preferred-language" ":age"]] (-> hiccup (nth 2))))
    (is (= 6 (-> hiccup  (nth 3) count))))



  (is (= :table
         (->
          (to-hiccup/render {:value (-> people-as-dataset
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
          (to-hiccup/render {:value (kind/hidden "(+ 1 1)")})
          :hiccup))))



(deftest nil-return-nil
  (is (= ""
         (:hiccup
          (to-hiccup/render {:value nil})))))

(deftest kind-map-works
  (is (=
       [:div
        {:class "kind-map"}
        [:div {:style {:border "1px solid grey", :padding "2px"}} ":a"]
        [:div {:style {:border "1px solid grey", :padding "2px"}} "1"]]
       (:hiccup
        (to-hiccup/render {:value (kind/map {:a 1})
                           :render-fn to-hiccup-js/render})))))


(deftest kind-image-works
  (is
   (str/starts-with?
    (-> (to-hiccup/render {:value image})
        :hiccup
        second
        :src)
    "data:image/png;base64,")))


(deftest nested-image-rendering-is-supported
  (is
   (str/starts-with?
    (->
     (to-hiccup/render {:value
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
     (to-hiccup/render {:value
                        [raw-image raw-image]
                        :render-fn to-hiccup-js/render})
     :hiccup
     (nth 2)
     (nth 2)
     second
     :src)
    "data:image/png;base64,")))



(deftest kind-var-works
  
  (is (= "#'scicloj.kindly-render.note.to-hiccup-test/a"
         (->
          (to-hiccup/render {:value (kind/var '(def a 1))})
          :hiccup))))



(deftest kind-pprint-works
  (is (=
       "{0 1,\n 2 3,\n 4 5,\n 6 7,\n 8 9,\n 10 11,\n 12 13,\n 14 15,\n 16 17,\n 18 19,\n 20 21,\n 22 23,\n 24 25,\n 26 27,\n 28 29}\n"

       (->
        (to-hiccup/render {:value (->> (range 30)
                                       (apply array-map)
                                       kind/pprint)})
        :hiccup
        (nth 2)
        second

        (nth 2)))))

(deftest kind-code-is-working
  (is (= [:pre {:class "kind-code"} [:code {:class "sourceCode"} ["(defn f [x] {:y (+  x 9)})"]]]
         (:hiccup (to-hiccup/render {:value (kind/code "(defn f [x] {:y (+  x 9)})")})))))


(deftest kind-video-is-working
  (is (= [:iframe {:src "https://www.youtube.com/embed/DAQnvAgBma8",
                   :allowfullscreen true,
                   :class "kind-video"}]
         (:hiccup (to-hiccup/render {:value (kind/video
                                             {:youtube-id "DAQnvAgBma8"})})))))

(deftest kind-fn-works-as-expected

  (is (= "3"
         (->
          (to-hiccup/render {:value
                             (kind/fn
                               {:kindly/f (fn [{:keys [x y]}]
                                            (+ x y))
                                :x 1
                                :y 2})
                             :render-fn to-hiccup-js/render})
          :hiccup)))


  (is (= [:p "_unnamed [3 2]:"]
         (->
          (to-hiccup/render {:value
                             (kind/fn
                               {:x (range 3)
                                :y (repeatedly 3 rand)}
                               {:kindly/f tc/dataset})
                             :render-fn to-hiccup-js/render})
          :hiccup
          (nth 2))))


  (is (= "3"
         (->
          (to-hiccup/render {:value
                             (kind/fn
                               [+ 1 2])
                             :render-fn to-hiccup-js/render})

          :hiccup)))



  (is (= [:p "_unnamed [3 2]:"]
         (->
          (to-hiccup/render {:value
                             (kind/fn
                               {:kindly/f tc/dataset
                                :x (range 3)
                                :y (repeatedly 3 rand)})
                             :render-fn to-hiccup-js/render})

          :hiccup
          (nth 2)))))


(deftest options-are-not-checked
  (is (= ""
         (->
          (to-hiccup/render {:value (kind/html "" {:invalid-option 1})})
          :hiccup))))


(deftest simple-kinds-with-form

  (is (= "2"
         (->
          (to-hiccup/render {:form '(+ 1 1)})
          :hiccup))))
  

  
