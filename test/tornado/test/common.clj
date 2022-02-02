(ns tornado.test.common
  (:require [clojure.test :refer :all]
            [tornado.core :refer :all]))

(defmacro commons-test-template [& body]
  `(are [x# y#] (= (compile-expression x#) y#)
                ~@body))

(deftest test-join
  (commons-test-template
    (join 1) "1px"
    (join 1 2 3 4) "1px 2px 3px 4px"
    (join em 1 2 3 4) "1em 2em 3em 4em"
    (join 1 (px 2) (percent 3) (css-rem 4) 5) "1px 2px 3% 4rem 5px"))

(deftest test-important
  (commons-test-template
    (important :flex) "flex !important"
    (important (px 5)) "5px !important"
    (important [[(join 1 2) :red :black]]) "1px 2px #FF0000 #000000 !important"))

(deftest test-grid-areas
  (commons-test-template
    (grid-areas [(repeat 3 :header) [:. :content :.] (repeat 3 :footer)])
    "\"header header header\" \". content .\" \"footer footer footer\""

    (grid-areas (for [i (range 3)] (repeat 5 (str "xyz-" i))))
    "\"xyz-0 xyz-0 xyz-0 xyz-0 xyz-0\" \"xyz-1 xyz-1 xyz-1 xyz-1 xyz-1\" \"xyz-2 xyz-2 xyz-2 xyz-2 xyz-2\""))