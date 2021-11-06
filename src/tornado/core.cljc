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

(def ^{:doc      "Compiles an expression: a number, string, symbol or a record. If the expression is
                  a vector of sequential structures, compiles each of the structures and str/joins them
                  with a space. Then, str/joins all these str/spacejoined structures with a comma.

                  E.g.:
                  (compile-expression [[(px 15) (percent 20)] [:red :chocolate]])
                  => \"15px 20%, #FF0000 #D2691E\""
       :arglists '([expr])}
  compile-expression compiler/compile-expression)

(def ^{:doc      "Generates CSS from a standard Tornado vector (or a list of hiccup vectors). If
                  pretty-print? is set to false, compresses it as well. Then saves the compiled CSS
                  to a given file path, if provided in the flags.

                  You can also call this function only with the hiccup vector, without any flags."
       :arglists '([css-hiccup] [flags css-hiccup])}
  css compiler/css)

(def ^{:doc      "Generates CSS from a standard Tornado hiccup vector (or a list of hiccup vectors)
                  and pretty prints the output CSS string, which is useful for evaluating any tornado
                  code in the REPL."
       :arglists '([css-hiccup])}
  repl-css compiler/repl-css)

(def ^{:doc      "Can be used for compilation of a map of style parameters to a single string of html
                  style=\"...\" attribute. Receives the styles map as its argument and returns a string
                  of compiled style:

                  (html-style {:width            (px 500)
                               :height           (percent 15)
                               :color            :font-black
                               :background-color :teal})

                  => \"width:500px;height:15%;color:#1A1B1F;background-color:#008080\""
       :arglists '([styles-map])}
  html-style compiler/html-style)

(def ^{:doc      "Given a map of HTML style attributes described in Tornado, compiles all the values
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
       :arglists '([attributes-map])}
  compile-params compiler/compile-params)

; - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

;; UNITS

#?(:clj
   (defmacro ^{:doc      "Creates a unit function which takes 1 argument and creates a CSSUnit record for future
                          compilation. Defunit can take 1 arg: (defunit px)
                                                    or 2 args: (defunit percent \"%\").

                          Usage of the defined units: (px 15)       ...  compiles to \"15px\"
                                                      (percent 33)  ...  compiles to \"33%\"

                          CSSUnits can be added, subtracted, multiplied or divided by using function calc (you can
                          also use these 4 keywords - they are defined just for better search in code:
                          :add, :sub, :mul, :div

                       E.g. (calc (px 500) :add 3 :mul (vw 5)) ... \"calc(500px + 3 * 5vw)\"."
               :arglists '([unit] [identifier css-unit])}
     defunit
     [& args]
     `(macros/defunit ~@args)))

;; absolute size units

(def ^{:doc      "An absolute length unit, \"pixel\"."
       :arglists '([value])} px u/px)

(def ^{:doc      "An absolute length unit, \"point\"."
       :arglists '([value])} pt u/pt)

(def ^{:doc      "An absolute length unit, \"pica\"."
       :arglists '([value])} pc u/pc)

(def ^{:doc      "An absolute length unit, \"inch\""
       :arglists '([value])} in u/in)

(def ^{:doc      "An absolute length unit, \"centimeter\"."
       :arglists '([value])} cm u/cm)

(def ^{:doc      "An absolute length unit, \"millimeter\"."
       :arglists '([value])} mm u/mm)

;; relative size units

(def ^{:doc      "An absolute length unit, \"percent\" (\"%\"), can be used as color alpha in this library."
       :arglists '([value])} percent u/percent)

(def ^{:doc      "A relative length unit, \"rem\", depending on the size of the root element"
       :arglists '([value])} css-rem u/css-rem)

(def ^{:doc      "A relative length unit, \"em\", depending on the size of the parent element."
       :arglists '([value])} em u/em)

(def ^{:doc      "A relative length unit, \"fraction\", depending on the size of the parent element."
       :arglists '([value])} fr u/fr)

(def ^{:doc      "A relative length unit, \"viewport width\", based on the width of the window."
       :arglists '([value])} vw u/vw)

(def ^{:doc      "A relative length unit, \"viewport height\", based on the height of the window."
       :arglists '([value])} vh u/vh)

(def ^{:doc      "A relative length unit, minimum of vw and vh."
       :arglists '([value])} vmin u/vmin)

(def ^{:doc      "A relative length unit, maximum of vw and vh."
       :arglists '([value])} vmax u/vmax)

(def ^{:doc      "A relative length unit, equal to the line height."
       :arglists '([value])} lh u/lh)

;; time units

(def ^{:doc      "A time unit, \"second\"."
       :arglists '([value])} s u/s)

(def ^{:doc      "A time unit, \"millisecond\"."
       :arglists '([value])} ms u/ms)

;; angular units

(def ^{:doc      "An angular unit, \"degree\"."
       :arglists '([value])} deg u/deg)

(def ^{:doc      "An angular unit, \"radian\". Equal to 360°/2π"
       :arglists '([value])} rad u/rad)

(def ^{:doc      "An angular unit, \"gradian\". 100 gradians are equal to 90 degrees."
       :arglists '([value])} grad u/grad)

(def ^{:doc      "An angular unit, \"turn\". Represents one whole turn, equal to 360 degrees."
       :arglists '([value])} turn u/turn)

;; frequency units

(def ^{:doc      "A frequency unit, \"Hertz."
       :arglists '([value])} Hz u/Hz)

(def ^{:doc      "A frequency unit, \"kiloHertz."
       :arglists '([value])} kHz u/kHz)

;; resolution units
(def ^{:doc      "A resolution unit, \"dots per inches\"."
       :arglists '([value])} dpi u/dpi)

(def ^{:doc      "A resolution unit, \"dots per pixels\"."
       :arglists '([value])} dppx u/dppx)

(def ^{:doc      "A resolution unit, \"dots per centimeter\"."
       :arglists '([value])} dpcm u/dpcm)

; - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
;; FUNCTIONS

(def ^{:doc      "Coming soon"
       :arglists '([{:keys [compiles-to args]}])}
  single-arg f/single-arg)

(def ^{:doc      "Coming soon"
       :arglists '([{:keys [compiles-to args]}])}
  comma-join f/comma-join)

(def ^{:doc      "Coming soon"
       :arglists '([{:keys [compiles-to args]}])}
  space-join f/space-join)

#?(:clj
   (defmacro ^{:doc      "Creates a cssfn function which which takes any number of arguments and creates
                          a CSSFunction record for future compilation.

                          Defcssfn can take 1 argument, which creates the function with the same name in CSS
                          and it will be expanded with str/join \", \" (default function - commajoin):
                          (defcssfn some-fn) => my.namespace/some-fn
                          (some-fn \"arg1\" 42 (px 15)) ... compiles to   \"some-fn(arg1, 42, 15px)\"

                          or it can take 2 arguments:
                          (defcssfn css-min \"min\") => my.namespace/css-min
                          (css-min (px 500) (vw 60)) ... compiles to   \"min(500px, 60vw)\"

                          or the 2nd argument can be a compiling function (most commonly spacejoin, commajoin
                          or single-arg for separating the args, you can also define a special function for
                          that, but it should not ever be needed; read docs to these 3 functions):
                          (defcssfn calc spacejoin)
                          (calc (px 500) add 3 mul (vw 5)) ... \"calc(500px + 3 * 5vw)\"

                          you can also give defcssfn 3 arguments, where the 2nd one will be a special string
                          for translation to CSS and the 3rd one the compiling function."
               :arglists '([fn-name] [fn-name css-fn-or-fn-tail] [clojure-fn-name compiles-to compile-fn])}
     defcssfn
     [& args]
     `(macros/defcssfn ~@args)))

;; single arg functions

^{:doc      "Coming soon"
  :arglists '([arg])}

(def ^{:doc      "Coming soon"
       :arglists '([arg])} blur f/blur)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} brightness f/brightness)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} contrast f/contrast)

(def ^{:dpc      "A special function for @font-face."
       :arglists '([arg])} css-format f/css-format)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} grayscale f/grayscale)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} hue-rotate f/hue-rotate)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} invert f/invert)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} perspective f/perspective)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} rotate f/rotate)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} rotateX f/rotateX)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} rotateY f/rotateY)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} rotateZ f/rotateZ)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} sepia f/sepia)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} skewX f/skewX)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} skewY f/skewY)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} scaleX f/scaleX)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} scaleY f/scaleY)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} scaleZ f/scaleZ)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} translateX f/translateX)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} translateY f/translateY)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} translateZ f/translateZ)

;; comma-join functions

(def ^{:doc      "Coming soon"
       :arglists '([& args])} attr f/attr)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} counter f/counter)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} counters f/counters)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} cubic-bezier f/cubic-bezier)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} css-filter f/css-filter)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} hwb f/hwb)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} linear-gradient f/linear-gradient)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} matrix f/matrix)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} matrix3d f/matrix3d)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} css-max f/css-max)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} css-min f/css-min)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} polygon f/polygon)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} radial-gradient f/radial-gradient)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} repeating-linear-gradient f/repeating-linear-gradient)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} repeating-radial-gradient f/repeating-radial-gradient)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} rotate3d f/rotate3d)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} scale f/scale)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} scale3d f/scale3d)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} skew f/skew)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} translate f/translate)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} translate3d f/translate3d)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} url f/url)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} css-var f/css-var)

;; space-join functions

(def ^{:doc      "Coming soon"
       :arglists '([& args])} calc f/calc)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} circle f/circle)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} drop-shadow f/drop-shadow)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} ellipse f/ellipse)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} image f/image)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} inset f/inset)

(def ^{:doc      "Coming soon"
       :arglists '([& args])} symbols f/symbols)


;; - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
;; SELECTORS

;; attribute selectors

#?(:clj
   (defmacro ^{:doc      "Defines a CSS attribute selector. Those select all descendant elements containing
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
               :arglists '([selector-name compiles-to])}
     defattributeselector
     [& args]
     `(macros/defattributeselector ~@args)))

(def ^{:doc      "An attribute selector which selects all elements which have a given
                  attribute with any value, or all html elements on/below the current
                  nested selectors level which have a given attribute with any value."
       :arglists '([attribute] [tag attribute])}
  has-attr sel/has-attr)

(def ^{:doc      "Selects all descendants of a html tag which have a given parameter with a given value."
       :arglists '([attribute subvalue] [tag attribute subvalue])}
  has-val sel/has-val)

(def ^{:doc      "Selects all descendant elements which have a given parameter with a value containing
                  a given word (substring is not enough - a matching word separated by commas or spaces)."
       :arglists '([attribute subvalue] [tag attribute subvalue])}
  contains-word sel/contains-word)

(def ^{:doc      "Selects all descendant elements which have a given parameter with a value containing
                  a given substring (unlike the contains-word selector, the substring does not have to
                  be a whole word)."
       :arglists '([attribute subvalue] [tag attribute subvalue])}
  contains-subs sel/contains-subs)

(def ^{:doc      "Selects all descendant elements which have a given parameter with a value starting with
                  a given word (substring is not enough - a matching word separated by commas or spaces)."
       :arglists '([attribute subvalue] [tag attribute subvalue])}
  starts-with-word sel/starts-with-word)

(def ^{:doc      "Selects all descendant elements which have a given parameter with a value starting with
                  a given substring (unlike the contains-word selector, the substring does not have to be
                  a whole matching word."
       :arglists '([attribute subvalue] [tag attribute subvalue])}
  starts-with sel/starts-with)

(def ^{:doc      "Selects all descendant elements which have a given parameter with a value ending
                  with a given substring. The substring does not have to be a whole matching word."
       :arglists '([attribute subvalue] [tag attribute subvalue])}
  ends-with sel/ends-with)

;; pseudoclass selectors

#?(:clj
   (defmacro ^{:doc      "Defines a CSS pseudoclass. A CSS pseudoclass can activate some CSS properties on
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
               :arglists '([pseudoclass] [identifier css-pseudoclass])}
     defpseudoclass
     [& args]
     `(macros/defpseudoclass ~@args)))

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
               [:.err-msg {:display :flex}]]"}
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
   (defmacro ^{:doc      "Creates a special CSS pseudoclass function, which compiles similarly as a standard
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
               :arglists '([pseudoclass] [pseudoclass compiles-to])}
     defpseudoclassfn
     [& args]
     `(macros/defpseudoclassfn ~@args)))

(def ^{:doc      "Coming soon"
       :arglists '([arg])}
  lang sel/lang)

(def ^{:doc      "Coming soon"
       :arglists '([arg])}
  css-not sel/css-not)

(def ^{:doc      "Coming soon"
       :arglists '([arg])}
  nth-child sel/nth-child)

(def ^{:doc      "Coming soon"
       :arglists '([arg])}
  nth-last-child sel/nth-last-child)

(def ^{:doc      "Coming soon"
       :arglists '([arg])}
  nth-last-of-type sel/nth-last-of-type)

(def ^{:doc      "Coming soon"
       :arglists '([arg])}
  nth-of-type sel/nth-of-type)

;; pseudoelement selectors

#?(:clj
   (defmacro ^{:doc      "Defines a CSS pseudoelement. A CSS pseudoelement activates some CSS properties on
                          a special part of a css-class/css-id/html-element.

                          For example, first-letter: (defpseudoclass first-letter)
                          When compiling a selectors sequence, e.g. [:.abc :#def first-letter], the resulting CSS
                          selectors sequence will look like this: \".abc #def::first-letter\".

                          So, what does it even do? We can give the first letter of an element a special value:
                          ... [:.abc :p first-letter {:font-size (px 60)} ...] - this causes the first letter
                          of every paragraph in an element with class .abc to have the first letter significantly
                          bigger than the rest of the paragraph."
               :arglists '([pseudoelement])}
     defpseudoelement
     [& args]
     `(macros/defpseudoelement ~@args)))

(def ^{:doc "Coming soon"}
  after sel/after)

(def ^{:doc "Coming soon"}
  before sel/before)

(def ^{:doc "Coming soon"}
  first-letter sel/first-letter)

(def ^{:doc "Coming soon"}
  first-line sel/first-line)

(def ^{:doc "Coming soon"}
  marker sel/marker)

(def ^{:doc "Coming soon"}
  selection sel/selection)

;; combinator selectors

#?(:clj
   (defmacro ^{:doc      "Defines a combinator selector function which describes relationships between its
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
               :arglists '([selector-name compiles-to])}
     defcombinatorselector
     [& args]
     `(macros/defcombinatorselector ~@args)))

(def ^{:doc      "Coming soon"
       :arglists '([& selectors])} child-selector sel/child-selector)

(def ^{:doc      "Coming soon"
       :arglists '([& selectors])} adjacent-sibling sel/adjacent-sibling)

(def ^{:doc      "Coming soon"
       :arglists '([& selectors])} general-sibling sel/general-sibling)

;; - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
;; COLORS

(def ^{:doc      "Creates an rgb color."
       :arglists '([red green blue] [[red green blue]])}
  rgb colors/rgb)

(def ^{:doc      "Creates an rgba color."
       :arglists '([red green blue] [red green blue alpha] [[red green blue]] [[red green blue alpha]])}
  rgba colors/rgba)

(def ^{:doc      "Creates an hsl color."
       :arglists '([hue saturation lightness] [[hue saturation lightness]])}
  hsl colors/hsl)

(def ^{:doc      "Creates an hsla color."
       :arglists '([hue saturation lightness] [hue saturation lightness alpha] [[hue saturation lightness]] [[hue saturation lightness alpha]])}
  hsla colors/hsla)

(def ^{:doc      "Transforms a color to hsl/hsla and rotates its hue by an angle."
       :arglists '([color angle])}
  rotate-hue colors/rotate-hue)

(def ^{:doc      "Transforms a color to hsl/hsla and rotates its hue by a third clockwise."
       :arglists '([color])}
  triad-next colors/triad-next)

(def ^{:doc      "Transforms a color to hsl/hsla and rotates its hue by a third counterclockwise."
       :arglists '([color])}
  triad-previous colors/triad-previous)

(def ^{:doc      "Transforms a color to hsl/hsla and rotates its hue by a half."
       :arglists '([color])}
  opposite-hue colors/opposite-hue)

(def ^{:doc      "Transforms a color to hsl/hsla and adds an absolute saturation to it.
                  E.g.: (saturate (rgb 50 100 150) \"15%\"),
                  (saturate :gray 0.35), (saturate \"#123456\" (percent 50))"
       :arglists '([color value])}
  saturate colors/saturate)

(def ^{:doc      "Transforms a color to hsl/hsla and subtracts an absolute saturation from it.
                  E.g.: (desaturate (rgb 50 100 150) \"15%\"),
                  (desaturate :gray 0.35), (desaturate \"#123456\" (percent 50))"
       :arglists '([color value])}
  desaturate colors/desaturate)

(def ^{:doc      "Transforms a color to hsl/hsla and adds multiplies its saturation with
                  a numeric value.  E.g.: (saturate (rgb 50 100 150) \"15%\"),
                  (saturate :gray 0.35), (saturate \"#123456\" (percent 50))"
       :arglists '([color value])}
  scale-saturation colors/scale-saturation)

(def ^{:doc      "Transforms a color to hsl/hsla and adds an absolute lightness to it.
                  E.g.: (lighten (rgb 50 100 150) \"15%\"),
                  (lighten :gray 0.35), (lighten \"#123456\" (percent 50))"
       :arglists '([color value])}
  lighten colors/lighten)

(def ^{:doc      "Transforms a color to hsl/hsla and subtracts an absolute lightness from it. E.g.:
                  (darken (rgb 50 100 150) \"15%\"), (darken :gray 0.35), (darken \"#123456\" (percent 50))"
       :arglists '([color value])}
  darken colors/darken)

(def ^{:doc      "Transforms a color to hsl/hsla and adds multiplies its lightness with
                  a numeric value.  E.g.: (scale-lightness (rgb 50 100 150) \"15%\"),
                  (scale-lightness :gray 0.35), (scale-lightness \"#123456\" (percent 50))"
       :arglists '([color value])}
  scale-lightness colors/scale-lightness)

(def ^{:doc      "Transforms a color to its with-alpha form and adds an absolute alpha to it.
                  E.g.: (opacify(rgb 50 100 150) \"15%\"),
                  (opacify :gray 0.35), (opacify \"#123456\" (percent 50))"
       :arglists '([color value])}
  opacify colors/opacify)

(def ^{:doc      "Transforms a color to its with-alpha form and subtracts an absolute alpha from it.
                  E.g.: (transparentize (rgb 50 100 150) \"15%\"),
                  (transparentize :gray 0.35), (transparentize \"#123456\" (percent 50))"
       :arglists '([color value])}
  transparentize colors/transparentize)

(def ^{:doc      "Transforms a color to its with-alpha form and adds multiplies its alpha with
                  a numeric value.  E.g.: (scale-alpha (rgb 50 100 150) \"15%\"),
                  (scale-alpha :gray 0.35), (scale-alpha \"#123456\" (percent 50))"
       :arglists '([color value])}
  scale-alpha colors/scale-alpha)

(def ^{:doc      "Given a color, transforms it into HSL and sets its hue to a given value.
             E.g.: (with-hue :red 150), (with-hue (rgba 200 150 100 0.3) 75)"
       :arglists '([color hue])}
  with-hue colors/with-hue)

(def ^{:doc      "Given a color, transforms it into HSL and sets its saturation to a given value.
             E.g.: (with-saturation :red 0.5), (with-saturation (rgba 200 150 100 0.3) 0.5)"
       :arglists '([color hue])}
  with-saturation colors/with-saturation)

(def ^{:doc      "Given a color, transforms it into HSL and sets its lightness to a given value.
             E.g.: (with-lightness :red 0.8), (with-lightness (rgba 200 150 100 0.3) 0.8)"
       :arglists '([color hue])}
  with-lightness colors/with-lightness)

(def ^{:doc      "Given a color, sets its alpha to a given value.
             E.g.: (with-alpha :red 0.4), (with-alpha \"#FF7FCF\" 0.4)"
       :arglists '([color hue])}
  with-alpha colors/with-alpha)

(def ^{:doc      "Given any number of colors in any form (alpha-hex, non-alpha-hex, rgb, rgba,
                  hsl, hsla), converts them to the most frequent type and mixes them."
       :arglists '([color & more])}
  mix-colors colors/mix-colors)

;; - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
;; AT-RULES: at the moment, these are available:
;;           @media, @font-face, @keyframes

(def ^{:doc      "Takes a rules map and any number of media changes and creates a CSSAtRule instance
                  with \"media\" identifier:

                  (at-media {:screen    :only
                           :max-width (px 600)
                           :min-width (px 800}
                           [:& {:margin [[(px 15 0 (px 15) (px 20]]
                           [:.abc #:def {:margin  (px 20)
                                         :padding [[(px 30) (px 15)]]
                             [:span {:background-color (mix-colors :red :green)]]
                           [:footer {:font-size (em 1)])

                  The :& selector selects the current element.
                  As you can see, you can nest the affected CSS hiccup how you only want.
                  Special rules values: :screen :only => only screen
                                   :screen true  => screen
                                   :screen false => not screen

                  {:screen    true
                   :speech    false
                   :max-width (px 600)
                   :min-width (px 800}
                   => @media screen and not speech and (min-width: 600px) and (max-width: 800px) {..."
       :arglists '([rules & changes])}
  at-media at-rules/at-media)

(def ^{:doc      "Can be used for more convenient describing of @font-face. This is how example
                  props-maps look like:

                  {:src         [[(url \"../webfonts/woff2/roboto.woff2\") (css-format :woff2)]
                                 [(url \"../webfonts/woff/roboto.woff\") (css-format :woff)]]
                   :font-family \"Roboto\"
                   :font-weight :normal
                   :font-style  :italic}

                  This function can receive any number of props maps so that you can also write
                  the example above this way:

                  {:src         [[(url \"../webfonts/woff2/roboto.woff2\") (css-format :woff2)]]}
                  {:src         [[(url \"../webfonts/woff/roboto.woff\") (css-format :woff)]]
                   :font-family \"Roboto\"
                   :font-weight :normal
                   :font-style  :italic}"
       :arglists '([& props-maps])}
  at-font-face at-rules/at-font-face)

#?(:clj
   (defmacro ^{:doc      "Defines a CSS @keyframes animation. The animation name should have a unique symbol
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
                                                 :animation-name     fade-in-opacity)}]
                                [:#another-element {:animation-name  fade-in-opacity
                                                    :animation-delay (s 1.5)}]))

                          You can also define from & to progress animations:

                          (defkeyframes translate-animation
                                        [:from {:transform (translate (px 100) (px 200)}]
                                        [:to {:transform (translate (px 200) (px 400)}])"
               :arglists '([animation-name & frames])}
     defkeyframes
     [& args]
     `(macros/defkeyframes ~@args)))

;; - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
;; COMMON UTILITY FUNCTIONS

(def ^{:doc      "Given a sequence of grid-rows sequences, where each the element is represented by
                  a keyword, a string or a symbol, return a grid-areas string:

                  (grid-areas [(repeat 3 :header) [:. :content :.] (repeat 3 :footer)])
                  Output CSS string: ``\"header header header\" \". content .\" \"footer footer footer\"``"
       :arglists '([[first-row & more :as all-rows]])}
  grid-areas common/grid-areas)

(def ^{:doc      "After the expression is compiled, \" !important\" is appended to it:

                  (important [(repeat 3 :auto)])   =>   \"auto auto auto !important\"
                  (important :none   =>   \"none !important\"
                  (important \"yellow\"   =>   \"yellow !important\""
       :arglists '([expr])}
  important common/important)

(def ^{:doc      "A convenient function for simpler description of margin, padding or any similar CSS
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
       :arglists '([value] [unit-or-value & more-values])}
  join common/join)