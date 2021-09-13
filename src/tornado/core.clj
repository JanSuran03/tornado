(ns tornado.core
  (:require [tornado.types]
            [tornado.units :as u]
            [tornado.compiler :refer [compile-expression]]
            [tornado.colors :as colors]
            [tornado.selectors :as sel]
            [tornado.functions :as f]
            [tornado.at-rules :as at-rules]
            [tornado.util :as util])
  (:import (tornado.types CSSAtRule CSSFunction CSSUnit
                          CSSPseudoClass CSSPseudoElement CSSColor)))

;; UNITS

(defmacro ^{:doc      "Creates a unit function which takes 1 argument and creates a CSSUnit record for future
                       compilation. Defunit can either take 1 arg: (defunit px), 2 args: (defunit percent \"%\")
                       or 3 args: (defunit hs \"hs\" \"A time unit, halfsecond.\") (optional doc, but the 2nd arg
                       has to be given like in this case as well.

                       Usage of the defined units: (px 15)      ... compiles to \"15px\"
                                                   (percent 33) ... compiles to \"33%\"

                       CSSUnits can be added, subtracted, multiplied or divided by using function calc (and
                       maybe these 4 symbols, where they are defined just for better search after them in code:

                       add, sub, mul, div, e.g. (calc (px 500) add 3 mul (vw 5)) ... \"calc(500px + 3 * 5vw)\"."
            :arglists '([unit]
                        [identifier css-unit]
                        [identifier css-unit doc])}
  defunit
  [& args]
  `(u/defunit ~@args))

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
  commajoin f/commajoin)

(def ^{:doc      "Coming soon"
       :arglists '([{:keys [compiles-to args]}])}
  spacejoin f/spacejoin)

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
            :arglists '([fn-name]
                        [fn-name css-fn-or-fn-tail]
                        [clojure-fn-name compiles-to compile-fn])}
  defcssfn
  [& args]
  `(f/defcssfn ~@args))

;; single arg functions

^{:doc      "Coming soon"
  :arglists '([arg])}

(def ^{:doc      "Coming soon"
       :arglists '([arg])} blur f/blur)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} brightness f/brightness)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} contrast f/contrast)

(def ^{:dpc "A special function for @font-face."
       :arglists '([arg])} css-format f/css-format)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} grayscale f/grayscale)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} hue-rotate f/hue-rotate)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} invert f/invert)

(def ^{:doc      "Coming soon"
       :arglists '([arg])} opacity f/opacity)

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
       :arglists '([arg])} saturate f/saturate)

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

(defmacro ^{:doc      "Defines a CSS attribute selector. Those select all descendant elements containing
                       a given attribute, of which the value matches a given substring. All attribute
                       selectors have different conditions for matching:
                       Start with a word, start with a substring, contain a word, contain a substring,
                       end with a substring, have a given value, have a given attribute with any value.

                       By attributes, it is meant html attributes, e.g. span[class~=\"info\"] selects
                       all spans with a class containing a whole word \"info\".
                       In tornado, we can represent this by (contains-word :span :class \"info\").

                       We can also use (contains-word :class \"info\") to mark all elements with that
                       class ... compiles to [class~´\"info\"] and affects all elements with that condition."
            :arglists '([selector-name compiles-to])}
  defattributeselector
  [& args]
  `(sel/defattributeselector ~@args))

(def ^{:doc      "An attribute selector which selects all elements which have a given
                  attribute with any value, or all html elements on/below the current
                  nested selectors level which have a given attribute with any value."
       :arglists '([attribute]
                   [tag attribute])} has-attr sel/has-attr)

(def ^{:doc      "Selects all descendants of a html tag which have a given parameter with a given value."
       :arglists '([attribute subvalue]
                   [tag attribute subvalue])}
  has-val sel/has-val)

(def ^{:doc      "Selects all descendant elements which have a given parameter with a value containing
                  a given word (substring is not enough - a matching word separated by commas or spaces)."
       :arglists '([attribute subvalue]
                   [tag attribute subvalue])}
  contains-word sel/contains-word)

(def ^{:doc      "Selects all descendant elements which have a given parameter with a value starting with
                  a given word (substring is not enough - a matching word separated by commas or spaces)."
       :arglists '([attribute subvalue]
                   [tag attribute subvalue])}
  starts-with-word sel/starts-with-word)

(def ^{:doc      "Selects all descendant elements which have a given parameter with a value starting with
                  a given substring (unlike the contains-word selector, the substring does not have to be
                  a whole matching word."
       :arglists '([attribute subvalue]
                   [tag attribute subvalue])}
  starts-with sel/starts-with)

(def ^{:doc      "Selects all descendant elements which have a given parameter with a value ending
                  with a given substring. The substring does not have to be a whole matching word."
       :arglists '([attribute subvalue]
                   [tag attribute subvalue])}
  ends-with sel/ends-with)

(def ^{:doc      "Selects all descendant elements which have a given parameter with a value containing
                  a given substring (unlike the contains-word selector, the substring does not have to
                  be a whole word)."
       :arglists '([attribute subvalue]
                   [tag attribute subvalue])}
  contains-subs sel/contains-subs)

;; pseudoclass selectors

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
            :arglists '([pseudoclass]
                        [indetifier css-pseudoclass])}
  defpseudoclass
  [& args]
  `(sel/defpseudoclass ~@args))

(def ^{:doc "Coming soon"}
  active sel/active)

(def ^{:doc "Coming soon"}
  checked sel/checked)

(def ^{:doc "Coming soon"}
  default sel/default)

(def ^{:doc "Coming soon"}
  disabled sel/disabled)

(def ^{:doc "Coming soon"}
  css-empty sel/css-empty)

(def ^{:doc "Coming soon"}
  enabled sel/enabled)

(def ^{:doc "Coming soon"}
  css-first sel/css-first)

(def ^{:doc "Coming soon"}
  first-child sel/first-child)

(def ^{:doc "Coming soon"}
  first-of-type sel/first-of-type)

(def ^{:doc "Coming soon"}
  fullscreen sel/fullscreen)

(def ^{:doc "Coming soon"}
  focus sel/focus)

(def ^{:doc "Coming soon"}
  hover sel/hover)

(def ^{:doc "Coming soon"}
  indeterminate sel/indeterminate)

(def ^{:doc "Coming soon"}
  in-range sel/in-range)

(def ^{:doc "Coming soon"}
  invalid sel/invalid)

(def ^{:doc "Coming soon"}
  last-child sel/last-child)

(def ^{:doc "Coming soon"}
  last-of-type sel/last-of-type)

(def ^{:doc "Coming soon"}
  left sel/left)

(def ^{:doc "Coming soon"}
  links sel/links)

(def ^{:doc "Coming soon"}
  only-child sel/only-child)

(def ^{:doc "Coming soon"}
  only-of-type sel/only-of-type)

(def ^{:doc "Coming soon"}
  optional sel/optional)

(def ^{:doc "Coming soon"}
  out-of-range sel/out-of-range)

(def ^{:doc "Coming soon"}
  read-only sel/read-only)

(def ^{:doc "Coming soon"}
  read-write sel/read-write)

(def ^{:doc "Coming soon"}
  required sel/required)

(def ^{:doc "Coming soon"}
  right sel/right)

(def ^{:doc "Coming soon"}
  root sel/root)

(def ^{:doc "Coming soon"}
  scope sel/scope)

(def ^{:doc "Coming soon"}
  target sel/target)

(def ^{:doc "Coming soon"}
  valid sel/valid)

(def ^{:doc "Coming soon"}
  visited sel/visited)

;; pseudoclass selectors functions

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
            :arglists '([pseudoclass]
                        [pseudoclass compiles-to])}
  defpseudoclassfn
  [& args]
  `(sel/defpseudoclassfn ~@args))

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

(def ^{:doc      "Coming soon"
       :arglists '([arg])}
  nth-of-type sel/nth-of-type)

(def ^{:doc      "Coming soon"
       :arglists '([arg])}
  nth-of-type sel/nth-of-type)

;; pseudoelement selectors

(defmacro ^{:doc      "Defines a CSS pseudoelement. A CSS pseudoelement activates some CSS properties on
                      a special part of a css-class/css-id/html-element.

                      For example, first-letter: (defpseudoclass first-letter)
                      When compiling a selectors sequence, e.g. [:.abc :#def first-letter], the resulting CSS
                      selectors sequence will look like this: \".abc #def::first-letter\".

                      So, what does it even do? We can give the first letter of an element a special value:
                      ... [:.abc :p first-letter {:font-size (u/px 60)} ...] - this causes the first letter
                      of every paragraph in an element with class .abc to have the first letter significantly
                      bigger than the rest of the paragraph."
            :arglists '([pseudoelement])}
  defpseudoelement
  [& args]
  `(sel/defpseudoelement ~@args))

(def ^{:doc "Coming soon"}
  after sel/after)

(def ^{:doc "Coming soon"}
  before sel/before)

(def ^{:doc "Coming soon"}
  first-letter sel/first-letter)

(def ^{:doc "Coming soon"}
  first-line sel/first-line)

(def ^{:doc "Coming soon"}
  selection sel/selection)

;; combinator selectors

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
  `(sel/defcombinatorselector ~@args))

(def ^{:doc      "Coming soon"
       :arglists '([& selectors])} child-selector sel/child-selector)

(def ^{:doc      "Coming soon"
       :arglists '([& selectors])} adjacent-sibling sel/adjacent-sibling)

(def ^{:doc      "Coming soon"
       :arglists '([& selectors])} general-sibling sel/general-sibling)

;; - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
;; COLORS

(def ^{:doc "Creates an rgb color."
       :arglists '([red green blue]
                   [[red green blue]])}
  rgb colors/rgb)

(def ^{:doc "Creates an rgba color."
       :arglists '([red green blue]
                   [red green blue alpha]
                   [[red green blue]]
                   [[red green blue alpha]])}
  rgba colors/rgba)

(def ^{:doc      "Creates an hsl color."
       :arglists '([hue saturation lightness]
                   [[hue saturation lightness]])}
  hsl colors/hsl)

(def ^{:doc      "Creates an hsla color."
       :arglists '([hue saturation lightness]
                   [hue saturation lightness alpha]
                   [[hue saturation lightness]]
                   [[hue saturation lightness alpha]])}
  hsla colors/hsla)

(def ^{:doc "Transforms a color to hsl/hsla and rotates its hue by an angle."
       :arglists '([color angle])}
  rotate-hue colors/rotate-hue)

(def ^{:doc "Transforms a color to hsl/hsla and rotates its hue by a third clockwise."
       :arglists '([color])}
  triad-next colors/triad-next)

(def ^{:doc "Transforms a color to hsl/hsla and rotates its hue by a third counterclockwise."
       :arglists '([color])}
  triad-previous colors/triad-previous)

(def ^{:doc "Transforms a color to hsl/hsla and rotates its hue by a half."
       :arglists '([color])}
  opposite-hue colors/opposite-hue)

(def ^{:doc "Transforms a color to hsl/hsla and adds an absolute saturation to it.
             E.g.: (saturate (rgb 50 100 150) \"15%\"),
             (saturate :gray 0.35), (saturate \"#123456\" (percent 50))"
       :arglists '([color value])}
  saturate colors/saturate)

(def ^{:doc "Transforms a color to hsl/hsla and subtracts an absolute saturation from it.
             E.g.: (desaturate (rgb 50 100 150) \"15%\"),
             (desaturate :gray 0.35), (desaturate \"#123456\" (percent 50))"
       :arglists '([color value])}
  desaturate colors/desaturate)

(def ^{:doc "Transforms a color to hsl/hsla and adds multiplies its saturation with
             a numeric value.  E.g.: (saturate (rgb 50 100 150) \"15%\"),
             (saturate :gray 0.35), (saturate \"#123456\" (percent 50))"
       :arglists '([color value])}
  scale-saturation colors/scale-saturation)

(def ^{:doc "Transforms a color to hsl/hsla and adds an absolute lightness to it.
             E.g.: (lighten (rgb 50 100 150) \"15%\"),
             (lighten :gray 0.35), (lighten \"#123456\" (percent 50))"
       :arglists '([color value])}
  lighten colors/lighten)

(def ^{:doc "Transforms a color to hsl/hsla and subtracts an absolute lightness from it. E.g.:
             (darken (rgb 50 100 150) \"15%\"), (darken :gray 0.35), (darken \"#123456\" (percent 50))"
       :arglists '([color value])}
  darken colors/darken)

(def ^{:doc "Transforms a color to hsl/hsla and adds multiplies its lightness with
             a numeric value.  E.g.: (scale-lightness (rgb 50 100 150) \"15%\"),
             (scale-lightness :gray 0.35), (scale-lightness \"#123456\" (percent 50))"
       :arglists '([color value])}
  scale-lightness colors/scale-lightness)

(def ^{:doc "Transforms a color to its with-alpha form and adds an absolute alpha to it.
             E.g.: (opacify(rgb 50 100 150) \"15%\"),
             (opacify :gray 0.35), (opacify \"#123456\" (percent 50))"
       :arglists '([color value])}
  opacify colors/opacify)

(def ^{:doc "Transforms a color to its with-alpha form and subtracts an absolute alpha from it.
             E.g.: (transparentize (rgb 50 100 150) \"15%\"),
             (transparentize :gray 0.35), (transparentize \"#123456\" (percent 50))"
       :arglists '([color value])}
  transparentize colors/transparentize)

(def ^{:doc "Transforms a color to its with-alpha form and adds multiplies its alpha with
             a numeric value.  E.g.: (scale-alpha (rgb 50 100 150) \"15%\"),
             (scale-alpha :gray 0.35), (scale-alpha \"#123456\" (percent 50))"
       :arglists '([color value])}
  scale-alpha colors/scale-alpha)

(def ^{:doc "Given any number of colors in any form (alpha-hex, non-alpha-hex, rgb, rgba,
             hsl, hsla), converts them to the most frequent type and mixes them."
       :arglists '([color & more])}
  mix-colors colors/mix-colors)

;; - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
;; AT-RULES: @media, @font-face, @keyframes, @import. @feature

(def ^{:doc "Takes a rules map and any number of media changes and creates a CSSAtRule instance
             with \"media\" identifier:

             (at-media {:screen    :only
                        :max-width (u/px 600)
                        :min-width (u/px 800}
                        [:& {:margin [[(u/px 15 0 (u/px 15) (u/px 20]]
                        [:.abc #:def {:margin  (u/px 20)
                                      :padding [[(u/px 30) (u/px 15)]]
                          [:span {:background-color (colors/mix :red :green)]]
                        [:footer {:font-size (u/em 1)])

             The :& selector selects the current element.
             As you can see, you can nest the affected CSS hiccup how you only want.
             Special rules values: :screen :only => only screen
                                   :screen true  => screen
                                   :screen false => not screen

             {:screen    true
              :speech    false
              :max-width (u/px 600)
              :min-width (u/px 800}
              => @media screen and not speech and (min-width: 600px) and (max-width: 600px) {..."
       :arglists '([rules & changes])}
  at-media at-rules/at-media)

(def at-font-face at-rules/at-font-face)

;; - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
;; UTILITY FUNCTIONS

(def ^{:doc "A special utility function for compilation: all the arguments will be str/joined
             with \", \" during the compilation.

             Example usage:
             (with-comma
                [[(url \"/fonts/OpenSans-Regular-webfont.woff2\") (css-format :woff2)]]
                [[(url \"/fonts/OpenSans-Regular-webfont.woff\") (css-format :woff)]])}

             => \"url(/fonts/OpenSans-Regular-webfont.woff2) format(\"woff2\"),\n
                 url(/fonts/OpenSans-Regular-webfont.woff) format(\"woff\")\""
       :arglists '([& args])} with-comma util/with-comma)