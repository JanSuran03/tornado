(ns tornado.core
  (:require [tornado.types]
            [tornado.units :as u]
            [tornado.compiler]
            [tornado.colors :as colors]
            [tornado.selectors :as sel]
            [tornado.functions :as f]
            [tornado.at-rules :as at-rules])
  (:import (tornado.types CSSAtRule CSSFunction CSSUnit
                          CSSPseudoClass CSSPseudoElement CSSColor)))

; Cannot refer macros: u/defunit, functions/defcssfn etc. They have to be redefined in a different way.
;; - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
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

;; - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
;; FUNCTIONS

(def ^{:doc      "Coming soon"
       :arglists '([{:keys [compiles-to args]}])} single-arg f/single-arg)
(def ^{:doc      "Coming soon"
       :arglists '([{:keys [compiles-to args]}])} commajoin f/commajoin)
(def ^{:doc      "Coming soon"
       :arglists '([{:keys [compiles-to args]}])} spacejoin f/spacejoin)

(defmacro ^{:doc      "Creates a cssfn function which which takes any number of arguments and creates
                       a CSSFunction record for future compilation.

                       Defcssfn can take 1 argument, which creates the function with the same name in CSS
                       and it will be expanded with str/join \", \" (default function - functions/commajoin):
                       (defcssfn some-fn) => my.namespace/some-fn
                       (some-fn \"arg1\" 42 (px 15)) ... compiles to   \"some-fn(arg1, 42, 15px)\"

                       or it can take 2 arguments:
                       (defcssfn css-min \"min\") => my.namespace/css-min
                       (css-min (px 500) (vw 60)) ... compiles to   \"min(500px, 60vw)\"

                       or we can pass it 3 arguments: ...
                       "
            :arglists '([fn-name]
                        [fn-name css-fn-or-fn-tail]
                        [clojure-fn-name compiles-to compile-fn])}
  dfefcssfn
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