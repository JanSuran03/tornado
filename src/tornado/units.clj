(ns tornado.units
  (:require [tornado.types])
  (:import (tornado.types CSSUnit)))

(defn make-cssunit-record
  "Creates a unit function which takes a value parameter and gives it to
  CSSUnit record."
  [unit value]
  (CSSUnit. unit value))

(defmacro defunit
  "Creates a CSS unit, where a function (`identifier` <value>) can be used in
  code to give the unit a value. `css-unit` is for compiling the unit into CSS.
  If only one argument is given, the css-unit is same as the identifier.
  Accepts an optional doc-string as its 3rd argument.

  With an equal identifier:
     (defunit px)
     => #'tornado.units/px
     (px 15)
     => #tornado.types.CSSUnit{:compiles-to \"px\"
                               :value       15}

  With a different identifier:
     (defunit percent \"%\")
     => #'tornado.units/percent
     (percent 20)
     => #tornado.types.CSSUnit{:compiles-to \"%\"
                               :value       20}

  With a documentation:
     (defunit hs \"hs\" \"A time unit, halfsecond.\")
     **you have to include the 2nd arg**"
  ([unit]
   (let [compiles-to (str unit)]
     `(defunit ~unit ~compiles-to nil)))
  ([identifier css-unit]
   `(defunit ~identifier ~css-unit nil))
  ([identifier css-unit doc]
   `(do (def ~identifier (partial ~make-cssunit-record ~css-unit))
        (alter-meta! #'~identifier assoc :doc ~doc))))

;; absolute size units
(defunit px "px" "An absolute length unit \"pixel\".")
(defunit pt "pt" "An absolute length unit \"point\".")
(defunit pc "pc" "An absolute length unit \"pica\".")
(defunit in "in" "An absolute length unit \"inch\"")
(defunit cm "cm" "An absolute length unit \"centimeter\".")
(defunit mm "mm" "An absolute length unit \"millimeter\".")

;; relative size units
(defunit percent "%" "An absolute length unit \"percent\", can also\nbe used as color alpha in this library.")
(defunit rem* "rem" "A relative length unit \"rem\", depending\non the size of the root element")
(defunit em "em" "A relative length unit \"em\", depending\non the size of the parent element.")
(defunit fr "fr" "A relative length unit \"fraction\", depending\non the size of the parent element. Accepts values 0-1.")
(defunit vw "vw" "A relative length unit \"viewport width\", based on\nthe width of the window. Accepts values 0-100.")
(defunit vh "vh" "A relative length unit \"viewport height\", based on\nthe height of the window. Accepts values 0-100.")
(defunit vmin "vmin" "A relative length unit, minimum of vw and vh.\nAccepts values 0-100.")
(defunit vmax "vmax" "A relative length unit, maximum of vw and vh.\nAccepts values 0-100.")
(defunit lh "lh" "A relative length unit, equal to the line height.")

;; time units
(defunit s "s" "A time unit, \"second\".")
(defunit ms "ms" "A time unit, \"millisecond\".")

;; angular units
(defunit deg "deg" "An angular unit, \"degree\".")
(defunit rad "rad" "An angular unit, \"radian\". Equal to 360°/2π")
(defunit grad "grad" "An angular unit, \"gradian\". 100 gradians\nare equal to 90 degrees.")
(defunit turn "turn" "An angular unit, \"turn\". Represents one\n whole turn, equal to 360 degrees.")

;; frequency units
(defunit Hz "Hz" "A frequency unit, \"Hertz.")
(defunit kHz "kHz" "A frequency unit, \"kiloHertz.")

;; resolution units
(defunit dpi "dpi" "A resolution unit, \"dots per inches\".")
(defunit dppx "dppx" "A resolution unit, \"dots per pixels\".")
(defunit dpcm "dpcm" "A resolution unit, \"dots per centimeter\".")