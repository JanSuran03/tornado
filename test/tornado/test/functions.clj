(ns tornado.test.functions
  (:require [clojure.test :refer :all]
            [tornado.core :refer :all]))

(deftest test-functions
  (are [x y] (= (compile-expression x) y)
             (translateX (px 15)) "translateX(15px)"
             (css-min (px 500) (vw 80)) "min(500px, 80vw)"
             (calc (px 300) :+ (percent 15)) "calc(300px + 15%)"
             (calc (px 300) :add (percent 15)) "calc(300px + 15%)"
             (url "www.example.com") "url(www.example.com)"
             (url "\"www.example.com\"") "url(\"www.example.com\")"))