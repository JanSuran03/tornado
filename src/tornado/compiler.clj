(ns tornado.compiler
  (:require [tornado.types]
            [tornado.stylesheet :as stylesheet]
            [tornado.util :as util]
            [clojure.string :as str]
            [tornado.props-vals :as pv]
            [tornado.selectors :as sel]
            [tornado.colors :as colors])
  (:import (tornado.types CSSUnit CSSAtRule CSSFunction CSSColor
                          CSSSelector CSSPseudoClass CSSPseudoElement)))

(def comma ", ")
(def colon ": ")
(def semicolon "; ")
(def indent "  ")
(def left-bracket "{")
(def right-bracket "}")

(defn conjv [vect value]
  (if (sequential? vect)
    (conj (if (vector? vect)
            vect
            (vec vect))
          value)
    (throw (IllegalArgumentException. (str "Not sequential: " vect)))))

(defmacro cartesian-product [& seqs]
  (let [seqs (cond->> seqs (every? #(every? sequential? %) seqs) (apply concat))
        w-bindings (map #(vector (gensym) %) seqs)
        binding-syms (map first w-bindings)
        for-bindings (vec (apply concat w-bindings))]
    `(for ~for-bindings (vector ~@binding-syms))))

(defn contains-num-maps [coll]
  (if (sequential? coll)
    (->> coll (map type)
         (map (partial = (type {})))
         frequencies
         (#(get % true))
         (#(or % 0)))
    (throw (IllegalArgumentException. (str "Not sequential: " coll)))))

(defn check-valid
  "Checks, whether the given prop-or-val of a type \"property\"/\"value\" is a known
  CSS element of the given type. Always returns the given prop-or-val, just gives us
  a warning in the REPL. If the prop-or-val is a string or a record, always proceeds
  without logging anything - only checks for keywords.

  (let [cv check-valid]...
  (cv (CSSUnit. \"px\" 15)           => #tornado.types.CSSUnit{:compiles-to \"px\"
                                                                 :value        15

  (cv \"16rem\")                     => \"16rem\"

  (cv 15)                          \"Warning:... neither a tornado record nor a string.\"
                                   => 15

  (cv \"property\" :width)           => :width

  (cv \"property\" :widtth)          \"Warning: Unknown property: :widtth.\"
                                   => :widtth

  (cv \"value\" :grid-gap)           => :grid-gap

  (cv \"property\" :display)         \"Warning: Unknown property: :display.\"
                                   => :senter
  "
  ([string-or-record]
   (check-valid "none" string-or-record))
  ([prop-or-val type]
   (cond (record? prop-or-val) prop-or-val
         (string? prop-or-val) prop-or-val

         (= type "none")
         (do (println (str "Warning: Function tornado.stylesheet/check-valid was called with a single parameter, "
                           "supposing the parameter being a record or a string, but it is neither of them."))
             prop-or-val)

         :else
         (let [check-set (case type "property" pv/css-properties
                                    "value" pv/css-values
                                    (println "Warning: not a valid identifier for a CSS-property or a CSS-value set:" type))]
           (if check-set
             (do (when-not (contains? check-set prop-or-val)
                   (println (str "Warning: Unknown " type ": " prop-or-val)))
                 prop-or-val)
             prop-or-val)))))

(declare compile-expression
         expand-at-rule
         css)

(defmulti compile-color
          "Generates CSS from a color, calls a relevant method to do so depending on the
          color's type:
          \"rgb\", \"rgba\", \"hsl\", \"hsla\""
          colors/get-color-type)

(defmethod compile-color String
  [color] color)

(defmethod compile-color "rgb"
  [{:keys [value]}]
  (let [{:keys [red green blue]} value]
    (str "rgb(" red ", " green ", " blue ")")))

(defmethod compile-color "rgba"
  [{:keys [value]}]
  (let [{:keys [red green blue alpha]} value
        alpha (util/percent->number alpha)]
    (str "rgba(" red ", " green ", " blue ", " alpha ")")))

(defmethod compile-color "hsl"
  [{:keys [value]}]
  (let [{:keys [hue saturation lightness]} value
        saturation (util/percent* saturation)
        lightness (util/percent* lightness)]
    (str "hsl(" hue ", " saturation ", " lightness ")")))

(defmethod compile-color "hsla"
  [{:keys [value]}]
  (let [{:keys [hue saturation lightness alpha]} value
        saturation (util/percent* saturation)
        lightness (util/percent* lightness)
        alpha (util/percent->number alpha)]
    (str "hsl(" hue ", " saturation ", " lightness ", " alpha ")")))

(defmulti compile-css-record
          "Compiles a CSS record."
          class)

(defmethod compile-css-record :default
  [record]
  (println record)
  (throw (IllegalArgumentException. (str "Not a valid tornado record: " (class record)))))

(defmethod compile-css-record CSSUnit
  [{:keys [value compiles-to]}]
  (str (util/int* value) compiles-to))

(defmethod compile-css-record CSSFunction
  [{:keys [compiles-to args]}]
  (let [css-args (str/join ", " args)]
    (str compiles-to "(" css-args ")")))

(defmethod compile-css-record CSSAtRule
  [at-rule-record]
  (expand-at-rule at-rule-record))

(defmethod compile-css-record CSSPseudoClass
  [{:keys [pseudoclass parent]}]
  (let [css-pseudoclass (str ":" pseudoclass)]
    (str (css parent) css-pseudoclass)))

(defmethod compile-css-record CSSPseudoElement
  [{:keys [pseudoelement parent]}]
  (let [css-pseudoelement (str "::" pseudoelement)]
    (str (css parent) css-pseudoelement)))

(defmethod compile-css-record CSSColor
  [color-record]
  (compile-color color-record))

(defn compile-expression
  [unit-or-fn]
  (cond (string? unit-or-fn) unit-or-fn
        (number? unit-or-fn) (util/int* unit-or-fn)
        (record? unit-or-fn) (compile-css-record unit-or-fn)
        :else (throw (IllegalArgumentException.
                       (str "Not a CSS unit, CSS function, CSS at-rule, nor a string or"
                            " a number:" unit-or-fn)))))

(defmulti expand-at-rule
          "Generates CSS from CSSAtRule record: @media, @keyframes, @import, @font-face.

          E.g.:
          #tornado.types.CSSAtRule{:identifier \"media\"
                                   :value      {:rules   {:min-width \"500px\"
                                                           :max-width \"700px\"}
                                                :changes [:.abc {:margin-top \"20px\"}]}}

          Depending on the :identifier (\"media\" in this case), a relevant method is called."
          :identifier)

(defmethod expand-at-rule :default
  [{:keys [identifier]}]
  (throw (IllegalArgumentException. (str "Unknown at-rule identifier: " identifier))))

(defmethod expand-at-rule "media"
  [{:keys [value]}]
  (let [{:keys [rules changes]} value
        expanded-rules (as-> rules <> (stylesheet/validate-map <>)
                             (for [[prop unit] <>]
                               (let [compiled-property (util/keyword->str prop)
                                     compiled-unit (compile-expression unit)]
                                 (str "(" compiled-property ": " compiled-unit ")")))
                             (str/join " and " <>))]
    (str "@media " expanded-rules "\n"
         indent changes)))

;; rest can return an empty seq
;; next can return nil

(defn compile-selector-and-children
  ""
  [selector children]
  (for [child children]
    (css nil nil child)))

(defn expand-css
  ""
  ([tag-or-selector children])
  ([tag-or-selector params children]))

(defn css
  "Generates CSS from hiccup-like data structures. This is the main function, which is
  called after every further nesting. It then calls other relevant functions with
  arguments depending on its input."
  ([[tag-or-selector maybe-params & maybe-children :as input]]
   (css nil nil input))
  ([parent-tags parent-params [tag-or-selector maybe-params & maybe-children]]
   (when maybe-params
     (some-> maybe-children contains-num-maps
             (#(when (pos? %) (throw (IllegalArgumentException.
                                       (str "Invalid hiccup structure: If a param map is included, it has "
                                            "to be the second element of the vector: " tag-or-selector))))))
     (if-let [params (when (map? maybe-params)
                       maybe-params)]
       ;; with params map
       (if (seq maybe-children)
         ;; with params map and children
         (expand-css tag-or-selector params maybe-children)
         ;; with params map, without children
         (compile-selector-and-children tag-or-selector maybe-children))
       ;; without params map
       (expand-css tag-or-selector (concat [maybe-params] maybe-children))))))