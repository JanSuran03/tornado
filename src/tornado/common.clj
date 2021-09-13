(ns tornado.common
  (:require [clojure.string :as str]
            [tornado.util :as util])
  (:import (tornado.types CSScomma-join)))

(defn grid-areas
  "Given a sequence of grid-rows sequences, where each the element is represented by
  a keyword, a string or a symbol, return a grid-areas string:

  (grid-areas [(repeat 3 :header) [:. :content :.] (repeat 3 :footer)])
  Output CSS string: ``\"header header header\" \". content .\" \"footer footer footer\"``
  "
  [[first-row & more :as all-rows]]
  (assert (and (every? sequential? all-rows)
               (every? #(every? util/valid? %) all-rows))
          "All grid areas must be sequences of symbols, keywords or strings.")
  (let [length (count first-row)]
    (if (every? #(= length (count %)) more)
      (->> all-rows (map (fn [row]
                           (let [row-str (str/join " " (map name row))]
                             (str "\"" row-str "\""))))
           vector)
      (throw (IllegalArgumentException.
               (str "Vectors in all grid rows must have the same length:\n" all-rows))))))

(defn important
  "When the expression is compiled, \" !important\" is appended to it:

  (important [(repeat 3 :auto)]) =>   \"auto auto auto !important\"
  (important :none =>   \"none !important\"
  (important \"yellow\" =>   \"yellow !important\""
  [expr]
  [[expr "!important"]])