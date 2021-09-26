(ns tornado.compiler
  "The Tornado compiler, where you should only care about these 3 functions:
  css, repl-css, compile-expression."
  (:require [tornado.types :as t]
            [tornado.at-rules :as at-rules]
            [tornado.util :as util]
            [clojure.string :as str]
            [tornado.selectors :as sel]
            [tornado.colors :as colors]
            [tornado.compression :as compression]
            #?(:clj [tornado.macros :as m]))
  #?(:clj  (:import (tornado.types CSSUnit CSSAtRule CSSFunction CSSColor
                                   CSSCombinator CSSAttributeSelector
                                   CSSPseudoClass CSSPseudoElement CSSPseudoClassFn)
                    (clojure.lang Keyword Symbol))
     :cljs (:require-macros [tornado.macros :refer [cartesian-product]])))

(def IUnit #?(:clj  CSSUnit
              :cljs t/CSSUnit))

(def IAtRule #?(:clj  CSSAtRule
                :cljs t/CSSAtRule))

(def IFunction #?(:clj  CSSFunction
                  :cljs t/CSSFunction))

(def IColor #?(:clj  CSSColor
               :cljs t/CSSColor))

(def ICombinator #?(:clj  CSSCombinator
                    :cljs t/CSSCombinator))

(def IAttribute #?(:clj  CSSAttributeSelector
                   :cljs t/CSSAttributeSelector))

(def IPseudoClass #?(:clj  CSSPseudoClass
                     :cljs t/CSSPseudoClass))

(def IPseudoElement #?(:clj  CSSPseudoElement
                       :cljs t/CSSPseudoElement))

(def IPseudoClassFn #?(:clj  CSSPseudoClassFn
                       :cljs t/CSSPseudoClassFn))

(def ^{:dynamic true
       :doc     "The current flags for a tornado build compilation:

             :indent-length - Specifies, how many indentation spaces should be in the compiled
                              CSS file after any nesting in @rule or params map. Defaults to 4.

             :pretty-print? - Specifies, whether the compiled CSS should be pretty printed.
                              Defaults to true. If set to false, the CSS file will be compressed
                              after compilation (removal of unnecessary characters like spaces
                              and newlines) to make the CSS file a bit smaller.

             :output-to     - Specifies, where the compiled CSS file should be saved.

             _show-time     - When set to true, elapsed time will be printed."}
  *flags* {:indent-length 4
           :pretty-print? true
           :output-to     nil})

(defn indent
  "The actual globally used indent in a string form of *X* spaces."
  []
  (apply str (repeat (:indent-length *flags*) " ")))

(def ^:dynamic *media-query-parents*
  "Current parents Used for compiling @media to temporarily store parents
  paths for compiling @media changes."
  nil)

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
         simplify-prepared-expanded-hiccup
         compile-all-selectors-params-combinations)

(defn conjs
  "Conj(oin)s to a (potentially empty) set."
  [s value]
  (conj (or s #{}) value))

(defmulti compile-selector
          "Compiles a CSS combinator, attribute selector, pseudoclass or pseudoelement
          or a selector in a keyword/symbol/string form."
          #?(:clj  class
             :cljs (fn [sel]
                       (cond (keyword? sel) Keyword
                             (symbol? sel) Symbol
                             (string? sel) (type "")
                             (instance? IAttribute sel) IAttribute
                             (instance? IPseudoClass sel) IPseudoClass
                             (instance? IPseudoElement sel) IPseudoElement
                             (instance? IPseudoClassFn sel) IPseudoClassFn
                             (instance? ICombinator sel) ICombinator))))

(defmethod compile-selector Keyword
  [selector]
  (name selector))

(defmethod compile-selector Symbol
  [selector]
  (name selector))

(defmethod compile-selector #?(:clj  String
                               :cljs (type ""))
  [selector]
  (name selector))

(defmethod compile-selector IAttribute
  [{:keys [compiles-to tag attribute subvalue]}]
  (let [maybe-subvalue (when subvalue (str "\"" (name subvalue) "\""))]
    (str (util/get-str-form tag) "[" (util/get-str-form attribute) compiles-to maybe-subvalue "]")))

(defmethod compile-selector IPseudoClass
  [{:keys [pseudoclass]}]
  (str ":" pseudoclass))

(defmethod compile-selector IPseudoElement
  [{:keys [pseudoelement]}]
  (str "::" pseudoelement))

(defmethod compile-selector IPseudoClassFn
  [{:keys [compiles-to arg]}]
  (str ":" compiles-to "(" (compile-expression arg) ")"))

(defmethod compile-selector ICombinator
  [{:keys [compiles-to children]}]
  (->> children (map #(str compiles-to " " (name %)))
       util/str-spacejoin))

(defn compile-selectors-sequence
  "Given a selectors path, which can contain special selectors, this function
  generates a CSS string from the selectors."
  [selectors-path]
  (as-> selectors-path <> (reduce (fn [selectors next-selector]
                                    (assert (or (sel/selector? next-selector)
                                                (sel/id-class-tag? next-selector))
                                            (str "Expected a selector while compiling: " next-selector))
                                    (let [selectors (if (util/some-instance? next-selector IPseudoClass
                                                                             IPseudoClassFn IPseudoElement)
                                                      selectors
                                                      (util/conjv selectors " "))]
                                      (util/conjv selectors (compile-selector next-selector))))
                                  [] <>)
        (apply str <>)
        (str/trim <>)))

(defn compile-selectors
  "Given a sequence of selectors paths, e.g. '([:iframe :.abc] [:#def sel/after :.ghi]),
  this function translates all the selectors paths to CSS and str/joins them with a comma,
  which is a shorthand that can be used in CSS to give different selectors paths the same
  parameters. ... => \"iframe .abc, #def::after .ghi\""
  [selectors-sequences]
  (let [compiled-selectors (->> selectors-sequences (map compile-selectors-sequence)
                                util/str-commajoin)]
    (str *at-media-indent* compiled-selectors)))

(defmulti compile-color
          "Generates CSS from a color, calls a relevant method to do so depending on the
          color's type:
          \"rgb\", \"rgba\", \"hsl\", \"hsla\", keyword, string, keyword (for keywords,
          tries to get an exact hex-value of the color from colors/default-colors),
          otherwise prints out a warning and returns a string form of that keyword."
          colors/get-color-type)

(defn round [x] (-> x float Math/round))

(defmethod compile-color "rgb"
  [{:keys [value]}]
  (let [{:keys [red green blue]} value
        [red green blue] (map round [red green blue])]
    (str "rgb(" red ", " green ", " blue ")")))

(defmethod compile-color "rgba"
  [{:keys [value]}]
  (let [{:keys [red green blue alpha]} value
        alpha (util/percent->number alpha)
        [red green blue alpha] (map round [red green blue alpha])]
    (str "rgba(" red ", " green ", " blue ", " alpha ")")))

(defmethod compile-color "hsl"
  [{:keys [value]}]
  (let [{:keys [hue saturation lightness]} value
        saturation (util/percent-with-symbol-append saturation)
        lightness (util/percent-with-symbol-append lightness)
        hue (round hue)]
    (str "hsl(" hue ", " saturation ", " lightness ")")))

(defmethod compile-color "hsla"
  [{:keys [value]}]
  (let [{:keys [hue saturation lightness alpha]} value
        saturation (util/percent-with-symbol-append saturation)
        lightness (util/percent-with-symbol-append lightness)
        alpha (util/percent->number alpha)
        hue (round hue)]
    (str "hsla(" hue ", " saturation ", " lightness ", " alpha ")")))

(defmulti compile-css-record
          "Compiles a CSS record (unit, function, at-rule, color). For 4 different types of
          CSS selectors, there is a different multifunction \"compile-selector\"."
          #?(:clj  class
             :cljs (fn [rec]
                       (cond (instance? IUnit rec) IUnit
                             (instance? IFunction rec) IFunction
                             (instance? IAtRule rec) IAtRule
                             (instance? IColor rec) IColor))))

(defmethod compile-css-record :default
  [record]
  (util/exception (str "Not a valid tornado record: " record " with a class: " #?(:clj  (class record)
                                                                                  :cljs (type record)))))

(defmethod compile-css-record IUnit
  [{:keys [value compiles-to]}]
  (str (util/int* value) compiles-to))

(defn commajoin
  "Redefining functions/commajoin because there would be a cyclic dependency otherwise."
  [{:keys [compiles-to args]}]
  (str compiles-to "(" (->> args (map compile-expression)
                            util/str-commajoin) ")"))

(defmethod compile-css-record IFunction
  [{:keys [compile-fn] :or {compile-fn commajoin} :as CSSFn-record}]
  (compile-fn CSSFn-record))

(defmethod compile-css-record IAtRule
  [at-rule-record]
  (compile-at-rule at-rule-record))

(defmethod compile-css-record IColor
  [color-record]
  (compile-color color-record))

(def calc-keywords
  "A special map for calc keywords"
  {:add "+"
   :sub "-"
   :mul "*"
   :div "/"})

(defn compile-expression
  "Compiles an expression: a number, string, symbol or a record. If the expression is
  a vector of sequential structures, compiles each of the structures and str/joins them
  with a space. Then, str/joins all these str/spacejoined structures with a comma.

  E.g.:
  (compile-expression [[(u/px 15) (u/percent 20)] [:red :chocolate]])
  => \"15px 20%, #FF0000 #D2691E\""
  [expr]
  (cond (and (util/valid? expr)
             (get colors/default-colors (colors/color->1-wd expr))) (get colors/default-colors (colors/color->1-wd expr))
        (get calc-keywords expr) (get calc-keywords expr)
        (util/valid? expr) (name expr)
        (number? expr) (util/int* expr)
        (record? expr) (compile-css-record expr)
        (and (sequential? expr)
             (every? sequential? expr)) (->> expr (map #(->> % (map compile-expression)
                                                             util/str-spacejoin))
                                             util/str-commajoin)
        :else (util/exception
                (str "None of a CSS unit, CSS function, CSS at-rule, a keyword a string, a number or"
                     " a sequential structure consisting of more sequential structures:\n" expr))))

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
                    :background-color (colors/rotate-hue \"#ff0000\" 60)}
  => width: 50%;
     margin: 0 15px rem3 auto;
     background-color: hsl(60 1 0.5);"
  [attributes-map]
  (when attributes-map
    (->> attributes-map compile-attributes-map
         (map util/str-colonjoin)
         (map #(str *keyframes-indent* *at-media-indent* % ";"))
         (str/join (str "\n" (indent) *at-media-indent*))
         (str *keyframes-indent*))))

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
          :identifier)

(defmethod compile-at-rule :default
  [{:keys [identifier] :as at-rule}]
  (util/exception (str "Unknown at-rule identifier: " identifier " of at-rule: " at-rule)))

(def special-media-rules-map
  "A special map for generating media queries rules, e.g.:
  (tornado.at-rules/at-media {:rules {:screen  :only   -> \"only screen\"
                                      :screen  false   -> \"not screen\"
                                      :screen  true    -> \"screen\"} ... "
  {:only #(str "only " (name %))
   true  name
   false #(str "not " (name %))})

(defmethod compile-at-rule "media"
  [{:keys [value] :as at-media}]
  (let [paths *media-query-parents*
        {:keys [rules changes]} value
        compiled-media-rules (->> (for [[param value] rules]
                                    (if-let [param-fn (get special-media-rules-map value)]
                                      (param-fn param)
                                      (if-let [compiled-param (util/get-str-form param)]
                                        (let [compiled-unit (compile-expression value)]
                                          (str "(" compiled-param ": " compiled-unit ")"))
                                        (util/exception
                                          (str "Invalid format of a CSS property: " value " in a map of rules:"
                                               rules " in at-media compilation of @media expression: " at-media)))))
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
                             (str/join (str "\n" (indent))))]
    (str "@font-face {\n" (indent) compiled-params "\n}")))

(defmethod compile-at-rule "keyframes"
  [{:keys [value]}]
  (let [{:keys [anim-name frames]} value]
    (if *in-params-context*
      anim-name
      (binding [*keyframes-indent* (indent)]
        (let [compiled-frames (->> frames (map (fn [[progress params]]
                                                 (let [compiled-progress (compile-expression progress)
                                                       compiled-params (attr-map-to-css params)]
                                                   (str compiled-progress " {\n"
                                                        compiled-params "\n" *keyframes-indent* "}"))))
                                   (str/join (str "\n" *keyframes-indent*))
                                   (str *keyframes-indent*))]
          (str "@keyframes " anim-name " {\n" compiled-frames "\n}"))))))

(defn expand-seqs
  "Expands lists and lazy sequences in a nested structure. Always expands the first
   collection. When any more deeply nested collection is neither a list nor a lazy-seq,
   this function does not expand it.
   See clojure.core/flatten."
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
                                                   :else (util/exception
                                                           (str "Invalid hiccup element: " hiccup-element "\nin"
                                                                " hiccup: " hiccup "\nNone from a class, id,"
                                                                " selector, child-vector, at-media CSSAtRule"
                                                                " instance or a params map.")))]
                              (if (or (and (not= belongs-to :selectors)
                                           (empty? selectors))
                                      (and (= belongs-to :selectors)
                                           (or (seq params) (seq children) (seq at-media)))
                                      (and (= belongs-to :params)
                                           (or (seq params) (seq children) (seq at-media))))
                                (util/exception
                                  (str "Error: Hiccup rules:\nYou have to include at least one selector before"
                                       " params or children.\nIf you include any of params or children, the order"
                                       " has to be selectors -> params -> children.\nYou also cannot include more"
                                       " than one parameters map. At-font-face can be included anywhere in the"
                                       " hiccup vector.\nHiccup received: " hiccup))
                                (update spc-map belongs-to conj hiccup-element))))
                          {:selectors []
                           :params    []
                           :children  []
                           :at-media  []} <>)
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
       (reduce (fn [params->paths-map {:keys [path params at-media at-font-face at-keyframes]}]
                 (let [known-at-media (get params->paths-map at-media)]
                   (cond at-keyframes (update params->paths-map :keyframes-set conj at-keyframes)
                         at-font-face (update params->paths-map :font-faces-set conjs at-font-face)
                         (and at-media known-at-media) (update params->paths-map at-media conjs path)
                         (and at-media (not known-at-media)) (assoc params->paths-map at-media #{path})
                         (get params->paths-map params) (update params->paths-map params conjs path)
                         :else (assoc params->paths-map params #{path}))))
               {})
       (reduce (fn [final-expanded-hiccup [params selectors-set]]
                 (cond (at-rules/at-media? params) (conj final-expanded-hiccup {:paths    (vec selectors-set)
                                                                                :at-media params})
                       (= :keyframes-set params) (reduce #(conj %1 {:at-keyframes %2}) final-expanded-hiccup selectors-set)
                       (= :font-faces-set params) (reduce #(conj %1 {:at-font-face %2}) final-expanded-hiccup selectors-set)
                       :else (conj final-expanded-hiccup {:paths  (vec selectors-set)
                                                          :params params})))
               [])))

(defn expand-hiccup-vector
  "Given a (potentially nil) current parents sequence, unevaluated hiccup combinations
  in a vector the current hiccup vector, which is in a form
  [sel1 maybe-sel2 maybe-sel3 ... {maybe-params-map} [maybe-child1] [maybe-child2] ... ]
  where each child is a hiccup vector as well, this function adds all combinations
  of selectors and descendant children and their selectors together with corresponding
  parameters (or skips the combination of params are nil) to the unevaluated-hiccup
  argument, recursively."
  [parents unevaluated-hiccup hiccup-vector]
  (cond (at-rules/at-font-face? hiccup-vector) (conj unevaluated-hiccup {:at-font-face hiccup-vector})
        (at-rules/at-keyframes? hiccup-vector) (conj unevaluated-hiccup {:at-keyframes hiccup-vector})
        :else (let [{:keys [selectors params children at-media]} (selectors-params-children hiccup-vector)
                    maybe-at-media (when (seq at-media)
                                     (as-> selectors <> (map (partial util/conjv parents) <>)
                                           (#?(:clj  m/cartesian-product
                                               :cljs cartesian-product) <> at-media)
                                           (map (fn [[path media-rules]]
                                                  {:path     path
                                                   :at-media media-rules}) <>)))
                    unevaluated-hiccup (if maybe-at-media
                                         (reduce conj unevaluated-hiccup maybe-at-media)
                                         unevaluated-hiccup)]
                (if (seq children)
                  (reduce (fn [current-unevaluated-hiccup [selector child]]
                            (let [new-parents (util/conjv parents selector)
                                  updated-hiccup (update-unevaluated-hiccup current-unevaluated-hiccup new-parents params)]
                              (expand-hiccup-list-for-compilation new-parents updated-hiccup (list child))))
                          unevaluated-hiccup
                          (#?(:clj  m/cartesian-product
                              :cljs cartesian-product) selectors children))
                  (reduce (fn [current-unevaluated-hiccup selector]
                            (let [new-parents (util/conjv parents selector)]
                              (update-unevaluated-hiccup current-unevaluated-hiccup new-parents params)))
                          unevaluated-hiccup
                          selectors)))))

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
  [{:keys [paths params at-media at-font-face at-keyframes]}]
  (cond at-media (binding [*media-query-parents* paths
                           *at-media-indent* (indent)]
                   (compile-at-rule at-media))
        at-font-face (compile-css-record at-font-face)
        at-keyframes (compile-css-record at-keyframes)
        :else (when params (let [compiled-selectors (compile-selectors paths)
                                 compiled-params (attr-map-to-css params)]
                             (str compiled-selectors " {\n" (indent) compiled-params "\n" *at-media-indent* "}")))))

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

(defn just-css
  "Compiles the hiccup to a string of CSS. Does not do any printing or file output,
  that is what the functions below are. This one is separate to simplify both
  functions css and repl-css which just do something with the output of this function."
  [css-hiccup]
  (->> css-hiccup list
       (expand-hiccup-list-for-compilation nil [])
       simplify-prepared-expanded-hiccup
       compile-all-selectors-params-combinations))

(defn css
  "Generates CSS from a hiccup vector or a (maybe multiple times) nested list of hiccup
  vectors (and/or with @keyframes, @font-face). If pretty-print? is set to false,
  compresses it as well. Then saves the compiled CSS string to a given file path.

  You can also call this function without flags whenever you want to to just get
  the whole compiled CSS string printed out."
  ([css-hiccup]
   (css nil css-hiccup))
  ([flags css-hiccup]
   (binding [*flags* (merge *flags* flags)]
     (let [{:keys [pretty-print? output-to]} *flags*]
       (cond->> css-hiccup true just-css
                (not pretty-print?) compression/compress
                output-to ((fn [x] #?(:clj  (do (spit output-to x)
                                                (println "   Wrote: " output-to))
                                      :cljs (util/exception "Cannot save compiled stylesheet in ClojureScript.")))))))))

(defn repl-css
  "Generates CSS from a hiccup vector or a (maybe multiple times) nested list of hiccup
  vectors (and/or with @keyframes, @font-face). It then pretty prints the output string,
  which is useful for evaluating any tornado code in REPL."
  [css-hiccup]
  (let [compiled-and-split-css-string (->> css-hiccup just-css
                                           str/split-lines)]
    (newline)
    (doseq [line compiled-and-split-css-string]
      (println line))
    (newline)))