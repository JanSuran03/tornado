(ns tornado.units
  (:require [tornado.types])
  (:import (tornado.types CSSUnit)))

(defn- make-cssunit-record
  "An internal CSSUnit function which takes the \"compiles-to\" unit parameter,
  e.g. \"%\" and the unit value parameter and creates a CSSUnit record."
  [unit value]
  (CSSUnit. unit value))

(defmacro defunit
  "Creates a CSS unit, where a function (`identifier` <value>) can be used in
  code to give the unit a value. `css-unit` is for compiling the unit to CSS.
  If only one argument is given, the css-unit is same as the identifier.
  Accepts an optional doc-string as the 3rd argument.

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
(defunit px)
(defunit pt)
(defunit pc)
(defunit in)
(defunit cm)
(defunit mm)

;; relative size units
(defunit percent "%")
(defunit css-rem "rem")
(defunit em)
(defunit fr)
(defunit vw)
(defunit vh)
(defunit vmin)
(defunit vmax)
(defunit lh)

;; time units
(defunit s)
(defunit ms)

;; angular units
(defunit deg)
(defunit rad)
(defunit grad)
(defunit turn)

;; frequency units
(defunit Hz)
(defunit kHz)

;; resolution units
(defunit dpi)
(defunit dppx)
(defunit dpcm)