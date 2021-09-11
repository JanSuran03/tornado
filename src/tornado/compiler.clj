(ns tornado.compiler
  (:require [tornado.types]
            [tornado.at-rules :as at-rules]
            [tornado.util :as util]
            [clojure.string :as str]
            [tornado.selectors :as sel]
            [tornado.colors :as colors]
            [tornado.units :as u]
            [tornado.compression :as compression])
  (:import (tornado.types CSSUnit CSSAtRule CSSFunction CSSColor
                          CSSCombinator CSSAttributeSelector
                          CSSPseudoClass CSSPseudoElement)
           (clojure.lang Keyword Symbol)))


(declare compile-expression
         expand-at-rule
         css)

(defn general-parser-fn
  "A universal compile function for #'tornado.functions/defcssfn."
  [{:keys [compiles-to args]}]
  (str compiles-to "(" (->> args (map compile-expression)
                            util/str-commajoin) ")"))

(defn conjs [s value]
  (conj (or s #{}) value))

(defmulti compile-selector
          "Compiles a CSS combinator, attribute selector, pseudoclass or pseudoelement."
          class)

#_(defmethod compile-selector CSSCombinator
    )

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
        saturation (util/percent-with-symbol-append saturation)
        lightness (util/percent-with-symbol-append lightness)]
    (str "hsl(" hue ", " saturation ", " lightness ")")))

(defmethod compile-color "hsla"
  [{:keys [value]}]
  (let [{:keys [hue saturation lightness alpha]} value
        saturation (util/percent-with-symbol-append saturation)
        lightness (util/percent-with-symbol-append lightness)
        alpha (util/percent->number alpha)]
    (str "hsla(" hue ", " saturation ", " lightness ", " alpha ")")))

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
             (= (count expr) 1)
             (sequential? (first expr))) (->> expr first (map compile-expression)
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

(defmethod expand-at-rule
  "media"
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

(defn compile-attributes-map [attributes-map]
  (when attributes-map
    (for [[attribute value] attributes-map]
      [(compile-expression attribute) (compile-expression value)])))

(defn attr-map-to-css [attributes-map]
  (when attributes-map
    (->> attributes-map compile-attributes-map
         (map util/str-colonjoin)
         util/str-semicolonjoin)))

;(defn selectors-attributes-string)

(defmacro cartesian-product [& seqs]
  (let [w-bindings (map #(vector (gensym) %) seqs)
        binding-syms (mapv first w-bindings)
        for-bindings (vec (apply concat w-bindings))]
    `(for ~for-bindings ~binding-syms)))

(defmacro apply-cartesian-product [input-seq]
  `(cartesian-product ~@input-seq))

(defn expand-seqs
  "Expands lists and lazy sequences in a nested structure. Always expands the first
   collection. When any more deeply nested collection is neither a list nor a lazy-seq,
   this function does not expand it.
   (expand-seqs [:a :b])
   => (:a :b) ... the first element is anything seqable -> transforms it to a list

   (expand-seqs [[:a :b]])
   => ([:a :b]) ... the 2nd element is neither a list nor a lazy-seq -> does not expand it

   (expand-seqs [(list :a [:b (map identity [:c :d :e])])])
   => (:a [:b (:c :d :e)]) ... 2nd element a vector -> does not expand the nested lazy-seq"
  [coll]
  (mapcat (fn [coll]
            (if (seq? coll)
              (expand-seqs coll)
              (list coll)))
          coll))

(defn selectors-params-children [hiccup]
  (as-> hiccup <> (reduce (fn [{:keys [selectors params children at-media] :as spc-map} hiccup-element]
                            (let [belongs-to (cond (or (sel/id-class-tag? hiccup-element)
                                                       (sel/selector? hiccup-element)) :selectors
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

(defn update-unevaluated-hiccup [hiccup path params]
  (util/conjv hiccup {:path   path
                      :params params}))

(defn simplify-prepared-expanded-hiccup [path-params-vector]
  (->> path-params-vector
       (reduce (fn [params->paths-map {:keys [path params]}]
                 (if (get params->paths-map params)
                   (update params->paths-map params conjs path)
                   (assoc params->paths-map params #{path})))
               {})
       (reduce (fn [final-expanded-hiccup [params selectors-set]]
                 (conj! final-expanded-hiccup {:paths  (vec selectors-set)
                                               :params params}))
               (transient []))
       persistent!))

(defn --css
  "<parents> are in a form of a vector of selectors before the current
             hiccup vector: [:.abc :#def :.ghi ...], can potentially be nil

  <hiccup-vector> is a vector containing selectors, params & children:
  [*sel1* *sel2* *sel3* ... *optional-params-map*
    [*child1*]
    [*child3*]
    [*child2*]
       ...]
  Since each child is a vector and the 2nd argument passed to this function
  is a vector as well, we can call this function recursively infinitely."
  [parents unevaluated-hiccup hiccup-vector]
  (let [{:keys [selectors params children at-media]} (selectors-params-children hiccup-vector)
        maybe-media (when (seq at-media)
                      ;
                      ; (pp/pprint at-media)
                      (->> (cartesian-product (concat parents selectors) at-media)
                           (map (fn [[selector media-rules]]
                                  {:path     selector
                                   :at-media media-rules}))))
        #_unevaluated-hiccup #_(if maybe-media
                                 (reduce conj unevaluated-hiccup maybe-media)
                                 unevaluated-hiccup)]
    (if (seq children)
      (reduce (fn [current-unevaluated-hiccup [selector child]]
                (let [new-parents (util/conjv parents selector)
                      updated-hiccup (update-unevaluated-hiccup current-unevaluated-hiccup new-parents params)]
                  (-css new-parents updated-hiccup (list child))))
              unevaluated-hiccup
              (cartesian-product selectors children))
      (reduce (fn [current-unevaluated-hiccup selector]
                (let [new-parents (util/conjv parents selector)]
                  (update-unevaluated-hiccup current-unevaluated-hiccup new-parents params)))
              unevaluated-hiccup
              selectors))))

(defn -css
  "Given a hiccup element path (parents) and current unevaluated hiccup, this function
  first expands all lists and lazy sequences until it comes across a structure which
  is neither of them. After that, the function recursively goes through all hiccup vectors
  in the expanded hiccup vectors list: The current vector of unevaluated hiccup
  combinations is passed to another expanding function to update it. Then, the updated
  vector is passed to another hiccup vector in that expanded sequence and the another
  expanding function expands its inner again...
  The second expanding function calculates cartesian product of all selectors and
  children in the current vector and attempts to expand all the combinations further,
  if the children is another nested hiccup. It updates the received current unevaluated
  hiccup with a map of the current :path and :params."
  [parents unevaluated-hiccup nested-hiccup-vectors-list]
  (let [expanded-list (expand-seqs nested-hiccup-vectors-list)
        unevaled-hiccup (or unevaluated-hiccup [])]
    (reduce (fn [current-unevaluated-hiccup hiccup-vector]
              (--css parents current-unevaluated-hiccup hiccup-vector))
            unevaled-hiccup
            expanded-list)))

(defn save-stylesheet [path stylesheet]
  (spit path stylesheet))

(defn css
  "Generates CSS from a list of hiccup."
  [css-hiccup-list]
  (->> css-hiccup-list (-css nil nil)
       simplify-prepared-expanded-hiccup))

(defn css-time [x]
  (time (let [_ (css x)])))

(def styles2
  (list
    (list
      [:.abc :#mno {:height (u/vw 25)
                    :width  (u/vh 20)}
       [:.def :.ghi {:width :something-else}]]
      [:.jkl (sel/adjacent-sibling :#stu) :#pqr {:width  (u/percent 15)
                                                 :height (u/percent 25)}])))

(def styles
  (list
    (list
      [:.something
       [:.abc :#def {:width      (u/px 15)
                     :height     (u/percent 20)
                     :margin-top [[(u/px 15) 0 (u/px 20) (u/rem* 3)]]}
        [:.ghi :#jkl {:height (u/fr 15)}]
        [:.mno {:height           (u/px 20)
                :background-color :chocolate}
         [:.pqr (sel/adjacent-sibling :#stu) {:height (u/vw 25)
                                              :width  (u/vh 20)}
          [:.vwx :.yza {:width nil}]]
         (at-rules/at-media {:min-width "500px"
                             :max-width "700px"}
                            [:& {:height (u/px 40)}]
                            [:.abc :#def {:margin-top [[0 "15px" "3rem" "1fr"]]}]
                            [:.ghi {:margin "20px"}
                             [:.jkl {:margin "150pc"}]]
                            [:.mno {:overflow :hidden}])]]]
      [:.something :#something-else :#more-examples! {:width  (u/percent 15)
                                                      :height (u/percent 25)}]
      [:*])))

(def brutal
  (list
    (list
      [:.something
       [:.abc :#def {:width      (u/px 15)
                     :height     (u/percent 20)
                     :margin-top [[(u/px 15) 0 (u/px 20) (u/rem* 3)]]}
        [:.ghi :#jkl {:height (u/fr 15)}]
        [:.mno {:height           (u/px 20)
                :background-color :chocolate}
         [:.pqr (sel/adjacent-sibling :#stu) {:height (u/vw 25)
                                              :width  (u/vh 20)}
          [:.vwx :.yza {:width nil}]]
         (at-rules/at-media {:min-width "500px"
                             :max-width "700px"}
                            [:& {:height (u/px 40)}]
                            [:.abc :#def {:margin-top [[0 "15px" "3rem" "1fr"]]}]
                            [:.ghi {:margin "20px"}
                             [:.jkl {:margin "150pc"}]]
                            [:.mno {:overflow :hidden}])]]]
      (repeat 500 [:.something :#something-else :#more-examples! {:width  (u/percent 15)
                                                                  :height (u/percent 25)}])
      )))