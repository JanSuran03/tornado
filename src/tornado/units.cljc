(ns tornado.units
  "A namespace for defining CSS units."
  (:require [tornado.types :as t])
  #?(:cljs (:require-macros [tornado.units :refer [defunit]]))
  #?(:clj (:import (tornado.types CSSUnit))))

#?(:clj (defn- make-cssunit-record
          "An internal CSSUnit function which takes the \"compiles-to\" unit parameter,
          e.g. \"%\" and the unit value parameter and creates a CSSUnit record."
          [unit value]
          (CSSUnit. unit value)))

#?(:clj (defmacro defunit
          "Creates a unit function which takes 1 argument and creates a CSSUnit record for future
           compilation. Defunit can take 1 arg: (defunit px)
                                     or 2 args: (defunit percent \"%\").

           Usage of the defined units: (px 15)       ...  compiles to \"15px\"
                                       (percent 33)  ...  compiles to \"33%\"

           CSSUnits can be added, subtracted, multiplied or divided by using function calc (you can
           also use these 4 keywords - they are defined just for better search in code:
           :add, :sub, :mul, :div

           E.g. (calc (px 500) :add 3 :mul (vw 5)) ... \"calc(500px + 3 * 5vw)\"."
          ([unit]
           (let [compiles-to (str unit)]
             `(defunit ~unit ~compiles-to)))
          ([identifier css-unit]
           `(def ~identifier (partial ~make-cssunit-record ~css-unit)))))

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