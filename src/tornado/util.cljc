(ns tornado.util
  "Utility functions used internally in Tornado."
  (:require [tornado.types :as t]
            [#?(:clj  clojure.edn
                :cljs cljs.reader) :as edn]
            [clojure.set :as set]
            [clojure.string :as str])
  #?(:clj (:import (tornado.types CSSUnit))))

#?(:cljs (def JS-STR-TYPE (type "")))
(def str-type #?(:clj  String
                 :cljs JS-STR-TYPE))

(defn math-round [x]
  #?(:clj  (Math/round (float x))
     :cljs (js/Math.round x)))

(defn math-pow [x y]
  (#?(:clj  Math/pow
      :cljs js/Math.pow) x y))

(defn math-abs [x]
  #?(:clj  (Math/abs (if (ratio? x)
                       (float x)
                       x))
     :cljs js/Math.abs))

(defn parse-float [s]
  (#?(:clj  Float/parseFloat
      :cljs js/parseFloat) s))

(defn str-butlast
  "Returns the given string without the last character."
  [s]
  (subs s 0 (dec (count s))))

(defn exception [arg]
  (throw
    (#?(:clj  IllegalArgumentException.
        :cljs js/Error.) arg)))

(defn named?
  "Returns true if the argument is a symbol, a keyword or a string."
  [x]
  (or (keyword? x)
      (string? x)
      (symbol? x)))

(defn get-str-form
  "If the argument is a symbol, a keyword or a string, returns its string form.
   Otherwise, returns nil."
  [x]
  (when (named? x)
    (name x)))

(defn int*
  "Converts a float to an integer if the value would remain equal. Ratios will
  be converted to a float, or, again, to an integer if possible."
  [x]
  #?(:clj  (let [non-ratio (if (ratio? x)
                             (float x)
                             x)
                 int-x (int non-ratio)]
             (if (and (float? non-ratio) (== non-ratio int-x))
               int-x
               non-ratio))
     :cljs (let [int-x (int x)]
                (if (== int-x x)
                  int-x
                  x))))

(defn ->fixed
  "Rounds a given number x to d decimal digits, or 0 by default."
  ([x] (if (int? x)
         x
         (math-round x)))
  ([x d] (if (zero? d)
           (math-round x)
           (let [scale (math-pow 10 d)]
             (/ (math-round (* x scale)) scale)))))

(defn round-4d
  "Rounds a given number to 4 decimal digits, which should be enough in most cases.
  Used for inaccurate calculations, e.g.: (* 1.0 100)"
  [x]
  (->fixed x 4))

(defn to-percent-float
  "Parses a percentage from a string or multiplies a number with 100 to get
  a percentage value of it:
  (tpf \"12%\") => 12
  (tpf \"3\") => 300
  (tpf 8) => 800"
  [value]
  (assert (or (number? value) (string? value))
          (str "Cannot transform to percent int from a value that is none from"
               " a number or a string: " value))
  (cond (number? value) (int* (* value 100))
        (str/ends-with? value "%") (-> value str-butlast edn/read-string)
        :else (-> value parse-float to-percent-float)))

(defn percent-with-symbol-append
  "Multiplies a number with 100 to get a percentage value of it. Returns
  a string form of it with \"%\" appended. Non-numbers are returned unaffected."
  [value]
  (if (number? value)
    (str (int* (round-4d (* value 100))) "%")
    value))

(defn percent->number
  "If the argument is a value in percent, convert it to a corresponding numeral value.
  Throws an exception if the optional 2nd argument has a truthy value and the input
  type is none of string, number or CSSUnit instance. Otherwise thís function does not
  do anything and will return the input.

  (percent->int 0.5)
  => 0.5

  (percent->int \"75%\")   ; can be \"75 %\" as well
  => 0.75

  (percent->int (tornado.units/percent 35))
  => 0.35"
  ([value]
   (percent->number value false))
  ([value throw-if-no-match]
   (cond (string? value) (if (str/ends-with? value "%")
                           (->> value str-butlast edn/read-string (#(/ % 100)) int*)
                           (edn/read-string value))
         (number? value) (int* value)
         (instance? #?(:clj  CSSUnit
                       :cljs t/CSSUnit) value) (if (= (:compiles-to value) "%")
                                                 (int* (/ (:value value) 100))
                                                 value)
         throw-if-no-match (exception
                             (str "Not a valid value for conversion from percent to number: " value))
         :else value)))

(defn- -average
  "Computes the average of 1 or more numbers."
  ([x] x)
  ([x y] (/ (+ x y) 2))
  ([x y & more] (/ (reduce + (+ x y) more)
                   (+ 2 (count more)))))

(defn average
  "Computes the average of 1 or more numbers. Accepts elements directly,
  use apply-avg for a sequence."
  [& args]
  (let [avg (apply -average args)]
    (round-4d avg)))

(defn apply-avg
  "Same as (apply average coll)"
  [coll]
  (apply average coll))

(defn between
  "Returns true if value is smaller than or equal n1 and greater than or equal n2."
  [value n1 n2]
  (<= (min n1 n2) value (max n1 n2)))

;; HEXCODE TO DECIMAL NUMBERS CONVERSION, USED FOR HEX->RGBA AND RGBA->HEX COLOR CONVERSIONS

(defn char->int [x]
  #?(:clj  (int x)
     :cljs (.charCodeAt x)))

(def base16-chars "0123456789ABCDEF")
(def uppercase-base16-set (set "ABCDEF"))
(def lowercase-uppercase-difference (- (char->int \a) (char->int \A)))
(defn toLower
  "Characters [A-F] will be transformed to [a-f], other chars will be returned unchanged."
  [c]
  (if (contains? uppercase-base16-set c)
    (char (+ (char->int c) lowercase-uppercase-difference))
    c))

(def double-hex->base10-map
  "A precalculated map for very fast conversions of hexadecimal strings in format \"xx\"
  to base10 numbers."
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

(defn double-hex?
  "Returns true if every 2-characters substring of the given string expression
  matches to the hexadecimal characters."
  [str-expr]
  (and (even? (count str-expr))
       (let [pairs (->> str-expr (partition 2) (map #(apply str %)))]
         (every? double-hex->base10-map pairs))))

(defn str-space-join
  "str/join with \" \""
  [coll]
  (str/join " " coll))

(defn str-comma-join
  "str/join with \", \""
  [coll]
  (str/join ", " coll))

(defn str-colon-join
  "str/join with \": \""
  [coll]
  (str/join ": " coll))

(defn conjv
  "Equal to (conj (vec v) value)."
  [v value]
  (cond (sequential? v) (conj (if (vector? v)
                                v
                                (vec v))
                              value)
        (nil? v) [value]
        :else (exception (str "Not sequential, nor `nil`: " v))))

(defn in-range
  "If the value is not in range of min-val and max-val, returns the value of the
  corresponding border which is nearer to the value, otherwise returns the value."
  [val min-val max-val]
  (min max-val (max val min-val)))

(defn some-instance?
  "Returns true if the expression is an instance of any from the given classes."
  [expr & classes]
  (some #(instance? % expr) classes))

(defn prune-nils
  "Given a map, removes all keys having nil value. If the pruned map is empty, returns nil."
  [m]
  (->> m (remove (fn [[_ v]]
                   (nil? v)))
       (into {})
       not-empty))

(def ^:dynamic *compress?*
  "Moved from this ns to util in version 0.2.10 to prevent cyclic dependency needed in `ns-kw->str`."
  false)

(defn ns-kw->str [expr]
  (if (and (keyword? expr)
           (namespace expr))
    (-> (str (namespace expr) (if *compress?* "-" "--") (name expr))
        (str/replace #"\." "-"))
    (name expr)))