(ns tornado.core
  (:require [tornado.types]
            [tornado.units :as u]
            [tornado.compiler]
            [tornado.colors :as colors]
            [tornado.selectors :as sel]
            [tornado.functions :as functions]
            [tornado.at-rules :as at-rules])
  (:import (tornado.types CSSAtRule CSSFunction CSSUnit
                          CSSPseudoClass CSSPseudoElement CSSColor)))

;; cannot refer macros: units/defunit, functions/defcssfn, defpseudoclass etc.
;; - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
;; UNITS

;; absolute size units
(def px "An absolute length unit, \"pixel\"." u/px)
(def pt "An absolute length unit, \"point\"." u/pt)
(def pc "An absolute length unit, \"pica\"." u/pc)
(def in "An absolute length unit, \"inch\"" u/in)
(def cm "An absolute length unit, \"centimeter\"." u/cm)
(def mm "An absolute length unit, \"millimeter\"." u/mm)

;; relative size units
(def percent "An absolute length unit, \"percent\", can also be used as color alpha in this library." u/percent)
(def css-rem "A relative length unit, \"rem\", depending on the size of the root element" u/css-rem)
(def em "A relative length unit, \"em\", depending on the size of the parent element." u/em)
(def fr "A relative length unit, \"fraction\", depending on the size of the parent element." u/fr)
(def vw "A relative length unit, \"viewport width\", based on the width of the window." u/vw)
(def vh "A relative length unit, \"viewport height\", based on the height of the window." u/vh)
(def vmin "A relative length unit, minimum of vw and vh." u/vmin)
(def vmax "A relative length unit, maximum of vw and vh." u/vmax)
(def lh "A relative length unit, equal to the line height." u/lh)

;; time units
(def s "A time unit, \"second\"." u/s)
(def ms "A time unit, \"millisecond\"." u/ms)

;; angular units
(def deg "An angular unit, \"degree\"." u/deg)
(def rad "An angular unit, \"radian\". Equal to 360°/2π" u/rad)
(def grad "An angular unit, \"gradian\". 100 gradians are equal to 90 degrees." u/grad)
(def turn "An angular unit, \"turn\". Represents one whole turn, equal to 360 degrees." u/turn)

;; frequency units

(def Hz "A frequency unit, \"Hertz." u/Hz)
(def kHz "A frequency unit, \"kiloHertz." u/kHz)

;; resolution units
(def dpi "A resolution unit, \"dots per inches\"." u/dpi)
(def dppx "A resolution unit, \"dots per pixels\"." u/dppx)
(def dpcm "A resolution unit, \"dots per centimeter\"." u/dpcm)