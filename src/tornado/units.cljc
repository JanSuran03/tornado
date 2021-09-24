(ns tornado.units
  "A namespace for defining CSS units."
  (:require [tornado.types :as t]
            #?(:clj [tornado.clj-macros :refer [defunit]]))
  #?(:cljs (:require-macros [tornado.units :refer [defunit]])))

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