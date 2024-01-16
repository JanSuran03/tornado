(ns tornado.compiler
  "The Tornado compiler, where you should only care about these 4 functions:
  css, repl-css, compile-expression, html-style."
  (:require [tornado.types :as t]
            [tornado.at-rules :as at-rules]
            [tornado.util :as util :refer [*compress?*]]
            [clojure.string :as str]
            [tornado.selectors :as sel]
            [tornado.colors :as colors]
            [tornado.compression :as compression]
            #?(:clj [tornado.macros :refer [cartesian-product]]))
  #?(:clj  (:import (tornado.types CSSUnit CSSAtRule CSSFunction CSSColor
                                   CSSCombinator CSSAttributeSelector
                                   CSSPseudoClass CSSPseudoElement CSSPseudoClassFn)
                    (clojure.lang Keyword Symbol))
     :cljs (:require-macros [tornado.macros :refer [cartesian-product]])))

(def CSS-Unit #?(:clj  CSSUnit
                 :cljs t/CSSUnit))

(def CSS-AtRule #?(:clj  CSSAtRule
                   :cljs t/CSSAtRule))

(def CSS-Function #?(:clj  CSSFunction
                     :cljs t/CSSFunction))

(def CSS-Color #?(:clj  CSSColor
                  :cljs t/CSSColor))

(def CSS-Combinator #?(:clj  CSSCombinator
                       :cljs t/CSSCombinator))

(def CSS-Attribute #?(:clj  CSSAttributeSelector
                      :cljs t/CSSAttributeSelector))

(def CSS-PseudoClass #?(:clj  CSSPseudoClass
                        :cljs t/CSSPseudoClass))

(def CSS-PseudoElement #?(:clj  CSSPseudoElement
                          :cljs t/CSSPseudoElement))

(def CSS-PseudoClassFn #?(:clj  CSSPseudoClassFn
                          :cljs t/CSSPseudoClassFn))

(def ^:dynamic *flags*
  "The current flags for a tornado build compilation:

  :indent-length - Specifies, how many indentation spaces should be in the compiled
                   CSS file after any nesting in @rule or params map. Defaults to 4.

  :pretty-print? - Specifies, whether the compiled CSS should be pretty printed.
                   Defaults to true. If set to false, the CSS file will be compressed
                   after compilation (removal of unnecessary characters like spaces
                   and newlines) to make the CSS file a bit smaller.

  :output-to     - Specifies, where the compiled CSS file should be saved."
  {:indent-length 4
   :pretty-print? true
   :output-to     nil})

(defn update-in-keys
  "Given a map or a record, a function, a common partial path and keys which will be
  appended to that path, updates all keys in the given map with that function."
  [m f path & ks]
  (reduce (fn [m k]
            (update-in m (conj path k) f))
          m
          ks))

(def ^:dynamic *indent* (apply str (repeat (:indent-length *flags*) " ")))

(def ^:dynamic *at-media-indent*
  "Extra indentation when nested inside a media query."
  "")

(def ^:dynamic *keyframes-indent*
  "Extra indentation when nested inside keyframes."
  "")

(def ^:dynamic *in-params-context* false)

(declare compile-expression
         compile-at-rule
         expand-hiccup-list-for-compilation
         attr-map-to-css
         compile-prepared-expressions)

(defrecord EvalExpr [selectors expr])

(defmulti compile-selector
          "Compiles a CSS combinator, attribute selector, pseudoclass or pseudoelement
          or a selector in a keyword/symbol/string form."
          #?(:clj  class
             :cljs (fn [sel]
                       (cond (keyword? sel) Keyword
                             (symbol? sel) Symbol
                             (string? sel) util/JS-STR-TYPE
                             (instance? CSS-Attribute sel) CSS-Attribute
                             (instance? CSS-PseudoClass sel) CSS-PseudoClass
                             (instance? CSS-PseudoElement sel) CSS-PseudoElement
                             (instance? CSS-PseudoClassFn sel) CSS-PseudoClassFn
                             (instance? CSS-Combinator sel) CSS-Combinator))))

(defmethod compile-selector Keyword
  [selector]
  (name selector))

(defmethod compile-selector Symbol
  [selector]
  (name selector))

(defmethod compile-selector util/str-type
  [selector]
  (name selector))

(defmethod compile-selector CSS-Attribute
  [{:keys [compiles-to tag attribute subvalue]}]
  (let [maybe-subvalue (when subvalue (str "\"" (name subvalue) "\""))]
    (str (util/get-str-form tag) "[" (util/get-str-form attribute) compiles-to maybe-subvalue "]")))

(defmethod compile-selector CSS-PseudoClass
  [{:keys [pseudoclass]}]
  (str ":" pseudoclass))

(defmethod compile-selector CSS-PseudoElement
  [{:keys [pseudoelement]}]
  (str "::" pseudoelement))

(defmethod compile-selector CSS-PseudoClassFn
  [{:keys [compiles-to arg]}]
  (str ":" compiles-to "(" (compile-expression arg) ")"))

(defmethod compile-selector CSS-Combinator
  [{:keys [compiles-to children]}]
  (->> children (map #(str compiles-to " " (name %)))
       util/str-space-join))

(defn compile-selectors-sequence
  "Given a path of selectors, which can contain special selectors, this function
  generates a CSS string from the selectors."
  [selectors-path]
  (->> selectors-path (reduce (fn [selectors next-selector]
                                (assert (or (sel/selector? next-selector)
                                            (sel/id-class-tag? next-selector))
                                        (str "Expected a selector while compiling: " next-selector))
                                (let [selectors (if (util/some-instance? next-selector CSS-PseudoClass
                                                                         CSS-PseudoClassFn CSS-PseudoElement)
                                                  selectors
                                                  (util/conjv selectors " "))]
                                  (util/conjv selectors (compile-selector next-selector))))
                              [])
       (apply str)
       str/trim))

(defn compile-selectors
  "Given a sequence of selectors paths, e.g. '([:iframe :.abc] [:#def sel/after :.ghi]),
  this function translates all the selectors paths to CSS and str/joins them with a comma,
  which is a shorthand that can be used in CSS to give different selectors paths the same
  parameters. ... => \"iframe .abc, #def::after .ghi\""
  [selectors-sequences]
  (println "SELSEQ = " selectors-sequences)
  (let [compiled-selectors (->> selectors-sequences (map compile-selectors-sequence)
                                util/str-comma-join)]
    (str *at-media-indent* compiled-selectors)))

(defmulti compile-color
          "Generates CSS from a color, calls a relevant method to do so depending on the
          color's type:
          \"rgb\", \"rgba\", \"hsl\", \"hsla\", keyword, string, keyword (for keywords,
          tries to get an exact hex-value of the color from colors/default-colors),
          otherwise prints out a warning and returns a string form of that keyword."
          colors/get-color-type)

(defmethod compile-color "rgb"
  [{:keys [value] :as color}]
  (if *compress?*
    (-> color (update-in-keys util/math-round [:value] :red :green :blue)
        colors/rgb->hex)
    (let [{:keys [red green blue]} value
          [red green blue] (map util/math-round [red green blue])]
      (str "rgb(" red ", " green ", " blue ")"))))

(defmethod compile-color "rgba"
  [{:keys [value] :as color}]
  (if *compress?*
    (-> color (update-in-keys util/math-round [:value] :red :green :blue)
        (update-in [:value :alpha] util/percent->number)
        colors/maybe-without-alpha
        colors/rgb->hex)
    (let [{:keys [red green blue alpha]} value
          alpha (util/percent->number alpha)
          [red green blue] (map util/math-round [red green blue])]
      (str "rgba(" red ", " green ", " blue ", " alpha ")"))))

(defmethod compile-color "hsl"
  [{:keys [value] :as color}]
  (if *compress?*
    (-> color (update-in [:value :hue] util/math-round)
        colors/hsl->rgb
        colors/rgb->hex)
    (let [{:keys [hue saturation lightness]} value
          saturation (util/percent-with-symbol-append saturation)
          lightness (util/percent-with-symbol-append lightness)
          hue (util/math-round hue)]
      (str "hsl(" hue ", " saturation ", " lightness ")"))))

(defmethod compile-color "hsla"
  [{:keys [value] :as color}]
  (if *compress?*
    (-> color (update-in [:value :hue] util/math-round)
        colors/maybe-without-alpha
        colors/hsl->rgb
        colors/rgb->hex)
    (let [{:keys [hue saturation lightness alpha]} value
          saturation (util/percent-with-symbol-append saturation)
          lightness (util/percent-with-symbol-append lightness)
          alpha (util/percent->number alpha)
          hue (util/math-round hue)]
      (str "hsla(" hue ", " saturation ", " lightness ", " alpha ")"))))

(defmulti compile-css-record
          "Compiles a CSS record (unit, function, at-rule, color). For 4 different types of
          CSS selectors, there is a different multifunction \"compile-selector\"."
          #?(:clj  class
             :cljs (fn [record]
                       (cond (instance? CSS-Unit record) CSS-Unit
                             (instance? CSS-Function record) CSS-Function
                             (instance? CSS-AtRule record) CSS-AtRule
                             (instance? CSS-Color record) CSS-Color
                             :else record))))

(defmethod compile-css-record :default
  [record]
  (util/exception (str "Not a valid tornado record: " record " with a class: " #?(:clj  (class record)
                                                                                  :cljs (type record)))))

(defmethod compile-css-record CSS-Unit
  [{:keys [value compiles-to]}]
  (if (zero? value)
    0
    (str (util/int* value) compiles-to)))

(defn comma-join
  "Redefining functions/comma-join because there would be a cyclic dependency otherwise."
  [{:keys [compiles-to args]}]
  (str compiles-to "(" (->> args (map compile-expression)
                            util/str-comma-join) ")"))

(defmethod compile-css-record CSS-Function
  [{:keys [compile-fn] :or {compile-fn comma-join} :as CSSFn-record}]
  (compile-fn CSSFn-record))

(defmethod compile-css-record CSS-AtRule
  [at-rule-record]
  (compile-at-rule nil at-rule-record))

(defmethod compile-css-record CSS-Color
  [color-record]
  (compile-color color-record))

(def calc-keywords
  "A special map for calc keywords."
  {:add "+"
   :sub "-"
   :mul "*"
   :div "/"})

(defn compile-expression
  "Compiles an expression: a number, string, symbol or a record. If the expression is
  a vector of sequential structures, compiles each of the structures and str/joins them
  with a space. Then, str/joins all these str/space-joined structures with a comma.

  E.g.:
  (compile-expression [[(u/px 15) (u/percent 20)] [:red :chocolate]])
  => \"15px 20%, #FF0000 #D2691E\""
  [expr]
  (if-let [as-color-hex (when (util/named? expr)
                          (get colors/default-colors (colors/color->1-word expr)))]
    as-color-hex
    (cond (get calc-keywords expr) (get calc-keywords expr)
          (util/named? expr) (util/ns-kw->str expr)
          (number? expr) (util/int* expr)
          (record? expr) (compile-css-record expr)
          (and (sequential? expr)
               (every? sequential? expr)) (->> expr (map #(->> % (map compile-expression)
                                                               util/str-space-join))
                                               util/str-comma-join)
          :else (util/exception
                  (str "None of a CSS unit, CSS function, CSS at-rule, a keyword a string, a number or"
                       " a sequential structure consisting of more sequential structures:\n" expr)))))

(defn compile-attributes-map
  "Compiles an attributes map, returns a sequence of [compiled-attribute compiled-value]."
  [attributes-map]
  (when-let [attributes-map (util/prune-nils attributes-map)]
    (for [[attribute value] attributes-map]
      [(compile-expression attribute) (binding [*in-params-context* true]
                                        (compile-expression value))])))

(defn attr-map-to-css
  "Compiles an attributes map and translates the compiled data to CSS:
  (attr-map-to-css {:width            (units/percent 50)
                    :margin           [[0 (units/px 15) (units/css-rem 3) :auto]]
                    :background-color (colors/rotate-hue \"#ff0000\" 60)})
  => width: 50%;
     margin: 0 15px rem3 auto;
     background-color: hsl(60 1 0.5);"
  [attributes-map]
  (when attributes-map
    (->> attributes-map compile-attributes-map
         (map util/str-colon-join)
         (map #(str *keyframes-indent* *at-media-indent* % ";"))
         (str/join (str "\n" *indent* *at-media-indent*))
         (str *keyframes-indent*))))

(defn compile-params
  "Given a map of HTML style attributes described in Tornado, compiles all the values
  of the parameters, but the parameters names remain the same. This function is useful
  for Reagent to allow you describing the style with Tornado.
  Example usage:

  (compile-params {:width            (px 500)
                   :background-color (important (rgb 100 150 200))
                   :border           [[(px 1) :solid :black]]
                   :display          :flex})

  => {:width            \"500px\",
      :background-color \"rgb(100, 150, 200) !important\",
      :border           \"1px solid #000000\",
      :display          \"flex\"}"
  [attributes-map]
  (->> (for [[attr val] attributes-map]
         [attr (compile-expression val)])
       (into {})))

(defn html-style
  "Can be used for compilation of a map of style parameters to a single string of html
  style=\"...\" attribute. Receives the styles map as its argument and returns a string
  of compiled style:

  (html-style {:width            (px 500)
               :height           (percent 15)
               :color            :font-black
               :background-color :teal})

  => \"width:500px;height:15%;color:#1A1B1F;background-color:#008080\""
  [attributes-map]
  (as-> attributes-map <> (compile-attributes-map <>)
        (map #(str/join ":" %) <>)
        (str/join ";" <>)))

(defmulti compile-at-rule
          "Generates CSS from a CSSAtRule record, at the moment, these are available:
          at-media, at-keyframes, at-font-face.

          E.g.:
          #tornado.types.CSSAtRule{:identifier \"media\"
                                   :value      {:rules   {:min-width \"500px\"
                                                           :max-width \"700px\"}
                                                :changes [:.abc {:margin-top \"20px\"}]}}

          Depending on the :identifier (\"media\" in this case), a relevant method is called."
          (fn [_selectors {:keys [identifier]}]
            identifier))

(defmethod compile-at-rule :default
  [_selectors {:keys [identifier] :as at-rule}]
  (util/exception (str "Unknown at-rule identifier: " identifier " of at-rule: " at-rule)))

(def special-media-rules-map
  "A special map for generating media queries rules, e.g.:
  (tornado.at-rules/at-media {:rules {:screen  :only   -> \"only screen\"
                                      :screen  false   -> \"not screen\"
                                      :screen  true    -> \"screen\"} ... })"
  {:only #(str "only " (name %))
   true  name
   false #(str "not " (name %))})

(defmethod compile-at-rule "media"
  [selectors {:keys [value] :as at-media}]
  (let [{:keys [rules changes]} value
        compiled-media-rules (->> (for [[param value] rules]
                                    (if-some [param-fn (get special-media-rules-map value)]
                                      (param-fn param)
                                      (if-let [compiled-param (util/get-str-form param)]
                                        (let [compiled-unit (compile-expression value)]
                                          (str "(" compiled-param ": " compiled-unit ")"))
                                        (util/exception
                                          (str "Invalid format of a CSS property: " value " in a map of rules:"
                                               rules " in at-media compilation of @media expression: " at-media)))))
                                  (str/join " and "))
        compiled-media-changes (-> (expand-hiccup-list-for-compilation selectors [] changes)
                                   compile-prepared-expressions
                                   (str/replace #" \&" ""))]
    (str "@media " compiled-media-rules " {\n" compiled-media-changes "\n}")))

(defmethod compile-at-rule "font-face"
  [_selectors {:keys [value]}]
  (let [compiled-params (->> value (map attr-map-to-css)
                             (str/join (str "\n" *indent*)))]
    (str "@font-face {\n" *indent* compiled-params "\n}")))

(defmethod compile-at-rule "keyframes"
  [_selectors {:keys [value]}]
  (let [{:keys [anim-name frames]} value]
    (if *in-params-context*
      anim-name
      (binding [*keyframes-indent* *indent*]
        (let [compiled-frames (->> frames (map (fn [[progress params]]
                                                 (let [compiled-progress (compile-expression progress)
                                                       compiled-params (attr-map-to-css params)]
                                                   (str compiled-progress " {\n"
                                                        compiled-params "\n" *keyframes-indent* "}"))))
                                   (str/join (str "\n" *keyframes-indent*))
                                   (str *keyframes-indent*))]
          (str "@keyframes " anim-name " {\n" compiled-frames "\n}"))))))

(defn flatten-seqs
  "Recursively expands collections for which `(seq? coll)` returns true. Always expands
  the first collection, even if it is e.g. a vector ((seq? []) is false); all more
  deeply nested collections will not be expanded.
  See clojure.core/flatten."
  [coll]
  (mapcat (fn [coll]
            (if (seq? coll)
              (flatten-seqs coll)
              (list coll)))
          coll))

(defn selectors-attributes-children
  "Given a hiccup vector [selector(+) attributes(0|1) #{child, @media, @font-face}(*)],
  asserts the hiccup structure and returns a map of these."
  [hiccup]
  (as-> hiccup <> (reduce (fn [{:keys [state] :as result} element]
                            (let [element-type (cond (or (sel/id-class-tag? element)
                                                         (sel/selector? element)) :selector
                                                     (and (not (record? element))
                                                          (map? element)) :attributes
                                                     (or (vector? element)
                                                         (at-rules/at-media? element)
                                                         (at-rules/at-font-face? element)) :child
                                                     :else (util/exception
                                                             (str "Invalid hiccup element: " element "\nin"
                                                                  " hiccup: " hiccup "\nNone from a class, id,"
                                                                  " selector, child-vector, at-media CSSAtRule"
                                                                  " instance or a params map.")))]
                              (case state
                                :expect-selector (if (identical? element-type :selector)
                                                   (-> result (update :selectors conj element)
                                                       (assoc :state :expect-any))
                                                   (util/exception (str "Error: Expected at least 1 selector in a hiccup vector before other elements: " hiccup)))
                                :expect-any (case element-type
                                              :selector (update result :selectors conj element)
                                              :attributes (assoc result :attributes element :state :expect-children)
                                              :child (-> result (update :children conj element)
                                                         (assoc state :expect-children)))
                                :expect-children (if (identical? element-type :child)
                                                   (update result :children conj element)
                                                   (util/exception (str "Error: Expected child, @media or @font-face, got " element " in: " hiccup))))))
                          {:selectors []
                           :children  []
                           :state     :expect-selector}
                          <>)
        (dissoc <> :state)))

(defn expand-hiccup-vector
  "Given (potentially empty) current parent-selectors vector, pending unevaluated
   hiccup and the next hiccup item in a form
   [selector(+) attributes(0|1) #{child, @media, @font-face}(*)],
   this function walks through the hiccup item and adds expression with selector contexts
   to be evaluated, recursively, while updating parent selectors accordingly."
  [parent-selectors pending-hiccup hiccup-item]
  (if (at-rules/cssatrule? hiccup-item)
    (conj pending-hiccup (EvalExpr. parent-selectors hiccup-item))
    (let [flattened (reduce (fn [ret next-item]
                              (if (seq? next-item)
                                (into ret next-item)
                                (conj ret next-item)))
                            []
                            hiccup-item)
          {:keys [selectors attributes children]} (selectors-attributes-children flattened)
          new-selector-sequences (mapv #(conj parent-selectors %) selectors)
          pending-hiccup (if attributes
                           (into pending-hiccup (map #(EvalExpr. % attributes)) new-selector-sequences)
                           pending-hiccup)]
      (reduce (fn [pending-hiccup [new-selectors child]]
                (expand-hiccup-vector new-selectors pending-hiccup child))
              pending-hiccup
              (cartesian-product new-selector-sequences children)))))

(defn compile-selectors-and-params
  "For a current :paths & :params/:at-media map, translates the paths (selectors) which
  all have the equal params map or at-media record to CSS and str/joins them with a comma
  (a shorthand which can be used in CSS). Also translates the params map or at-media
  record to CSS and creates a CSS block from these compiled things, e.g.:
  (compile-selectors-and-params {:paths  #{[:.abc :#def :iframe] [:td :span sel/hover]}
                                 :attributes {:color   :font-black
                                          :margin  [[(units/px 15) (units/em 2)]]
                                          :display :flex}}
  => .abc #def iframe, td span:hover {
         color: #1A1B1F;
         margin: 15px 2em;
         display: flex;
      }"
  [{:keys [selectors expr]}]
  (cond (at-rules/at-media? expr) (binding [*at-media-indent* *indent*] (compile-at-rule selectors expr))
        (at-rules/cssatrule? expr) (compile-at-rule selectors expr)
        :else-attributes (let [compiled-selectors (compile-selectors-sequence selectors)
                               compiled-attributes (attr-map-to-css expr)]
                           (str *at-media-indent* compiled-selectors " {\n" *indent* compiled-attributes "\n" *at-media-indent* "}"))))

(defn compile-prepared-expressions
  "Given a vector of EvalExpr records, compiles them to CSS and joins them to one string."
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
  (let [expanded-list (flatten-seqs nested-hiccup-vectors-list)]
    (reduce (fn [current-unevaluated-hiccup hiccup-vector]
              (expand-hiccup-vector parents current-unevaluated-hiccup hiccup-vector))
            unevaluated-hiccup
            expanded-list)))

(defn just-css
  "Compiles the hiccup to a string of CSS. Does not do any printing or file output,
  that is what the functions below are. This one is separate to simplify both
  functions css and repl-css which just do something with the output of this function."
  [css-hiccup]
  (->> css-hiccup list
       (expand-hiccup-list-for-compilation [] [])
       compile-prepared-expressions))

(defn css
  "Generates CSS from a standard Tornado vector (or a list of hiccup vectors). If
  pretty-print? is set to false, compresses it as well. Then saves the compiled CSS
  to a given file path, if provided in the flags.

  You can also call this function only with the hiccup vector, without any flags."
  ([css-hiccup]
   (css nil css-hiccup))
  ([flags css-hiccup]
   (binding [*flags* (merge *flags* flags)]
     (let [{:keys [output-to pretty-print? indent-length]} *flags*]
       (binding [*compress?* (not pretty-print?)
                 *indent* (apply str (repeat indent-length " "))]
         (cond->> css-hiccup true just-css
                  *compress?* compression/compress
                  output-to ((fn [x] #?(:clj  (do (spit output-to x)
                                                  (println "   Wrote: " output-to))
                                        :cljs (util/exception "Cannot save compiled stylesheet in ClojureScript."))))))))))

(defn repl-css
  "Generates CSS from a standard Tornado hiccup vector (or a list of hiccup vectors)
  and pretty prints the output CSS string, which is useful for evaluating any tornado
  code in the REPL."
  [css-hiccup]
  (let [compiled-and-split-css-string (->> css-hiccup just-css
                                           str/split-lines)]
    (newline)
    (doseq [line compiled-and-split-css-string]
      (println line))
    (newline)))