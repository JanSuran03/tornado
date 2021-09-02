(ns tornado.util
  (:require [tornado.types]
            [clojure.edn :as edn]
            [clojure.set :as set])
  (:import (tornado.types CSSUnit)))

(defn keyword->str [k]
  (name k))

(defn valid?
  "Returns tue if the argument is a symbol, a keyword or a string."
  [x]
  (or (keyword? x)
      (string? x)
      (symbol? x)))

(defn get-valid
  "If the argument is a symbol, a keyword or a string, return its string form."
  [x]
  (if (valid? x)
    (name x)
    x))

(defn int*
  "Converts a float to an integer if the value would stay the same.
  A ratio will be converted to a float, or to an integer if possible."
  [^Number x]
  (let [non-ratio (if (ratio? x)
                    (float x)
                    x)
        int-x (int non-ratio)]
    (if (and (float? non-ratio) (== non-ratio int-x))
      int-x
      non-ratio)))


(defn percent*
  "If the given value is a number, convert it to a string with \"%\" appended."
  [value]
  (if (number? value)
    (str (int* (* value 100)) "%")
    value))

(defn percent->number
  "If the argument is a value in percent, convert it to an integer between 0 and 1.

  Correct value:
     (percent->int 0.5)
     => 0.5

  Incorrect (percent-string) value
     (percent->int \"50%\")
     => 0.5"
  [value]
  (cond (string? value) (->> value butlast (apply str) edn/read-string (#(/ % 100)) int*)
        (number? value) (int* value)
        (instance? CSSUnit value) (if (= (:compiles-to value) "%")
                                    (int* (/ (:value value) 100))
                                    value)
        :else value))

(defn average
  ([x] x)
  ([x y] (/ (+ x y) 2))
  ([x y & more] (/ (reduce + (+ x y) more)
                   (+ 2 (count more)))))

(def avg "Alias for \"average\". Takes any number of args, directly, not in a sequence."
  average)

(defn between
  "Returns true if value is smaller than or equal n1 and bigger than or equal n2."
  [value n1 n2]
  (<= n1 value n2))

(def ^:private base16-chars "0123456789ABCDEF")
(def ^:private lowercase-base16 "abcdef")
(def ^:private uppercase-base16-set (set "ABCDEF"))
(def ^:private lowercase-uppercase-difference (- (int \a) (int \A)))
(def ^:private l-u-d lowercase-uppercase-difference)
(defn- toLower [c]
  (if (contains? uppercase-base16-set c)
    (char (+ (int c) l-u-d))
    c))
(def base16->base10-map (merge (zipmap base16-chars (range 16))
                               (zipmap lowercase-base16 (range 10 16))))
(def base10->base64-map (set/map-invert (reduce dissoc
                                                base16->base10-map
                                                lowercase-base16)))

(def double-hex->base10-map
  (->> (for [i (range 16)
             j (range 16)
             :let [i-th (nth base16-chars i)
                   j-th (nth base16-chars j)
                   base10 (+ (* i 16) j)]]
         [[(str i-th j-th) base10]
          [(str (toLower i-th) j-th) base10]
          [(str i-th (toLower j-th)) base10]
          [(str (toLower i-th) (toLower j-th)) base10]])
       (map set)
       (apply concat)
       (into {})))

(def base10->double-hex-map (set/map-invert double-hex->base10-map))