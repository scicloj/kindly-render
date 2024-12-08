(ns scicloj.kindly-render.notes.js-deps-test
  (:require [clojure.test :refer [deftest is testing]]
            [scicloj.kindly.v4.api :as kindly]
            [scicloj.kindly-render.notes.js-deps :as js-deps]))

(deftest kind-deps-are-known-kinds
  (is (= ()
         (remove kindly/known-kinds
                 (keys js-deps/kind-deps)))))

(deftest deps-exist
  (is (js-deps/resolve-deps-tree (set (concat (keys js-deps/kind-deps)
                                              (keys js-deps/js-deps)))
                                 nil)))
