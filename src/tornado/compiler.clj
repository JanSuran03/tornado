(ns tornado.compiler
  (:require [tornado.types]
            [tornado.stylesheet :as stylesheet]
            [tornado.util :as util]
            [clojure.string :as str]
            [tornado.selectors :as sel]
            [tornado.colors :as colors]
            [tornado.units :as u])
  (:import (tornado.types CSSUnit CSSAtRule CSSFunction CSSColor
                          CSSSelector CSSPseudoClass CSSPseudoElement)
           (clojure.lang PersistentArrayMap)))

(def comma ", ")
(def colon ": ")
(def semicolon "; ")
(def indent "  ")
(def left-bracket "{")
(def right-bracket "}")

(defonce unevaluated-hiccup (atom #{}))

(declare compile-expression
         expand-at-rule
         css)
(defn general-parser-fn
  "A universal compile function for #'tornado.functions/defcssfn."
  [{:keys [compiles-to args]}]
  (str compiles-to "(" (->> args (map compile-expression)
                            util/str-commajoin) ")"))

(defn self-compile-CSSFunction [{:keys [compile-fn] :as CSSFn-record}]
  (compile-fn CSSFn-record))

(def vector* "Same as (vec (list* ...))" (comp vec list*))

(defn conjv [vect value]
  (if (sequential? vect)
    (conj (if (vector? vect)
            vect
            (vec vect))
          value)
    (throw (IllegalArgumentException. (str "Not sequential: " vect)))))

(defn contains-num-maps [coll]
  (if (sequential? coll)
    (as-> coll <> (map type <>)
          (frequencies <>)
          (get <> PersistentArrayMap)
          (or <> 0))
    (throw (IllegalArgumentException. (str "Not sequential: " coll)))))

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
  [{:keys [compile-fn] :as cssfn}]
  (compile-fn cssfn))

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
  "Compilers an expression: a number, string or a record. If the expresiion is
  a two-times nested structure (lazy-seq in a vector, vector ain a vector etc.),
  compile each of these and concatenate them by #(str/join \" \" %)"
  [expr]
  (cond (util/valid? expr) (name expr)
        (number? expr) (util/int* expr)
        (record? expr) (compile-css-record expr)
        (and (sequential? expr)
             (= (count expr) 1)) (->> expr first (map compile-expression)
                                      util/str-spacejoin)
        :else (throw (IllegalArgumentException.
                       (str "Not a CSS unit, CSS function, CSS at-rule, nor a string,"
                            " a number or a nested sequential structure:\n" expr)))))

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
        expanded-rules (->> (for [[prop unit] rules]
                              (let [compiled-property (util/keyword->str prop)
                                    compiled-unit (compile-expression unit)]
                                (str "(" compiled-property ": " compiled-unit ")")))
                            (str/join " and "))]
    (str "@media " expanded-rules "{\n  "
         (str/join "\n  " changes) "}")))

(defn expand-css
  ""
  ([tag-or-selector children])
  ([tag-or-selector params children]))

(def spc-err-msg (str "Error: Hiccup rules:\nYou have to include at least one selector before params or "
                      "children.\nIf you include any of params or children, the order has to be selectors"
                      " -> params -> children.\nYou also cannot include more than one parameters map."))

(defmacro cartesian-product [& seqs]
  (let [w-bindings (map #(vector (gensym) %) seqs)
        binding-syms (mapv first w-bindings)
        for-bindings (vec (apply concat w-bindings))]
    `(for ~for-bindings ~binding-syms)))

(defmacro apply-cartesian-product [input-seq]
  `(cartesian-product ~@input-seq))

(defn expand-seqs
  "Expands and concanetates nested sequences (lists and lazy-seqs)."
  [coll]
  (mapcat (fn [coll]
            (if (seq? coll)
              (expand-seqs coll)
              (list coll)))
          coll))

(defn selectors-params-children [hiccup]
  (as-> hiccup <> (reduce (fn [{:keys [selectors params* children] :as spc-map} hiccup-element]
                            (let [belongs-to (cond (sel/id-class-tag? hiccup-element) :selectors
                                                   (map? hiccup-element) :params*
                                                   (vector? hiccup-element) :children
                                                   :else (throw (IllegalArgumentException.
                                                                  (str "Invalid hiccup element: " hiccup-element
                                                                       "\nNone from a class, id, selector, child-vector"
                                                                       " or a params map."))))]
                              (if (or (and (not= belongs-to :selectors)
                                           (empty? selectors))
                                      (and (= belongs-to :selectors)
                                           (or (seq params*) (seq children)))
                                      (and (= belongs-to :params*)
                                           (or (seq params*) (seq children))))
                                (throw (IllegalArgumentException. spc-err-msg))
                                (update spc-map belongs-to conj hiccup-element))))
                          {:selectors []
                           :params*   []
                           :children  []} <>)
        (update <> :params* first)))

(declare -css)

(defn --css [parents params hiccup-vector]
  (let [{:keys [selectors params* children]} (selectors-params-children hiccup-vector)
        new-params (merge params params*)
        children-with-params (if (seq children) (apply-cartesian-product [selectors children])
                                                (apply-cartesian-product [selectors]))]
    (println selectors)
    (println params*)
    (println children)
    (for [child children-with-params]
      (if children
        (do (swap! unevaluated-hiccup conj {:route   child
                                            :params  new-params
                                            :parents parents})
            (-css parents params child))
        (swap! unevaluated-hiccup conj {:route   child
                                        :params  new-params
                                        :parents parents})))))

(defn -css [parents params css-hiccup]
  (let [expanded-list (expand-seqs css-hiccup)]
    (doseq [hiccup-vector expanded-list]
      (--css parents params hiccup-vector))))

(defn css [css-hiccup]
  (-css nil nil css-hiccup))

(defn x []
  (count @unevaluated-hiccup))