(ns tornado.compiler
  (:require [tornado.types]
            [tornado.at-rules :as at-rules]
            [tornado.util :as util]
            [clojure.string :as str]
            [tornado.selectors :as sel]
            [tornado.colors :as colors]
            [tornado.units :as u]
            [tornado.compression :as compression]
            [clojure.pprint :as pp])
  (:import (tornado.types CSSUnit CSSAtRule CSSFunction CSSColor
                          CSSCombinator CSSAttributeSelector
                          CSSPseudoClass CSSPseudoElement)
           (clojure.lang Keyword Symbol)))

(def ^:dynamic media-query-context false)
(def ^:dynamic keyframes-context false)

(defmacro in-media-query-context [& body]
  `(let [~'media-query-context true]
     ~@body))

(defmacro in-keyframes-context [& body]
  `(let [~'keyframes-context true]
     ~@body))

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

(defmethod compile-selector Keyword
  [selector]
  (name selector))

(defmethod compile-selector Symbol
  [selector]
  (name selector))

(defmethod compile-selector String
  [selector]
  (name selector))

(defmethod compile-selector CSSAttributeSelector
  [{:keys [compiles-to tag attribute subvalue]}]
  (str (util/get-valid tag) "[" (util/get-valid attribute) compiles-to \" subvalue \" "]"))

(defmethod compile-selector CSSPseudoClass
  [{:keys [pseudoclass]}]
  (str ":" pseudoclass))

(defmethod compile-selector CSSPseudoElement
  [{:keys [pseudoelement]}]
  (str "::" pseudoelement))

(defmethod compile-selector CSSCombinator
  [{:keys [compiles-to children]}]
  (->> children (map #(str compiles-to " " %))
       util/str-spacejoin))

(defn compile-selectors-sequence [selectors-path]
  (as-> selectors-path <> (reduce (fn [selectors next-selector]
                                    (assert (or (sel/selector? next-selector)
                                                (sel/id-class-tag? next-selector))
                                            (str "Expected a selector while compiling: " next-selector))
                                    (let [selectors (if (or (instance? CSSPseudoClass next-selector)
                                                            (instance? CSSPseudoElement next-selector))
                                                      selectors
                                                      (util/conjv selectors " "))]
                                      (util/conjv selectors (compile-selector next-selector))))
                                  [] <>)
        (apply str <>)
        (subs <> 1)))

(defn compile-selectors [selectors-sequences]
  (->> (for [selectors-path selectors-sequences]
         (compile-selectors-sequence selectors-path))
       util/str-commajoin))

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
         (map #(str % ";"))
         (str/join "\n  "))))

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

(declare expand-hiccup-list-for-compilation)

(defn update-unevaluated-hiccup [hiccup path params]
  (util/conjv hiccup {:path   path
                      :params params}))

(defn simplify-prepared-expanded-hiccup [path-params-vector]
  (->> path-params-vector
       (reduce (fn [params->paths-map {:keys [path params at-media]}]
                 (let [known-at-media (get params->paths-map at-media)]
                   (cond (and at-media known-at-media) (update params->paths-map at-media conjs path)
                         (and at-media (not known-at-media)) (assoc params->paths-map at-media #{path})
                         (get params->paths-map params) (update params->paths-map params conjs path)
                         :else (assoc params->paths-map params #{path}))))
               {})
       (reduce (fn [final-expanded-hiccup [params selectors-set]]
                 (if (at-rules/at-media? params)
                   (conj final-expanded-hiccup {:paths    (vec selectors-set)
                                                :at-media params})
                   (conj final-expanded-hiccup {:paths  (vec selectors-set)
                                                :params params})))
               [])))

(defn expand-hiccup-vector
  "<parents> are in a form of a vector of selectors before the current
             hiccup vector: [:.abc :#def :.ghi ...], can potentially be nil

  <hiccup-vector> is a vector containing selectors, params & children:
  [*sel1* *sel2* *sel3* ... *optional-params-map*
    [*child1*]
    [*child3*]
    [*child2*]
       ...]"
  [parents unevaluated-hiccup hiccup-vector]
  (let [{:keys [selectors params children at-media]} (selectors-params-children hiccup-vector)
        maybe-media (when (seq at-media)
                      (->> (let [new-selectors (for [selector selectors]
                                                 (util/conjv parents selector))]
                             (cartesian-product new-selectors at-media))
                           (map (fn [[path media-rules]]
                                  {:path     path
                                   :at-media media-rules}))))
        unevaluated-hiccup (if maybe-media
                             (reduce conj unevaluated-hiccup maybe-media)
                             unevaluated-hiccup)]
    (if (seq children)
      (reduce (fn [current-unevaluated-hiccup [selector child]]
                (let [new-parents (util/conjv parents selector)
                      updated-hiccup (update-unevaluated-hiccup current-unevaluated-hiccup new-parents params)]
                  (expand-hiccup-list-for-compilation new-parents updated-hiccup (list child))))
              unevaluated-hiccup
              (cartesian-product selectors children))
      (reduce (fn [current-unevaluated-hiccup selector]
                (let [new-parents (util/conjv parents selector)]
                  (update-unevaluated-hiccup current-unevaluated-hiccup new-parents params)))
              unevaluated-hiccup
              selectors))))

(defn expand-hiccup-list-for-compilation
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
              (expand-hiccup-vector parents current-unevaluated-hiccup hiccup-vector))
            unevaled-hiccup
            expanded-list)))

(defn save-stylesheet [path stylesheet]
  (spit path stylesheet))

(defn compile-selectors-and-params
  ""
  [{:keys [paths params]}]
  (when params (let [compiled-selectors (compile-selectors paths)
                     compiled-params (attr-map-to-css params)]
                 (str compiled-selectors " {\n  " compiled-params "\n}"))))

(defn compile-all-selectors-params-combinations
  ""
  [prepared-hiccup]
  (->> prepared-hiccup (map compile-selectors-and-params)
       (remove nil?)
       (str/join "\n\n")))

(defn css
  "Generates CSS from a list of hiccup."
  [css-hiccup-list]
  (->> css-hiccup-list (expand-hiccup-list-for-compilation nil nil)
       simplify-prepared-expanded-hiccup
       ; compile-all-selectors-params-combinations
       ))

(defn compressed-css [css-hiccup-list]
  (->> css-hiccup-list css
       compression/compress))

(defn css-time [x]
  (time (let [_ (css x)])))

(defn css-time* [x]
  (time (let [_ (compressed-css x)])))

(def styles2
  (list
    (repeat 1
            [:.someselector
             [:.abc :#mno {:height (u/vw 25)
                           :width  (u/vh 20)}
              [:.def :.ghi {:width :something-else}]
              (at-rules/at-media {:min-width "500px"
                                  :max-width "700px"}
                                 [:.ghi {:margin "20px"}
                                  [:.jkl {:margin "150pc"}]])]
             [:.jkl :.kekw (sel/adjacent-sibling :#stu) :#pqr {:width  (u/percent 15)
                                                               :height (u/percent 25)}]])))

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

(def sels [[:#abc :.def sel/after :iframe sel/hover]
           [:#ghi sel/focus (sel/contains-subs :div :class "info")]
           [:.jkl (sel/child-selector :div :p :iframe) :#mno]])