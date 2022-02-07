(ns tornado.core
  "The core tornado namespace, where every single function or value you could
  need is defined and documented in this namespace."
  (:require [tornado.units :as u]
            [tornado.colors :as colors]
            [tornado.compiler :as compiler]
            [tornado.selectors :as sel]
            [tornado.functions :as f]
            [tornado.at-rules :as at-rules]
            [tornado.common :as common]
            #?(:clj [tornado.macros :as macros])))

;; COMPILER FUNCTIONS

(defn compile-expression
  "Compiles an expression: a number, string, symbol or a record. If the expression is
  a vector of sequential structures, compiles each of the structures and str/joins them
  with a space. Then, str/joins all these str/spacejoined structures with a comma.

  E.g.:
  (compile-expression [[(px 15) (percent 20)] [:red :chocolate]])
  => \"15px 20%, #FF0000 #D2691E\""
  [expr] (compiler/compile-expression expr))

(defn css
  "Generates CSS from a standard Tornado vector (or a list of hiccup vectors). If
  pretty-print? is set to false, compresses it as well. Then saves the compiled CSS
  to a given file path, if provided in the flags.

  You can also call this function only with the hiccup vector, without any flags."
  ([css-hiccup] (compiler/css css-hiccup))
  ([flags css-hiccup] (compiler/css flags css-hiccup)))

(defn repl-css
  "Generates CSS from a standard Tornado hiccup vector (or a list of hiccup vectors)
                  and pretty prints the output CSS string, which is useful for evaluating any tornado
                  code in the REPL."
  [css-hiccup] (compiler/repl-css css-hiccup))

(defn html-style
  "Can be used for compilation of a map of style parameters to a single string of html
  style=\"...\" attribute. Receives the styles map as its argument and returns a string
  of compiled style:

  (html-style {:width            (px 500)
               :height           (percent 15)
               :color            :font-black
               :background-color :teal})

  => \"width:500px;height:15%;color:#1A1B1F;background-color:#008080\""
  [styles-map] (compiler/html-style styles-map))

(defn compile-params
  "Given a map of HTML style attributes described in Tornado, compiles all the values
                  of the parameters, but the parameters names remain the same. This function is useful
                  for Reagent to allow you describing the style with Tornado.
                  Example usage:

                  {:style (compile-params {:width            (px 500)
                                           :background-color (important (rgb 100 150 200))
                                           :border           [[(px 1) :solid :black]]
                                           :display          :flex})}

                  => {:style {:width            \"500px\",
                              :background-color \"rgb(100, 150, 200) !important\",
                              :border           \"1px solid #000000\",
                              :display          \"flex\"}"
  [attributes-map] (compiler/compile-params attributes-map))

; - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
;; UNITS

#?(:clj
   (defmacro defunit
     "Creates a unit function which takes 1 argument and creates a CSSUnit record for future
     compilation. Defunit can take 1 arg: (defunit px)
                               or 2 args: (defunit percent \"%\").

     Usage of the defined units: (px 15)       ...  compiles to \"15px\"
                                 (percent 33)  ...  compiles to \"33%\"

     CSSUnits can be added, subtracted, multiplied or divided by using function calc (you can
     also use these 4 keywords - they are defined just for better search in code:
     :add, :sub, :mul, :div

     E.g. (calc (px 500) :add 3 :mul (vw 5)) ... \"calc(500px + 3 * 5vw)\"."
     ([unit] `(macros/defunit ~unit))
     ([identifier css-unit] `(macros/defunit ~identifier ~css-unit))))

;; absolute size units

(defn px "An absolute length unit, \"pixel\"." [value] (u/px value))
(defn pt "An absolute length unit, \"point\"." [value] (u/pt value))
(defn pc "An absolute length unit, \"pica\"." [value] (u/pc value))
(defn in "An absolute length unit, \"inch\"" [value] (u/in value))
(defn cm "An absolute length unit, \"centimeter\"." [value] (u/cm value))
(defn mm "An absolute length unit, \"millimeter\"." [value] (u/mm value))

;; relative size units

(defn percent
  "An absolute length unit, \"percent\" (\"%\"), can also be used as color alpha
  or in defining keyframes in this library." [value] (u/percent value))
(defn css-rem "A relative length unit, \"rem\", depending on the size of the root element" [value] (u/css-rem value))
(defn em "A relative length unit, \"em\", depending on the size of the parent element." [value] (u/em value))

(defn fr "A relative length unit, \"fraction\", depending on the size of the parent element." [value] (u/fr value))
(defn vw "A relative length unit, \"viewport width\", based on the width of the window." [value] (u/vw value))
(defn vh "A relative length unit, \"viewport height\", based on the height of the window." [value] (u/vh value))
(defn vmin "A relative length unit, minimum of vw and vh." [value] (u/vmin value))
(defn vmax "A relative length unit, maximum of vw and vh." [value] (u/vmax value))
(defn lh "A relative length unit, equal to the line height." [value] (u/lh value))

;; time units

(defn s "A time unit, \"second\"." [value] (u/s value))
(defn ms "A time unit, \"millisecond\"." [value] (u/ms value))

;; angular units

(defn deg "An angular unit, \"degree\"." [value] (u/deg value))
(defn rad "An angular unit, \"radian\". Equal to 360°/2π" [value] (u/rad value))
(defn grad "An angular unit, \"gradian\". 100 gradians are equal to 90 degrees." [value] (u/grad value))
(defn turn "An angular unit, \"turn\". Represents one whole turn, equal to 360 degrees." [value] (u/turn value))

;; frequency units

(defn Hz "A frequency unit, \"Hertz." [value] (u/Hz value))
(defn kHz "A frequency unit, \"kiloHertz." [value] (u/kHz value))

;; resolution units
(defn dpi "A resolution unit, \"dots per inches\"." [value] (u/dpi value))
(defn dppx "A resolution unit, \"dots per pixels\"." [value] (u/dppx value))
(defn dpcm "A resolution unit, \"dots per centimeter\"." [value] (u/dpcm value))

; - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
;; FUNCTIONS

(defn single-arg "Coming soon" ([arg] (f/single-arg arg)) ([arg & more] (apply f/single-arg arg more)))
(alter-meta! #'single-arg assoc :arglists '([arg]))
(defn comma-join "Coming soon" [& args] (apply f/comma-join args))
(defn space-join "Coming soon" [& args] (apply f/space-join args))

#?(:clj
   (defmacro defcssfn
     "Creates a CSS function which takes any number of arguments and
     creates
     a CSSFunction record for future compilation.

     Defcssfn can take 1 argument, which creates the function with the same name in CSS
     and it will be expanded with str/join \", \" (default function - comma-join):
     (defcssfn some-fn) => my.namespace/some-fn
     (some-fn \"arg1\" 42 (px 15)) ... compiles to   \"some-fn(arg1, 42, 15px)\"

     or it can take 2 arguments:
     (defcssfn css-min \"min\") => my.namespace/css-min
     (css-min (px 500) (vw 60)) ... compiles to   \"min(500px, 60vw)\"

     or the 2nd argument can be a compiling function (most commonly space-join, comma-join
     or single-arg for separating the args, you can also define a special function for
     that, but it should not ever be needed; read docs to these 3 functions):
     (defcssfn calc space-join)
     (calc (px 500) add 3 mul (vw 5)) ... \"calc(500px + 3 * 5vw)\"

     you can also give defcssfn 3 arguments, where the 2nd one will be a special string
     for translation to CSS and the 3rd one the compiling function."
     ([fn-name] `(macros/defcssfn ~fn-name))
     ([fn-name css-fn-or-fn-tail] `(macros/defcssfn ~fn-name ~css-fn-or-fn-tail))
     ([clojure-fn-name compiles-to compile-fn] `(macros/defcssfn ~clojure-fn-name ~compiles-to ~compile-fn))))

;; single arg functions

(defn blur "Coming soon" ([arg] (f/blur arg)) ([arg & more] (apply f/blur arg more)))
(defn brightness "Coming soon" ([arg] (f/brightness arg)) ([arg & more] (apply f/brightness arg more)))
(defn contrast "Coming soon" ([arg] (f/contrast arg)) ([arg & more] (apply f/contrast arg more)))
(defn css-format "A special function for @font-face." ([arg] (f/css-format arg)) ([arg & more] (apply f/css-format arg more)))
(defn grayscale "Coming soon" ([arg] (f/grayscale arg)) ([arg & more] (apply f/grayscale arg more)))
(defn hue-rotate "Coming soon" ([arg] (f/hue-rotate arg)) ([arg & more] (apply f/hue-rotate arg more)))
(defn invert "Coming soon" ([arg] (f/invert arg)) ([arg & more] (apply f/invert arg more)))
(defn perspective "Coming soon" ([arg] (f/perspective arg)) ([arg & more] (apply f/perspective arg more)))
(defn rotate "Coming soon" ([arg] (f/rotate arg)) ([arg & more] (apply f/rotate arg more)))
(defn rotateX "Coming soon" ([arg] (f/rotateX arg)) ([arg & more] (apply f/rotateX arg more)))
(defn rotateY "Coming soon" ([arg] (f/rotateY arg)) ([arg & more] (apply f/rotateY arg more)))
(defn rotateZ "Coming soon" ([arg] (f/rotateZ arg)) ([arg & more] (apply f/rotateZ arg more)))
(defn sepia "Coming soon" ([arg] (f/sepia arg)) ([arg & more] (apply f/sepia arg more)))
(defn skewX "Coming soon" ([arg] (f/skewX arg)) ([arg & more] (apply f/skewX arg more)))
(defn skewY "Coming soon" ([arg] (f/skewY arg)) ([arg & more] (apply f/skewY arg more)))
(defn scaleX "Coming soon" ([arg] (f/scaleX arg)) ([arg & more] (apply f/scaleX arg more)))
(defn scaleY "Coming soon" ([arg] (f/scaleY arg)) ([arg & more] (apply f/scaleY arg more)))
(defn scaleZ "Coming soon" ([arg] (f/scaleZ arg)) ([arg & more] (apply f/scaleZ arg more)))
(defn translateX "Coming soon" ([arg] (f/translateX arg)) ([arg & more] (apply f/translateX arg more)))
(defn translateY "Coming soon" ([arg] (f/translateY arg)) ([arg & more] (apply f/translateY arg more)))
(defn translateZ "Coming soon" ([arg] (f/translateZ arg)) ([arg & more] (apply f/translateZ arg more)))
(do #?@(:clj [(defmacro alter-meta-arglists! [& fs]
                `(do ~@(map (fn [f] (list `alter-meta! (list 'var f) `assoc :arglists (list 'quote '([arg])))) fs)))
              (defmacro do-meta-alters! []
                `(alter-meta-arglists! blur brightness contrast css-format grayscale hue-rotate invert perspective rotate rotateX
                                       rotateY rotateZ sepia skewX skewY scaleX scaleY scaleZ translateX translateY translateZ))
              (do-meta-alters!)]))
;; comma-join functions

(defn attr "Coming soon" [& args] (apply f/attr args))
(defn counter "Coming soon" [& args] (apply f/counter args))
(defn counters "Coming soon" [& args] (apply f/counters args))
(defn cubic-bezier "Coming soon" [& args] (apply f/cubic-bezier args))
(defn css-filter "Coming soon" [& args] (apply f/css-filter args))
(defn hwb "Coming soon" [& args] (apply f/hwb args))
(defn linear-gradient "Coming soon" [& args] (apply f/linear-gradient args))
(defn matrix "Coming soon" [& args] (apply f/matrix args))
(defn matrix3d "Coming soon" [& args] (apply f/matrix3d args))
(defn css-max "Coming soon" [& args] (apply f/css-max args))
(defn css-min "Coming soon" [& args] (apply f/css-min args))
(defn polygon "Coming soon" [& args] (apply f/polygon args))
(defn radial-gradient "Coming soon" [& args] (apply f/radial-gradient args))
(defn repeating-linear-gradient "Coming soon" [& args] (apply f/repeating-linear-gradient args))
(defn repeating-radial-gradient "Coming soon" [& args] (apply f/repeating-radial-gradient args))
(defn rotate3d "Coming soon" [& args] (apply f/rotate3d args))
(defn scale "Coming soon" [& args] (apply f/scale args))
(defn scale3d "Coming soon" [& args] (apply f/scale3d args))
(defn skew "Coming soon" [& args] (apply f/skew args))
(defn translate "Coming soon" [& args] (apply f/translate args))
(defn translate3d "Coming soon" [& args] (apply f/translate3d args))
(defn url "Coming soon" [& args] (apply f/url args))
(defn css-var "Coming soon" [& args] (apply f/css-var args))

;; space-join functions

(defn calc "Coming soon" [& args] (apply f/calc args))
(defn circle "Coming soon" [& args] (apply f/circle args))
(defn drop-shadow "Coming soon" [& args] (apply f/drop-shadow args))
(defn ellipse "Coming soon" [& args] (apply f/ellipse args))
(defn image "Coming soon" [& args] (apply f/image args))
(defn inset "Coming soon" [& args] (apply f/inset args))
(defn symbols "Coming soon" [& args] (apply f/symbols args))

;; - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
;; SELECTORS

;; attribute selectors

#?(:clj
   (defmacro defattributeselector
     "Defines a CSS attribute selector. Those select all descendant elements containing
     a given attribute, of which the value matches a given substring. All attribute
     selectors have different conditions for matching:
     Start with a word, start with a substring, contain a word, contain a substring,
     end with a substring, have a given value, have a given attribute with any value.

     By attributes, it is meant html attributes,
     e.g. span[class~=\"info\"]
     selects all spans with a class containing a whole word \"info\".
     In tornado, we can represent this by (contains-word :span :class \"info\").

     We can also use (contains-word :class \"info\") to mark all elements with that
     class ... compiles to [class~=\"info\"] and affects all elements with that
     class (divs, spans, iframes, everything)."
     [selector-name compiles-to]
     `(macros/defattributeselector ~selector-name ~compiles-to)))

(defn has-attr
  "An attribute selector which selects all elements which have a given
  attribute with any value, or all html elements on/below the current
  nested selectors level which have a given attribute with any value."
  ([attribute] (sel/has-attr attribute))
  ([tag attribute] (sel/has-attr tag attribute)))

(defn has-val
  "Selects all descendants of a html tag which have a given parameter with a given value."
  ([attribute subvalue] (sel/has-val attribute subvalue))
  ([tag attribute subvalue] (sel/has-val tag attribute subvalue)))

(defn contains-word
  "Selects all descendant elements which have a given parameter with a value containing
  a given word (substring is not enough - a matching word separated by commas or spaces)."
  ([attribute subvalue] (sel/contains-word attribute subvalue))
  ([tag attribute subvalue] (sel/contains-word tag attribute subvalue)))

(defn contains-subs
  "Selects all descendant elements which have a given parameter with a value containing
  a given substring (unlike the contains-word selector, the substring does not have to
  be a whole word)."
  ([attribute subvalue] (sel/contains-subs attribute subvalue))
  ([tag attribute subvalue] (sel/contains-subs tag attribute subvalue)))

(defn starts-with-word
  "Selects all descendant elements which have a given parameter with a value starting with
  a given word (substring is not enough - a matching word separated by commas or spaces)."
  ([attribute subvalue] (sel/starts-with-word attribute subvalue))
  ([tag attribute subvalue] (sel/starts-with-word tag attribute subvalue)))

(defn starts-with
  "Selects all descendant elements which have a given parameter with a value starting with
  a given substring (unlike the contains-word selector, the substring does not have to be
  a whole matching word."
  ([attribute subvalue] (sel/starts-with attribute subvalue))
  ([tag attribute subvalue] (sel/starts-with tag attribute subvalue)))

(defn ends-with
  "Selects all descendant elements which have a given parameter with a value ending
  with a given substring. The substring does not have to be a whole matching word."
  ([attribute subvalue] (sel/ends-with attribute subvalue))
  ([tag attribute subvalue] (sel/ends-with tag attribute subvalue)))

;; pseudoclass selectors

#?(:clj
   (defmacro defpseudoclass
     "Defines a CSS pseudoclass. A CSS pseudoclass can activate some CSS properties on
     a css-class/css-id/html-element based on some current special state of the element.

     For example, hover: (defpseudoclass hover)
     When compiling a selectors sequence, e.g. [:.abc :#def hover], the resulting CSS
     selectors sequence will look like this: \".abc #def:hover\".

     So, what does it even do? We can give the element a special value on hover:
     ... [:a hover {:color :blue} ...] - when we hover over a link with our mouse, the
     text color of the link will turn blue until we put our mouse away.

     Defpseudoclass can also take 2 parameters, where the 2nd one will be the translation
     to CSS to avoid collisions between Clojure and CSS -
     e.g.(defpseudolass css-empty \"empty\")."
     ([pseudoclass] `(macros/defpseudoclass ~pseudoclass))
     ([identifier css-pseudoclass] `(macros/defpseudoclass ~identifier ~css-pseudoclass))))

(def ^{:doc "CSS pseudoselector \"active\". Used as a value, e.g.:
             [:.some-sel {:width (px 100)}
              [active {:width (px 120)}]]"}
  active sel/active)

(def ^{:doc "CSS pseudoselector \"checked\". Used as a value, e.g.:
             [:.some-sel {:border [[(px 1) :solid :red]]}
              [checked {:border [[(px 1) :solid :crimson]]}]]"}
  checked sel/checked)

(def ^{:doc "CSS pseudoselector \"default\". Used as a value, e.g.:
             [:.some-sel {:width (px 100)}
              [default {:width (px 120)}]]"}
  default sel/default)

(def ^{:doc "CSS pseudoselector \"disabled\". Used as a value, e.g.:
             [:.some-sel {:background-color :white}
              [disabled {:background-color :gray}]]"}
  disabled sel/disabled)

(def ^{:doc "CSS pseudoselector \"css-empty\". Used as a value, e.g.:
             [:.some-sel {:padding (px 20)}
              [css-empty {:padding (px 5)}]]"}
  css-empty sel/css-empty)

(def ^{:doc "CSS pseudoselector \"enabled\". Used as a value, e.g.:
             [:.some-sel {:background-color :gray}
              [enabled {:background-color :white}]]"}
  enabled sel/enabled)

(def ^{:doc "CSS pseudoselector \"css-first\". Used as a value, e.g.:
             [:.some-sel
              [css-first {:transform (scale 1.1)}]]"}
  css-first sel/css-first)

(def ^{:doc "CSS pseudoselector \"first-child\". Used as a value, e.g.:
             [:.some-sel
              [first-child {:transform (scale 1.1)}]]"}
  first-child sel/first-child)

(def ^{:doc "CSS pseudoselector \"first-of-type\". Used as a value, e.g.:
             [:.some-sel
              [first-of-type {:transform (scale 1.1)}]]"}
  first-of-type sel/first-of-type)

(def ^{:doc "CSS pseudoselector \"fullscreen\". Used as a value, e.g.:
             [:.some-sel
              [fullscreen {:background-color :gray}]]"}
  fullscreen sel/fullscreen)

(def ^{:doc "CSS pseudoselector \"focus\". Used as a value, e.g.:
             [:.some-sel
              [focus {:background-color :blue}]]"}
  focus sel/focus)

(def ^{:doc "CSS pseudoselector \"hover\". Used as a value, e.g.:
             [:.some-sel {:width (px 100)}
              [hover {:width (px 120)}]]"}
  hover sel/hover)

(def ^{:doc "CSS pseudoselector \"indeterminate\". Used as a value, e.g.:
             [:.some-sel {:width (px 100)}
              [indeterminate {:width (px 120)}]]"}
  indeterminate sel/indeterminate)

(def ^{:doc "CSS pseudoselector \"in-range\". Used as a value, e.g.:
             [:.some-sel
              [in-range {:border [[(px 2) :solid :red]]}]]"}
  in-range sel/in-range)

(def ^{:doc "CSS pseudoselector \"invalid\". Used as a value, e.g.:
             [:.some-sel {:width (px 100)}
              [invalid
               [:.err-msg {:display :flex}]]]"}
  invalid sel/invalid)

(def ^{:doc "CSS pseudoselector \"last-child\". Used as a value, e.g.:
             [:.some-sel
              [last-child {:padding-right 0}]]"}
  last-child sel/last-child)

(def ^{:doc "CSS pseudoselector \"last-of-type\". Used as a value, e.g.:
             [:.some-sel
              [last-of-type {:margin-right (px 5p}]]"}
  last-of-type sel/last-of-type)

(def ^{:doc "CSS pseudoselector \"links\". Used as a value, e.g.:
             [:.some-sel
              [hover {:color :font-black}]]"}
  links sel/links)

(def ^{:doc "CSS pseudoselector \"only-child\". Used as a value, e.g.:
             [:.some-sel {:padding (px 10)}
              [only-child {:padding (px 15)}]]"}
  only-child sel/only-child)

(def ^{:doc "CSS pseudoselector \"only-of-type\". Used as a value, e.g.:
             [:.some-sel
              [only-of-type {:background-color :chocolate}]]"}
  only-of-type sel/only-of-type)

(def ^{:doc "CSS pseudoselector \"optional\". Used as a value, e.g.:
             [:.some-sel
              [optional {:opacity 0.8}]]"}
  optional sel/optional)

(def ^{:doc "CSS pseudoselector \"out-of-range\". Used as a value, e.g.:
             [:.some-sel
              [out-of-range {:display :none}]]"}
  out-of-range sel/out-of-range)

(def ^{:doc "CSS pseudoselector \"read-only\". Used as a value, e.g.:
             [:.some-sel
              [read-only {:background-color :blanchedalmond}]]"}
  read-only sel/read-only)

(def ^{:doc "CSS pseudoselector \"read-write\". Used as a value, e.g.:
             [:.some-sel
              [read-write {:cursor :pointer}]]"}
  read-write sel/read-write)

(def ^{:doc "CSS pseudoselector \"required\". Used as a value, e.g.:
             [:.some-sel
              [required {:border [[(px 2) :dotted :crimson]]}]]"}
  required sel/required)

(def ^{:doc "CSS pseudoselector \"root\". Used as a value, e.g.:
             [:.some-sel
              [root {:font-size (px 24)}]]"}
  root sel/root)

(def ^{:doc "CSS pseudoselector \"scope\". Used as a value, e.g.:
             [:.some-sel
              [scope {:background-color :lime}]]"}
  scope sel/scope)

(def ^{:doc "CSS pseudoselector \"target\". Used as a value, e.g.:
             [:.some-sel
              [target {:background-color :teal}]]"}
  target sel/target)

(def ^{:doc "CSS pseudoselector \"valid\". Used as a value, e.g.:
             [:.some-sel
              [valid {:background-color :limegreen}]]"}
  valid sel/valid)

(def ^{:doc "CSS pseudoselector \"visited\". Used as a value, e.g.:
             [:.some-sel
              [visited {:color :seashell}]]"}
  visited sel/visited)

;; pseudoclass selectors functions

#?(:clj
   (defmacro defpseudoclassfn
     "Creates a special CSS pseudoclass function, which compiles similarly as a standard
     CSS pseudoclass, but it is pseudoclass function with an argument.

     For example. if you wanted to only select every n-th argument:
     (defpseudoclassfn nth-child)
     (nth-child :odd)     ... compiles to   \"<parent>:nth-child(odd)\"
     (nth-child \"3n+1\")   ... compiles to   \"<parent>:nth-child(3n+1)\"

     Or if you wanted to show something based on the current language of the browser:
     (defpseudoclass lang)
     (lang \"en\") ... compiles to   \"<parent>:lang(en)\"

     To avoid collisions with some Clojure functions, you can give a second argument
     to defpseudoclassfn for a different translation to CSS:
     (defpseudoclass css-not \"not\")
     (css-not :p) ... compiles-to   \"not(p)\", which selects all descendants which are
     not a paragraph."
     ([pseudoclass] `(macros/defpseudoclassfn ~pseudoclass))
     ([pseudoclass compiles-to] `(macros/defpseudoclassfn ~pseudoclass ~compiles-to))))

(defn lang "Coming soon" [arg] (sel/lang arg))
(defn css-not "Coming soon" [arg] (sel/css-not arg))
(defn nth-child "Coming soon" [arg] (sel/nth-child arg))
(defn nth-last-child "Coming soon" [arg] (sel/nth-last-child arg))
(defn nth-last-of-type "Coming soon" [arg] (sel/nth-last-of-type arg))
(defn nth-of-type "Coming soon" [arg] (sel/nth-of-type arg))

;; pseudoelement selectors

#?(:clj
   (defmacro defpseudoelement
     "Defines a CSS pseudoelement. A CSS pseudoelement activates some CSS properties on
     a special part of a css-class/css-id/html-element.

     For example, first-letter: (defpseudoclass first-letter)
     When compiling a selectors sequence, e.g. [:.abc :#def first-letter], the resulting CSS
     selectors sequence will look like this: \".abc #def::first-letter\".

     So, what does it even do? We can give the first letter of an element a special value:
     ... [:.abc :p first-letter {:font-size (px 60)} ...] - this causes the first letter
     of every paragraph in an element with class .abc to have the first letter significantly
     bigger than the rest of the paragraph."
     [pseudoelement] `(macros/defpseudoelement ~pseudoelement)))

(def after "Coming soon" sel/after)
(def before "Coming soon" sel/before)
(def first-letter "Coming soon" sel/first-letter)
(def first-line "Coming soon" sel/first-line)
(def marker "Coming soon" sel/marker)
(def selection "Coming soon" sel/selection)

;; combinator selectors

#?(:clj
   (defmacro defcombinatorselector
     "Defines a combinator selector function which describes relationships between its
     arguments depending on the selector type:

     :#abc :.def is the default combinator selector - descendant selector. Affects all
     children with a class .def.

     child-selector \">\": is active when the given selectors are every of them a direct
     child of the previous one.

     adjacent-sibling (selector) \"+\": is active when the given html blocks elements or
     elements with a given class/id connected with the \"+\" sign are adjacent siblings.

     general-sibling (selector) \"~\" is active when the given selectors are on the same
     level of nesting; they do not have to be adjacent necessarily.

     Usage: [:.abc
             [:.def (child-selector :p :#ghi)]]
     compiles to   \".abc .def, .abc > p > #ghi\""
     [selector-name compiles-to]
     `(macros/defcombinatorselector ~selector-name ~compiles-to)))

(defn child-selector "Coming soon" [& selectors] (apply sel/child-selector selectors))
(defn adjacent-sibling "Coming soon" [& selectors] (apply sel/adjacent-sibling selectors))
(defn general-sibling "Coming soon" [& selectors] (apply sel/general-sibling selectors))

;; - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
;; COLORS

(defn rgb "Creates an rgb color."
  ([red green blue] (colors/rgb red green blue))
  ([[red green blue]] (colors/rgb red green blue)))

(defn rgba
  "Creates an rgba color."
  ([red green blue] (colors/rgba red green blue))
  ([red green blue alpha] (colors/rgba red green blue alpha))
  ([[red green blue alpha]] (colors/rgba red green blue alpha)))

(defn hsl "Creates an hsl color."
  ([hue saturation lightness] (colors/hsl hue saturation lightness))
  ([[hue saturation lightness]] (colors/hsl hue saturation lightness)))

(defn hsla "Creates an hsla color."
  ([hue saturation lightness] (colors/hsla hue saturation lightness))
  ([hue saturation lightness alpha] (colors/hsla hue saturation lightness alpha))
  ([[hue saturation lightness alpha]] (colors/hsla hue saturation lightness alpha)))
(alter-meta! #'rgba update :arglists conj '[[hue saturation lightness]])
(alter-meta! #'hsla update :arglists conj '[[hue saturation lightness]])

(defn rotate-hue "Transforms a color to hsl/hsla and rotates its hue by an angle." [color angle] (colors/rotate-hue color angle))

(defn triad-next "Transforms a color to hsl/hsla and rotates its hue by a third clockwise." [color] (colors/triad-next color))

(defn triad-previous "Transforms a color to hsl/hsla and rotates its hue by a third counterclockwise." [color] (colors/triad-previous color))

(defn opposite-hue "Transforms a color to hsl/hsla and rotates its hue by a half." [color] (colors/opposite-hue color))

(defn saturate
  "Transforms a color to hsl/hsla and adds an absolute saturation to it.
  E.g.: (saturate (rgb 50 100 150) \"15%\"),
  (saturate :gray 0.35), (saturate \"#123456\" (percent 50))"
  [color value] (colors/saturate color value))

(defn desaturate
  "Transforms a color to hsl/hsla and subtracts an absolute saturation from it.
  E.g.: (desaturate (rgb 50 100 150) \"15%\"),
  (desaturate :gray 0.35), (desaturate \"#123456\" (percent 50))"
  [color value] (colors/desaturate color value))

(defn scale-saturation
  "Transforms a color to hsl/hsla and adds multiplies its saturation with
  a numeric value.  E.g.: (saturate (rgb 50 100 150) \"15%\"),
  (saturate :gray 0.35), (saturate \"#123456\" (percent 50))"
  [color value] (colors/scale-saturation color value))

(defn lighten
  "Transforms a color to hsl/hsla and adds an absolute lightness to it.
  E.g.: (lighten (rgb 50 100 150) \"15%\"),
  (lighten :gray 0.35), (lighten \"#123456\" (percent 50))"
  [color value] (colors/lighten color value))

(defn darken
  "Transforms a color to hsl/hsla and subtracts an absolute lightness from it. E.g.:
  (darken (rgb 50 100 150) \"15%\"), (darken :gray 0.35), (darken \"#123456\" (percent 50))"
  [color value] (colors/darken color value))

(defn scale-lightness
  "Transforms a color to hsl/hsla and adds multiplies its lightness with
  a numeric value.  E.g.: (scale-lightness (rgb 50 100 150) \"15%\"),
  (scale-lightness :gray 0.35), (scale-lightness \"#123456\" (percent 50))"
  [color value] (colors/scale-lightness color value))

(defn opacify
  "Transforms a color to its with-alpha form and adds an absolute alpha to it.
  E.g.: (opacify(rgb 50 100 150) \"15%\"),
  (opacify :gray 0.35), (opacify \"#123456\" (percent 50))"
  [color value] (colors/opacify color value))

(defn transparentize
  "Transforms a color to its with-alpha form and subtracts an absolute alpha from it.
  E.g.: (transparentize (rgb 50 100 150) \"15%\"),
  (transparentize :gray 0.35), (transparentize \"#123456\" (percent 50))"
  [color value] (colors/transparentize color value))

(defn scale-alpha
  "Transforms a color to its with-alpha form and adds multiplies its alpha with
  a numeric value.  E.g.: (scale-alpha (rgb 50 100 150) \"15%\"),
  (scale-alpha :gray 0.35), (scale-alpha \"#123456\" (percent 50))"
  [color value] (colors/scale-alpha color value))

(defn with-hue
  "Given a color, transforms it into HSL and sets its hue to a given value.
  E.g.: (with-hue :red 150), (with-hue (rgba 200 150 100 0.3) 75)"
  [color hue] (colors/with-hue color hue))

(defn with-saturation
  "Given a color, transforms it into HSL and sets its saturation to a given value.
   E.g.: (with-saturation :red 0.5), (with-saturation (rgba 200 150 100 0.3) 0.5)"
  [color saturation] (colors/with-saturation color saturation))

(defn with-lightness
  "Given a color, transforms it into HSL and sets its lightness to a given value.
  E.g.: (with-lightness :red 0.8), (with-lightness (rgba 200 150 100 0.3) 0.8)"
  [color lightness] (colors/with-lightness color lightness))

(defn with-alpha
  "Given a color, sets its alpha to a given value.
  E.g.: (with-alpha :red 0.4), (with-alpha \"#FF7FCF\" 0.4)"
  [color alpha] (colors/with-alpha color alpha))

(defn mix-colors
  "Given any number of colors in any form (alpha-hex, non-alpha-hex, rgb, rgba,
  hsl, hsla), converts them to the most frequent type and mixes them."
  [color & more] (apply colors/mix-colors color more))

;; - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
;; AT-RULES: at the moment, these are available:
;;           @media, @font-face, @keyframes

(defn at-media
  "Takes a rules map and any number of media changes and creates a CSSAtRule instance
  with \"media\" identifier:

  (at-media {:screen    :only
             :max-width (px 600)
             :min-width (px 800)}
             [:& {:margin (join 15 0 15 20)}]
             [:.abc #:def {:margin  (px 20)
                           :padding (join 30 15)}
               [:span {:background-color (mix-colors :red :green)}]]
             [:footer {:font-size (em 1)}])

  The :& selector selects the current element.
  As you can see, you can nest the affected CSS hiccup how you only want.
  Special rules values: :screen :only => only screen
                   :screen true  => screen
                   :screen false => not screen

  {:screen    true
   :speech    false
   :max-width (px 600)
   :min-width (px 800)}
   => @media screen and not speech and (min-width: 600px) and (max-width: 800px) {...}"
  [rules & changes] (apply at-rules/at-media rules changes))

(defn at-font-face
  "Can be used for more convenient describing of @font-face. This is how example
  props-maps look like:

  {:src         [[(url \"../webfonts/woff2/roboto.woff2\") (css-format :woff2)]
                 [(url \"../webfonts/woff/roboto.woff\") (css-format :woff)]]
   :font-family \"Roboto\"
   :font-weight :normal
   :font-style  :italic}

  This function can receive any number of props maps so that you can also write
  the example above this way:

  {:src         (join (url \"../webfonts/woff2/roboto.woff2\") (css-format :woff2))}
  {:src         (join (url \"../webfonts/woff/roboto.woff\") (css-format :woff))
   :font-family \"Roboto\"
   :font-weight :normal
   :font-style  :italic}"
  [& props-maps]
  (apply at-rules/at-font-face props-maps))

#?(:clj
   (defmacro defkeyframes
     "Defines a CSS @keyframes animation. The animation name should have a unique symbol
     for later reference to it and then animation frames in a format [progress params]:

     (defkeyframes fade-in-opacity
                   [(percent 0) {:opacity 0}]
                   [(percent 25) {:opacity 0.1}]
                   [(percent 50) {:opacity 0.25}]
                   [(percent 75) {:opacity 0.5}]
                   [(percent 100) {:opacity 1}])

     Then, insert it to the CSS hiccup list to make tornado compile it for later usage:

     (def styles
        (list
           fade-in-opacity
           ...))

     After that, you can assign this animation to whatever element you want:

     (def styles
        (list
           fade-in-opacity
           [:.some-element {:animation-duration (ms 500)
                            :animation-name     fade-in-opacity}]
           [:#another-element {:animation-name  fade-in-opacity
                               :animation-delay (s 1.5)}]))

     You can also define from & to progress animations:

     (defkeyframes translate-animation
                   [:from {:transform (translate (px 100) (px 200))}]
                   [:to {:transform (translate (px 200) (px 400))}])"
     [animation-name & frames]
     `(macros/defkeyframes ~animation-name ~@frames)))

;; - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
;; COMMON UTILITY FUNCTIONS

(defn grid-areas
  "Given a sequence of grid-rows sequences, where each the element is represented by
  a keyword, a string or a symbol, return a grid-areas string:

  (grid-areas [(repeat 3 :header) [:. :content :.] (repeat 3 :footer)])
  Output CSS string: ``\"header header header\" \". content .\" \"footer footer footer\"``"
  [[first-row & more :as all-rows]] (common/grid-areas all-rows))

(defn important
  "After the expression is compiled, \" !important\" is appended to it:

  (important [(repeat 3 :auto)])   =>   \"auto auto auto !important\"
  (important :none   =>   \"none !important\"
  (important \"yellow\"   =>   \"yellow !important\""
  [expr] (common/important expr))

(defn join
  "A convenient function for simpler description of margin, padding or any similar CSS
  block which can can look like \"1px 2px 3px 4px\" after compilation. This function
  processes the input to create such a structure for much simpler description of the data.

  Example usage:

  (compile-expression (join 1 2 3))      ; is equal to [[(px 1) (px 2) (px 3)]]
  => \"1px 2px 3px\"


  (compile-expression (join em 1 2 3))      ; is equal to [[(em 1) (em 2) (em 3)]]
  => \"1em 2em 3em\"

  (compile-expression (join (em 3) 15 (fr 4) 3)
  ; is equal to [[(em 3) (px 15) (fr 4) (px 3)]]
  => \"3em 15px 4fr 3px\""
  ([value] (common/join value))
  ([unit-or-value & more-values] (apply common/join unit-or-value more-values)))