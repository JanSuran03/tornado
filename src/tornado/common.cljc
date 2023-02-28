(ns tornado.common
  "Common CSS functions which do not belong to any of the other namespaces
  but might be useful in some cases."
  (:require [clojure.string :as str]
            [tornado.util :as util]
            [tornado.units :refer [px]]))

(defn grid-areas
  "Given a sequence of grid-rows sequences, where each the element is represented by
  a keyword, a string or a symbol, return a grid-areas string:

  (grid-areas [(repeat 3 :header) [:. :content :.] (repeat 3 :footer)])
  Output CSS string: ``\"header header header\" \". content .\" \"footer footer footer\"``"
  [[first-row & more :as all-rows]]
  (assert (and (every? sequential? all-rows)
               (every? #(every? util/named? %) all-rows))
          "All grid areas must be sequences of symbols, keywords or strings.")
  (let [length (count first-row)]
    (if (every? #(= length (count %)) more)
      (->> all-rows (map (fn [row]
                           (let [row-str (str/join " " (map util/ns-kw->str row))]
                             (str "\"" row-str "\""))))
           vector)
      (util/exception
        (str "Vectors in all grid rows must have the same length:\n" all-rows)))))

(defn important
  "After the expression is compiled, \" !important\" is appended to it:

  (important [(repeat 3 :auto)]) =>   \"auto auto auto !important\"
  (important :none =>   \"none !important\"
  (important \"yellow\" =>   \"yellow !important\""
  [expr]
  [[expr "!important"]])

(defn join
  "A convenient function for simpler description of margin, padding or any similar CSS
  block which can can look like \"1px 2px 3px 4px\" after compilation. This function
  processes the input to create such a structure for much simpler description of the data.

  Example usage:

  (require '[tornado.compiler :refer [compile-expression]]
           '[tornado.units :refer [em fr]])

  (compile-expression (join 1 2 3))      ; is equal to [[(px 1) (px 2) (px 3)]]
  => \"1px 2px 3px\"

  (compile-expression (join em 1 2 3))      ; is equal to [[(em 1) (em 2) (em 3)]]
  => \"1em 2em 3em\"

  (compile-expression (join (em 3) 15 (fr 4) 3)
  ; is equal to [[(em 3) (px 15) (fr 4) (px 3)]]
  => \"3em 15px 4fr 3px\""
  ([value]
   (px value))
  ([unit-or-value & more-values]
   (if (fn? unit-or-value)
     [(map unit-or-value more-values)]
     [(map #(if (number? %)
              (px %)
              %)
           (cons unit-or-value more-values))])))

(defn css-class
  "Returns a keyword representing a CSS class. Accepts a keyword, a string or a symbol."
  [named]
  (keyword (str "." (name named))))

(defn css-id
  "Returns a keyword representing a CSS id. Accepts a keyword, a string or a symbol."
  [named]
  (keyword (str "#" (name named))))