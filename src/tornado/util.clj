(ns tornado.util
  (:require [tornado.types]
            [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.string :as str]
            [tornado.units :as u])
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
  Throws an exception if the optional 2nd argument has a truthy value and the input
  type is none of string, number or CSSUnit instance. Otherwise thís function does not
  do anything and will return the input.

  Correct value:
     (percent->int 0.5)
     => 0.5

  Incorrect (percent-string) value
     (percent->int \"50%\")   ; can be \"50 %\" as well
     => 0.5

  Percent unit:
     (percent->int (tornado.units/percent 35))

  This function does not check, whether a string input is convertible. It would
  potentially throw an error."
  ([value]
   (percent->number value false))
  ([value throw-if-no-match]
   (cond (string? value) (->> value butlast (apply str) edn/read-string (#(/ % 100)) int*)
         (number? value) (int* value)
         (instance? CSSUnit value) (if (= (:compiles-to value) "%")
                                     (int* (/ (:value value) 100))
                                     value)
         :else (if throw-if-no-match
                 (throw (IllegalArgumentException.
                          (str "Not a valid value for conversion from percent to number: " value)))
                 value))))

(defn average
  "Computes the average of 1 or more numbers. Accepts elements, not a sequence of elements."
  ([x] x)
  ([x y] (/ (+ x y) 2))
  ([x y & more] (/ (reduce + (+ x y) more)
                   (+ 2 (count more)))))

(def avg "Alias for \"average\". Takes any number of args, directly, not in a sequence."
  average)

(defn between
  "Returns true if value is smaller than or equal n1 and greater than or equal n2."
  [value n1 n2]
  (<= n1 value n2))

(def ^:private base16-chars "0123456789ABCDEF")
(def ^:private lowercase-base16 "abcdef")
(def ^:private uppercase-base16-set (set "ABCDEF"))
(def ^:private lowercase-uppercase-difference (- (int \a) (int \A)))
(defn- toLower [c]
  (if (contains? uppercase-base16-set c)
    (char (+ (int c) lowercase-uppercase-difference))
    c))
(def base16->base10-map (merge (zipmap base16-chars (range 16))
                               (zipmap lowercase-base16 (range 10 16))))
(def base10->base64-map (set/map-invert (reduce dissoc
                                                base16->base10-map
                                                lowercase-base16)))

(def double-hex->base10-map
  "A precalculated map for very fast conversions of hexadecimal strings in format \"xx\"
  to base10 numbers (more than 10^6 per second)."
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

(defn insert-at
  "(insert at [:a :b :c :d] 2 :val])
    => [:a :b :val :c :d]"
  [vect index value]
  (concat (subvec vect 0 index)
          [value]
          (subvec vect index)))

(defn str-spacejoin
  "str/join with \" \""
  [coll]
  (str/join " " coll))

(defn str-commajoin
  "str/join with \", \""
  [coll]
  (str/join ", " coll))