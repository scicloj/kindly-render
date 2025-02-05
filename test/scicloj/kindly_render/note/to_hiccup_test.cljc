(ns scicloj.kindly-render.note.to-hiccup-test
  (:require [clojure.test :refer [deftest is testing]]
            [scicloj.kindly-render.note.to-hiccup-js :as to-hiccup-js]
            [scicloj.kindly-render.note.to-hiccup :as to-hiccup]
            [scicloj.kindly.v4.kind :as kind])
  )
(deftest kind_video 
  (is (= 
       [:iframe {:src "https://www.youtube.com/embed/DAQnvAgBma8", :allowfullscreen true, :class "kind-video"}]
       (->
        (to-hiccup/render {:value (kind/video
                                   {:youtube-id "DAQnvAgBma8"})})
        :hiccup))))
