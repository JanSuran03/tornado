(ns tornado.test.compression
  (:require [clojure.test :refer :all]
            [tornado.compression :as compression]))

(deftest compress-whitespace
  (is (= " a b c "
         (compression/compress-whitespace "   a   \n b c   "))))

(deftest strip-whitespace-around
  (is (= "[]{};,:()"
         (compression/strip-whitespace-around " [ ] { } ; , : ( ) "))))

(deftest compress
  (is (= "div{border-color:rgb(255,255,255);}"
       (compression/compress
           "div {
                border-color: rgb( 255, 255, 255);
            }"))))
