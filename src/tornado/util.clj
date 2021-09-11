(ns tornado.util
  (:require [tornado.types]
            [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.string :as str])
  (:import (tornado.types CSSUnit)))

(defn str-butlast [s]
  (->> s butlast (apply str)))

(defn valid?
  "Returns true if the argument is a symbol, a keyword or a string."
  [x]
  (or (keyword? x)
      (string? x)
      (symbol? x)))

(defn get-valid
  "If the argument is a symbol, a keyword or a string, returns its string form."
  [x]
  (if (valid? x)
    (name x)
    x))

(defn valid-or-nil
  "If the argument is a symbol, a keyword or a string, returns its valid form.
  Otherwise, return nil."
  [x]
  (when (valid? x)
    (get-valid x)))

(defn int*
  "Converts a float to an integer if the value would remain equal. Ratios will
  be converted to a float, or, again, to an integer if possible."
  [x]
  (let [non-ratio (if (ratio? x)
                    (float x)
                    x)
        int-x (int non-ratio)]
    (if (and (float? non-ratio) (== non-ratio int-x))
      int-x
      non-ratio)))

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
        :else (-> value Float/parseFloat to-percent-float)))

(defn percent-with-symbol-append
  "Multiplies a number with 100 to get a percentage value of it. Returns
  a string form of it with \"%\" appended. Non-numbers are returned unaffected."
  [value]
  (if (number? value)
    (str (int* (* value 100)) "%")
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
         (instance? CSSUnit value) (if (= (:compiles-to value) "%")
                                     (int* (/ (:value value) 100))
                                     value)
         throw-if-no-match (throw (IllegalArgumentException.
                                    (str "Not a valid value for conversion from percent to number: " value)))
         :else value)))

(defn ->fixed
  "Rounds a given number x to d decimal digits, or 0 by default."
  ([x] (if (int? x)
         x
         (Math/round x)))
  ([x d] (if (zero? d)
           (Math/round x)
           (let [scale (Math/pow 10 d)]
             (/ (Math/round (* x scale)) scale)))))

(defn round
  "Rounds a given number to 4 decimal digits, which should be enough in most cases."
  [x]
  (->fixed (float x) 4))

(defn- -average
  "Computes the average of 1 or more numbers."
  ([x] x)
  ([x y] (/ (+ x y) 2))
  ([x y & more] (/ (reduce + (+ x y) more)
                   (+ 2 (count more)))))

(defn average
  "Computes the average of 1 or more numbers. Accepts elements directly,
  use apply-avg for a sequence.."
  [& args]
  (let [avg (apply -average args)]
    (round avg)))

(def avg
  "Alias for \"average\". Takes any number of args, directly, not in a sequence."
  average)

(defn apply-avg
  "Same as (apply average coll)"
  [coll]
  (apply average coll))

(defn between
  "Returns true if value is smaller than or equal n1 and greater than or equal n2."
  [value n1 n2]
  (<= (min n1 n2) value (max n1 n2)))

;; HEXCODE TO DECIMAL NUMBERS CONVERSION, USED FOR HEX->RGBA AND RGBA->HEX COLOR CONVERSIONS
(def ^:private base16-chars "0123456789ABCDEF")
(def ^:private lowercase-base16 "abcdef")
(def ^:private base16-chars-set (set (concat base16-chars lowercase-base16)))
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

(defn double-hex? [expr]
  (and (even? (count expr))
       (let [pairs (->> expr (partition 2) (map #(apply str %)))]
         (every? double-hex->base10-map pairs))))

(defn str-spacejoin
  "str/join with \" \""
  [coll]
  (str/join " " coll))

(defn str-commajoin
  "str/join with \", \""
  [coll]
  (str/join ", " coll))

(defn str-colonjoin
  "str/join with \": \""
  [coll]
  (str/join ": " coll))

(defn conjv
  "Equal to (conj (vec vect) value)."
  [vect value]
  (cond (sequential? vect) (conj (if (vector? vect)
                                   vect
                                   (vec vect))
                                 value)
        (nil? vect) [value]
        :else (throw (IllegalArgumentException. (str "Not sequential, nor `nil`: " vect)))))

(defn in-range
  "If the value is not in range of min-val and max-val, returns the value of the
  corresponding border which is nearer to the value, otherwise returns the value."
  [val min-val max-val]
  (min max-val (max val min-val)))