(ns tornado.compiler
  (:require [tornado.types]
            [tornado.at-rules :as at-rules]
            [tornado.util :as util]
            [clojure.string :as str]
            [tornado.selectors :as sel]
            [tornado.colors :as colors])
  (:import (tornado.types CSSUnit CSSAtRule CSSFunction CSSColor
                          CSSSelector CSSPseudoClass CSSPseudoElement)
           (clojure.lang PersistentArrayMap Keyword Symbol)))

(defonce unevaluated-hiccup (atom []))
(defonce unevaluated-at-media (atom []))

(declare compile-expression
         expand-at-rule
         css)

(defn general-parser-fn
  "A universal compile function for #'tornado.functions/defcssfn."
  [{:keys [compiles-to args]}]
  (str compiles-to "(" (->> args (map compile-expression)
                            util/str-commajoin) ")"))

(defn conjv [vect value]
  (cond (sequential? vect) (conj (if (vector? vect)
                                   vect
                                   (vec vect))
                                 value)
        (nil? vect) [value]
        :else (throw (IllegalArgumentException. (str "Not sequential, nor `nil`: " vect)))))

(defmulti compile-color
          "Generates CSS from a color, calls a relevant method to do so depending on the
          color's type:
          \"rgb\", \"rgba\", \"hsl\", \"hsla\""
          colors/get-color-type)

(defmethod compile-color Symbol
  [color] (name color))

(defmethod compile-color Keyword
  [color]
  (if-let [color* (get colors/default-colors color)]
    color*
    (do (println (str "Warning: Unknown color keyword: " color
                      ", not found in tornado.colors/default-colors."))
        (name color))))

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
  [{:keys [compile-fn] :or {compile-fn #'general-parser-fn} :as CSSFn-record}]
  (compile-fn CSSFn-record))

(defmethod compile-css-record CSSAtRule
  [at-rule-record]
  (expand-at-rule at-rule-record))

#_(defmethod compile-css-record CSSPseudoClass
    [{:keys [pseudoclass parent]}]
    (let [css-pseudoclass (str ":" pseudoclass)]
      (str (css parent) css-pseudoclass)))

#_(defmethod compile-css-record CSSPseudoElement
    [{:keys [pseudoelement parent]}]
    (let [css-pseudoelement (str "::" pseudoelement)]
      (str (css parent) css-pseudoelement)))

(defmethod compile-css-record CSSColor
  [color-record]
  (compile-color color-record))

(defn compile-expression
  "Compilers an expression: a number, string or a record. If the expression is
  a two-times nested structure (lazy-seq in a vector, vector ain a vector etc.),
  compile each of these and concatenate them by #(str/join \" \" %)"
  [expr]
  (cond (and (keyword? expr) (get colors/default-colors expr)) (get colors/default-colors expr)
        (util/valid? expr) (name expr)
        (number? expr) (util/int* expr)
        (record? expr) (compile-css-record expr)
        (and (sequential? expr)
             (= (count expr) 1)) (->> expr first (map compile-expression)
                                      util/str-spacejoin)
        :else (throw (IllegalArgumentException.
                       (str "Not a CSS unit, CSS function, CSS at-rule, nor a string,"
                            " a number or a double nested sequential structure:\n" expr)))))

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
                              (let [compiled-property (if-let [prop (util/valid-or-nil prop)]
                                                        prop
                                                        (throw (IllegalArgumentException.
                                                                 (str "Invalid format of a CSS property: " prop))))
                                    compiled-unit (compile-expression unit)]
                                (str "(" compiled-property ": " compiled-unit ")")))
                            (str/join " and "))]
    (str "@media " expanded-rules "{\n "
         (str/join "\n  " changes) "}")))

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
  (as-> hiccup <> (reduce (fn [{:keys [selectors params children at-media] :as spc-map} hiccup-element]
                            (let [belongs-to (cond (sel/id-class-tag? hiccup-element) :selectors
                                                   (and (not (record? hiccup-element))
                                                        (map? hiccup-element)) :params
                                                   (vector? hiccup-element) :children
                                                   (at-rules/at-media? hiccup-element) :at-media
                                                   :else (throw (IllegalArgumentException.
                                                                  (str "Invalid hiccup element: " hiccup-element "\nNone"
                                                                       " from a class, id, selector, child-vector, "
                                                                       "at-media CSSAtRule instance or a params map."))))]
                              (if (or (and (not= belongs-to :selectors)
                                           (empty? selectors))
                                      (and (= belongs-to :selectors)
                                           (or (seq params) (seq children) (seq at-media)))
                                      (and (= belongs-to :params)
                                           (or (seq params) (seq children) (seq at-media))))
                                (throw (IllegalArgumentException.
                                         (str "Error: Hiccup rules:\nYou have to include at least one selector before"
                                              " params or children.\nIf you include any of params or children, the order"
                                              " has to be selectors -> params -> children.\nYou also cannot include more"
                                              " than one parameters map.")))
                                (update spc-map belongs-to conj hiccup-element))))
                          {:selectors []
                           :params    []
                           :children  []
                           :at-media  []} <>)
        (update <> :params first)))

(declare -css)

(defn insert-to-unevaluated-seq [path params]
  (swap! unevaluated-hiccup conj {:path   path
                                  :params params}))

(defn insert-to-unevaluated-at-media [path at-media]
  (swap! unevaluated-at-media conj {:path   path
                                    :media-record at-media}))

(defn --css
  "<parents> are in a form of a vector of selectors before the current
             hiccup vector: [:.abc :#def :.ghi ...], can potentially be nil

  <hiccup-vector> is a vector containing selectors, params & children:
  [*sel1* *sel2* *sel3* ... *optional-params-map*
    [*child1*]
    [*child3*]
    [*child2*]
       ...]
  Since each child is a vector and that the 3rd argument passed to this
  function is a vector as well, we can call this function recursively
  infinitely."
  [parents hiccup-vector]
  (let [{:keys [selectors params children at-media]} (selectors-params-children hiccup-vector)]
    (when (seq at-media)
      (doseq [media at-media]
        (insert-to-unevaluated-at-media parents media)))
    (if (seq children)
      (doseq [[selector child] (cartesian-product selectors children)
              :let [new-parents (conjv parents selector)]]
        (insert-to-unevaluated-seq new-parents params)
        (-css new-parents (list child)))
      (doseq [selector selectors
              :let [new-parents (conjv parents selector)]]
        (insert-to-unevaluated-seq new-parents params)))))

(defn -css
  "Given a hiccup element path (parents) and params inherited from
  the parents, goes through it recursively and generates CSS from it."
  [parents hiccup-vector]
  (let [expanded-list (expand-seqs hiccup-vector)]
    ;; for each element of a hiccup vector, recursively unwraps it and generates CSS from it
    (doseq [hiccup-vector expanded-list]
      (--css parents hiccup-vector))))

(defn css
  "Generates CSS from a list of hiccup."
  [css-hiccup-list]
  (-css nil css-hiccup-list))

(defn !reset []
  (reset! unevaluated-hiccup [])
  (reset! unevaluated-at-media []))

(defn css!
  [css-hiccup-list]
  (!reset)
  (css css-hiccup-list))