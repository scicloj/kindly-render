(ns scicloj.kindly-render.note.to-hiccup-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [scicloj.kindly-render.note.to-hiccup :as to-hiccup]
   [scicloj.kindly.v4.kind :as kind]
   ))


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


(deftest video-src
  (is (= "https://www.sample-videos.com/video321/mp4/240/big_buck_bunny_240p_30mb.mp4"
         (->
          (to-hiccup/render {:value
                             (kind/video
                              {:src "https://www.sample-videos.com/video321/mp4/240/big_buck_bunny_240p_30mb.mp4"})})
          :hiccup
          (nth 2)
          second
          :src))))

(to-hiccup/render {:value (kind/code  "(def f [x] {:y (+ x 9)})")})
