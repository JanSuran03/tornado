(ns tornado.test.units
  (:require [clojure.test :refer :all]
            [tornado.core :refer :all]))

(def cex compile-expression)
(defunit celsius "°C")

(defmacro units-test-template [& body]
  `(are [x# y#] (= (cex x#) y#)
                ~@body))

(deftest common-units
  (units-test-template
    (px 10) "10px"
    (vw 1523) "1523vw"))

(deftest special-units
  (units-test-template
    (css-rem 15) "15rem"
    (percent 3.14159) "3.14159%"
    (celsius 25) "25°C"))

(deftest numeric-type-conversion
  (units-test-template
    (vw 5.0) "5vw"
    (ms 9/3) "3ms"
    (cm 5/2) "2.5cm"
    (pt 1.31000) "1.31pt"))
