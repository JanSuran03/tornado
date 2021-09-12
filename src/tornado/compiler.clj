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
                          CSSPseudoClass CSSPseudoElement CSSPseudoClassFn)
           (clojure.lang Keyword Symbol)
           (java.util Vector)))

(def -indent
  "General indent used globally for indenting new lines. Double size inside media queries."
  4)

(def indent
  "The actual globally used indent in a string form of *X* spaces."
  (apply str (repeat -indent " ")))

(def ^:dynamic *media-query-parents*
  "Current parents Used for compiling @media to temporarily store parents
  paths for compiling @media changes."
  nil)

(def ^:dynamic *maybe-at-media-indent*
  "Extra indentation when nested inside a media query."
  "")

(def ^:dynamic *keyframes-context* false)

(defmacro with-media-query-parents
  "Temporarily stores current parents paths for compiling @media changes and
  doubles the globally used indent.."
  [*parents* & body]
  `(binding [~'*media-query-parents* ~*parents*
             ~'*maybe-at-media-indent* ~indent]
     ~@body))

(defmacro in-keyframes-context
  "No documentation, since it has no usage yet."
  [& body]
  `(let [~'*keyframes-context* true]
     ~@body))

(declare compile-expression
         compile-at-rule
         expand-hiccup-list-for-compilation
         attr-map-to-css
         simplify-prepared-expanded-hiccup
         compile-all-selectors-params-combinations)

(defn conjs
  "Conj(oin)s to a (potentially empty) set,"
  [s value]
  (conj (or s #{}) value))

(defmulti compile-selector
          "Compiles a CSS combinator, attribute selector, pseudoclass or pseudoelement
          or a selector in a keyword/symbol/string form."
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
  (let [maybe-subvalue (when subvalue (str "\"" subvalue "\""))]
    (str (util/get-valid tag) "[" (util/get-valid attribute) compiles-to maybe-subvalue "]")))

(defmethod compile-selector CSSPseudoClass
  [{:keys [pseudoclass]}]
  (str ":" pseudoclass))

(defmethod compile-selector CSSPseudoElement
  [{:keys [pseudoelement]}]
  (str "::" pseudoelement))

(defmethod compile-selector CSSPseudoClassFn
  [{:keys [compiles-to arg]}]
  (str ":" compiles-to "(" (compile-expression arg) ")"))

(defmethod compile-selector CSSCombinator
  [{:keys [compiles-to children]}]
  (->> children (map #(str compiles-to " " %))
       util/str-spacejoin))

(defn compile-selectors-sequence
  "Given a selectors path, which can contain special selectors, this function
  generates a CSS string from the selectors."
  [selectors-path]
  (as-> selectors-path <> (reduce (fn [selectors next-selector]
                                    (assert (or (sel/selector? next-selector)
                                                (sel/id-class-tag? next-selector))
                                            (str "Expected a selector while compiling: " next-selector))
                                    (let [selectors (if (or (instance? CSSPseudoClass next-selector)
                                                            (instance? CSSPseudoElement next-selector)
                                                            (instance? CSSPseudoClassFn next-selector))
                                                      selectors
                                                      (util/conjv selectors " "))]
                                      (util/conjv selectors (compile-selector next-selector))))
                                  [] <>)
        (apply str <>)
        (subs <> 1)))

(defn compile-selectors
  "Given a sequence of selectors paths, e.g. '([:iframe :.abc] [:#def sel/after :.ghi]),
  this function translates all the selectors paths to CSS and str/joins them with a comma,
  which is a shorthand that can be used in CSS to give different selectors paths the same
  parameters. ... => \"iframe .abc, #def::after .ghi\""
  [selectors-sequences]
  (let [compiled-selectors (->> selectors-sequences (map compile-selectors-sequence)
                                util/str-commajoin)]
    (str *maybe-at-media-indent* compiled-selectors)))

(defmulti compile-color
          "Generates CSS from a color, calls a relevant method to do so depending on the
          color's type:
          \"rgb\", \"rgba\", \"hsl\", \"hsla\", keyword, string, keyword (for keywords,
          tries to get an exact hex-value of the color from colors/default-colors),
          otherwise prints out a warning and returns a string form of that keyword."
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
          "Compiles a CSS record (unit, function, at-rule, color). For 4 different types of
          CSS selectors, there is a different multifunction \"compile-selector\"."
          class)

(defmethod compile-css-record :default
  [record]
  (throw (IllegalArgumentException. (str "Not a valid tornado record: " record " with a class: " (class record)))))

(defmethod compile-css-record CSSUnit
  [{:keys [value compiles-to]}]
  (str (util/int* value) compiles-to))

(defn commajoin
  "Redefining functions/commajoin because there would be a cyclic dependency otherwise."
  [{:keys [compiles-to args]}]
  (str compiles-to "(" (->> args (map compile-expression)
                            util/str-commajoin) ")"))

(defmethod compile-css-record CSSFunction
  [{:keys [compile-fn] :or {compile-fn commajoin} :as CSSFn-record}]
  (compile-fn CSSFn-record))

(defmethod compile-css-record CSSAtRule
  [at-rule-record]
  (compile-at-rule at-rule-record))

(defmethod compile-css-record CSSColor
  [color-record]
  (compile-color color-record))

(defn compile-expression
  "Compiles an expression: a number, string, symbol or a record. If the
  expression is a vector of sequential structures, compiles each of the
  structures and str/joins them with a space."
  [expr]
  (cond (and (keyword? expr) (get colors/default-colors expr)) (get colors/default-colors expr)
        (util/valid? expr) (name expr)
        (number? expr) (util/int* expr)
        (record? expr) (compile-css-record expr)
        (and (vector? expr)
             (every? sequential? expr)) (-> (mapcat #(map compile-expression %) expr)
                                            util/str-spacejoin)
        :else (throw (IllegalArgumentException.
                       (str "Not a CSS unit, CSS function, CSS at-rule, nor a string, a number or"
                            " a sequential structure of sequential structures:\n" expr)))))

(defmacro cartesian-product
  "Given any number of seqs, this function returns a lazy sequence of all possible
  combinations of taking 1 element from each of the input sequences."
  [& seqs]
  (let [w-bindings (map #(vector (gensym) %) seqs)
        binding-syms (mapv first w-bindings)
        for-bindings (vec (apply concat w-bindings))]
    `(for ~for-bindings ~binding-syms)))

(defn compile-attributes-map
  "Compiles an attributes map, returns a sequence of [compiled-attribute compiled-value]."
  [attributes-map]
  (when attributes-map
    (for [[attribute value] attributes-map]
      [(compile-expression attribute) (compile-expression value)])))

(defn attr-map-to-css
  "Compiles an attributes map and translates the compiled data to CSS:
  (attr-map-to-css {:width            (units/percent 50)
                    :margin           [[0 (units/px 15) (units/rem* 3) :auto]]
                    :background-color (colors/rotate-hue \"#ff0000\" 60)}
  => width: 50%;
     margin: 0 15px rem3 auto;
     background-color: hsl(60 1 0.5);"
  [attributes-map]
  (when attributes-map
    (->> attributes-map compile-attributes-map
         (map util/str-colonjoin)
         (map #(str *maybe-at-media-indent* % ";"))
         (str/join (str "\n" indent (when *media-query-parents* indent))))))

(defmulti compile-at-rule
          "Generates CSS from CSSAtRule record: @media, @keyframes, @import, @font-face.

          E.g.:
          #tornado.types.CSSAtRule{:identifier \"media\"
                                   :value      {:rules   {:min-width \"500px\"
                                                           :max-width \"700px\"}
                                                :changes [:.abc {:margin-top \"20px\"}]}}

          Depending on the :identifier (\"media\" in this case), a relevant method is called."
          :identifier)

(defmethod compile-at-rule :default
  [{:keys [identifier] :as at-rule}]
  (throw (IllegalArgumentException. (str "Unknown at-rule identifier: " identifier " of at-rule: " at-rule))))

(def special-media-rules-map
  "A special map for generating media queries rules, e.g.:
  (tornado.at-rules/at-media {:rules {:screen  :only   -> \"only screen\"
                                      :screen  false   -> \"not screen\"
                                      :screen  true    -> \"screen\"} ... "
  {:only (fn [rule] (str "only " (name rule)))
   true  name
   false (fn [rule] (str "not " (name rule)))})

(defmethod compile-at-rule "media"
  [{:keys [value] :as at-media}]
  (let [paths *media-query-parents*
        {:keys [rules changes]} value
        compiled-media-rules (->> (for [[param value] rules]
                                    (let [param-fn (get special-media-rules-map value)]
                                      (if (nil? param-fn)
                                        (if-let [compiled-param (util/valid-or-nil param)]
                                          (let [compiled-unit (compile-expression value)]
                                            (str "(" compiled-param ": " compiled-unit ")"))
                                          (throw (IllegalArgumentException.
                                                   (str "Invalid format of a CSS property: " value " in a map of rules:"
                                                        rules " in at-media compilation of at-media: " at-media))))
                                        (param-fn param))))
                                  (str/join " and "))
        compiled-media-changes (->> (for [parents-path paths]
                                      (-> (expand-hiccup-list-for-compilation parents-path [] changes)
                                          simplify-prepared-expanded-hiccup
                                          compile-all-selectors-params-combinations
                                          (str/replace #" \&" "")))
                                    (str/join "\n\n"))]
    (str "@media " compiled-media-rules " {\n" compiled-media-changes "\n}")))

(defmethod compile-at-rule "font-face"
  [{:keys [value]}]
  (let [compiled-params (->> value (map attr-map-to-css)
                             (str/join (str "\n" indent)))]
    (str "@font-face {\n" indent compiled-params "\n}")))

(defn expand-seqs
  "Expands lists and lazy sequences in a nested structure. Always expands the first
   collection. When any more deeply nested collection is neither a list nor a lazy-seq,
   this function does not expand it.
   (expand-seqs [:a :b])
   => (:a :b) ... the first element is anything seqable -> transforms it to a list

   (expand-seqs [[:a :b]])
   => ([:a :b]) ... the 2nd element is neither a list nor a lazy-seq -> does not expand it

   (expand-seqs [(list :a [:b (map identity [:c :d :e])])])
   => (:a [:b (:c :d :e)]) ... 2nd element a vector -> does not expand the nested lazy-seq.

   See clojure.core/flatten. This function just only expands seqs."
  [coll]
  (mapcat (fn [coll]
            (if (seq? coll)
              (expand-seqs coll)
              (list coll)))
          coll))

(defn selectors-params-children
  "Given a hiccup vector, this function returns a map with keys :selectors, :params,
  :children and :at-media, where each of these keys' value is a vector of those
  elements. Besides :params, which is returned as a map, since there cannot be more
  than 1 params map.
  Including incorrect elements or failing to comply the right order of the elements
  (selector -> & more-selectors -> maybe-params -> & maybe-children & maybe-at-media)
  will throw a detailed error message."
  [hiccup]
  (as-> hiccup <> (reduce (fn [{:keys [selectors params children at-media] :as spc-map} hiccup-element]
                            (let [belongs-to (cond (or (sel/id-class-tag? hiccup-element)
                                                       (sel/selector? hiccup-element)) :selectors
                                                   (and (not (record? hiccup-element))
                                                        (map? hiccup-element)) :params
                                                   (vector? hiccup-element) :children
                                                   (at-rules/at-media? hiccup-element) :at-media
                                                   (at-rules/at-font-face? hiccup-element) :at-font-face
                                                   :else (throw (IllegalArgumentException.
                                                                  (str "Invalid hiccup element: " hiccup-element "\nin"
                                                                       " hiccup: " hiccup "\nNone from a class, id,"
                                                                       " selector, child-vector, at-media CSSAtRule"
                                                                       " instance or a params map."))))]
                              (if (or (and (not= belongs-to :selectors)
                                           (empty? selectors) (not= belongs-to :at-font-face))
                                      (and (= belongs-to :selectors)
                                           (or (seq params) (seq children) (seq at-media)))
                                      (and (= belongs-to :params)
                                           (or (seq params) (seq children) (seq at-media))))
                                (throw (IllegalArgumentException.
                                         (str "Error: Hiccup rules:\nYou have to include at least one selector before"
                                              " params or children.\nIf you include any of params or children, the order"
                                              " has to be selectors -> params -> children.\nYou also cannot include more"
                                              " than one parameters map. At-font-face can be included anywhere in the"
                                              " hiccup vector.\nHiccup received: " hiccup)))
                                (update spc-map belongs-to conj hiccup-element))))
                          {:selectors    []
                           :params       []
                           :children     []
                           :at-media     []
                           :at-font-face []} <>)
        (update <> :params first)))

(defn- update-unevaluated-hiccup
  "An internal function which adds :path, :params map to current unevaluated hiccup vector."
  [hiccup path params]
  (util/conjv hiccup {:path   path
                      :params params}))

(defn simplify-prepared-expanded-hiccup
  "Simplifies the expanded hiccup vector: A new map will be created for every unique
  parameters map or at-media record with {:params {...}, :paths #{...}} (with :at-media
  instead of :params alternatively), where elements with equal params or at-media record
  will be inserted to a set behind the :paths key. This function returns a vector of
  these unique params/at-media maps."
  [path-params-vector]
  (->> path-params-vector
       (reduce (fn [params->paths-map {:keys [path params at-media at-font-face]}]
                 (let [known-at-media (get params->paths-map at-media)]
                   (cond at-font-face (update params->paths-map :font-faces-set conjs at-font-face)
                         (and at-media known-at-media) (update params->paths-map at-media conjs path)
                         (and at-media (not known-at-media)) (assoc params->paths-map at-media #{path})
                         (get params->paths-map params) (update params->paths-map params conjs path)
                         :else (assoc params->paths-map params #{path}))))
               {})
       (reduce (fn [final-expanded-hiccup [params selectors-set]]
                 (cond (at-rules/at-media? params) (conj final-expanded-hiccup {:paths    (vec selectors-set)
                                                                                :at-media params})
                       (= :font-faces-set params) (reduce #(conj %1 {:at-font-face %2}) final-expanded-hiccup selectors-set)
                       :else (conj final-expanded-hiccup {:paths  (vec selectors-set)
                                                          :params params})))
               [])))

(defn reduce-invert
  "Reduce where there is passed [f coll val] instead of [f val coll]."
  [f coll val]
  (reduce f val coll))

(defn expand-hiccup-vector
  "Given a (potentially nil) current parents sequence, unevaluated hiccup combinations
  in a vector the current hiccup vector, which is in a form
  [sel1 maybe-sel2 maybe-sel3 ... {maybe-params-map} [maybe-child1] [maybe-child2] ... ]
  where each child is a hiccup vector as well, this function adds all combinations
  of selectors and descendant children and their selectors together with corresponding
  parameters (or skips the combination of params are nil) to the unevaluated-hiccup
  argument, recursively."
  [parents unevaluated-hiccup hiccup-vector]
  (let [{:keys [selectors params children at-media at-font-face]} (selectors-params-children hiccup-vector)
        maybe-at-media (when (seq at-media)
                         (as-> selectors <> (map (partial util/conjv parents) <>)
                               (cartesian-product <> at-media)
                               (map (fn [[path media-rules]]
                                      {:path     path
                                       :at-media media-rules}) <>)))
        maybe-at-font-face (when (seq at-font-face)
                             (map #(do {:at-font-face %}) at-font-face))
        unevaluated-hiccup (cond->> unevaluated-hiccup maybe-at-media (reduce-invert conj maybe-at-media)
                                    maybe-at-font-face (reduce-invert conj maybe-at-font-face))]
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

(defn compile-selectors-and-params
  "For a current :paths & :params/:at-media map, translates the paths (selectors) which
  all have the equal params map or at-media record to CSS and str/joins them with a comma
  (a shorthand which can be used in CSS). Also translates the params map or at-media
  record to CSS and creates a CSS block from these compiled things, e.g.:
  (compile-selectors-and-params {:paths  #{[:.abc :#def :iframe] [:td :span sel/hover]}
                                 :params {:color   :font-black
                                          :margin  [[(units/px 15) (units/em 2)]]
                                          :display :flex}}
  => .abc #def iframe, td span:hover {
         color: #1A1B1F;
         margin: 15px 2em;
         display: flex;
      }"
  [{:keys [paths params at-media at-font-face]}]
  (cond at-media (with-media-query-parents paths (compile-at-rule at-media))
        at-font-face (compile-css-record at-font-face)
        :else (when params (let [compiled-selectors (compile-selectors paths)
                                 compiled-params (attr-map-to-css params)]
                             (str compiled-selectors " {\n" indent compiled-params "\n" *maybe-at-media-indent* "}")))))

(defn compile-all-selectors-params-combinations
  "Given a prepared hiccup vector (with precalculated and simplified combinations of all
  selectors, children and params, this function generates a CSS string from the data."
  [prepared-hiccup]
  (->> prepared-hiccup (map compile-selectors-and-params)
       (remove nil?)
       (str/join "\n\n")))

(defn expand-hiccup-list-for-compilation
  "Given a hiccup element path (parents) and current unevaluated hiccup, this function
  first expands all lists and lazy sequences until it comes across a structure which
  is neither of them. After that, the function recursively goes through all hiccup vectors
  in the expanded hiccup vectors list:
  The current vector of unevaluated hiccup combinations is passed to another expanding
  function which recursively calculates all combinations of a current hiccup vector by
  calling itself and then this function for deeper expanding again - all combinations of
  its selectors (+ selectors received from parents), params and children. It then
  inserts all these combinations to the unevaluated hiccup and returns it updated with
  the combinations inserted."
  [parents unevaluated-hiccup nested-hiccup-vectors-list]
  (let [expanded-list (expand-seqs nested-hiccup-vectors-list)]
    (reduce (fn [current-unevaluated-hiccup hiccup-vector]
              (expand-hiccup-vector parents current-unevaluated-hiccup hiccup-vector))
            unevaluated-hiccup
            expanded-list)))

(defn css
  "Generates CSS from a list (of lists/lazy-seqs of lists...) of hiccup vectors."
  [css-hiccup-list]
  (->> css-hiccup-list (expand-hiccup-list-for-compilation nil [])
       simplify-prepared-expanded-hiccup
       compile-all-selectors-params-combinations))

(defn compressed-css [css-hiccup-list]
  (->> css-hiccup-list css
       compression/compress))

(defn generate-and-save-css [css-hiccup-list]
  (->> (css css-hiccup-list)
       (spit "C:\\Users\\JanSuran\\Documents\\somecss.txt")))

(defn css-time [x]
  (time (let [_ (css x)])))

(defn css-time* [x]
  (time (let [_ (compressed-css x)])))

(def styles2
  (list
    [:.someselector
     [:.abc :#mno {:height (u/vw 25)
                   :width  (u/vh 20)}
      [:.def :.ghi {:width :something-else}]
      (at-rules/at-media {:min-width (u/px 500)
                          :max-width (u/px 1000)}
                         [:& {:margin-top (u/px 15)}]
                         [:.ghi {:margin "20px"}
                          [:.jkl {:margin "150pc"}]])]
     [:.jkl :.kekw (sel/adjacent-sibling :#stu) :#pqr {:width  (u/percent 15)
                                                       :height (u/percent 25)}]]
    [(at-rules/at-font-face {:src         "url(https://webfonts-xyz.org)"
                             :font-family "Source Sans Pro VF"}
                            {:src         "url(https://webfonts-api.com)"
                             :font-weight [[400 500 600 700 800 900]]}
                            {:font-family "Roboto"})]))

(def styles
  (list
    (list
      [:.something
       [:.abc :#def {:width      (u/px 15)
                     :height     (u/percent 20)
                     :margin-top [[(u/px 15) 0 (u/px 20) (u/css-rem 3)]]}
        [:.ghi :#jkl {:height (u/fr 15)}]
        [:.mno {:height           (u/px 20)
                :background-color :chocolate}
         [:.pqr (sel/adjacent-sibling :#stu) {:height (u/vw 25)
                                              :width  (u/vh 20)}
          [:.vwx :.yza {:width (u/px 100)}]]
         (at-rules/at-media {:min-width (u/px 500)
                             :max-width (u/px 700)}
                            [:& {:height (u/px 40)}]
                            [:.abc :#def {:margin-top [[0 (u/px 15) (u/css-rem 3) (u/fr 1)]]}]
                            [:.ghi {:margin (u/px 20)}
                             [:.jkl {:margin (u/pc 150)}]]
                            [:.mno {:overflow :hidden}])]]]
      [:.something :#something-else :#more-examples! {:width  (u/percent 15)
                                                      :height (u/percent 25)}]
      [:*])))

(def sels [[:#abc :.def sel/after :iframe sel/hover]
           [:#ghi sel/focus (sel/contains-subs :div :class "info")]
           [:.jkl (sel/child-selector :div :p :iframe) :#mno]])