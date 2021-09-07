(ns tornado.core
  (:require [tornado.types]
            [tornado.units :as units]
            [tornado.compiler]
            [tornado.colors :as colors]
            [tornado.selectors :as sel]
            [tornado.functions :as functions]
            [tornado.at-rules :as at-rules])
  (:import (tornado.types CSSAtRule CSSFunction CSSUnit CSSSelector
                          CSSPseudoClass CSSPseudoElement CSSColor)))

;; cannot refer macros: units/defunit, functions/defcssfn, defpseudoclass

;; tornado.units
(defmacro defunit [& args]
  `(units/defunit ~@args))
(def px units/px)
(def pt units/pt)
(def pc units/pc)
(def in units/in)
(def cm units/cm)
(def mm units/mm)
(def percent units/percent)
(def rem* units/rem*)
(def em units/em)
(def fr units/fr)
(def vw units/vw)
(def vh units/vh)
(def vmin units/vmin)
(def vmax units/vmax)
(def lh units/lh)
(def s units/s)
(def ms units/ms)
(def deg units/deg)
(def rad units/rad)
(def grad units/grad)
(def turn units/turn)
(def Hz units/Hz)
(def kHz units/kHz)
(def dpi units/dpi)
(def dppx units/dppx)
(def dpcm units/dpcm)


;; tornado.colors
(def rgb colors/rgb)
(def rgba colors/rgba)
(def hsl colors/hsl)
(def hsla colors/hsla)
(def hex->rgba colors/hex->rgba)
(def rgb?a->hex colors/rgb?a->hex)
(def mix-colors colors/mix-colors)


;; tornado.selectors
(defmacro defpseudoclass [& args]
  `(sel/defpseudoclass ~@args))
(defmacro defpseudoelement [& args]
  `(sel/defpseudoelement ~@args))
(def active sel/active)
(def checked sel/checked)
(def default sel/default)
(def disabled sel/disabled)
(def empty* sel/empty*)
(def enabled sel/enabled)
(def first* sel/first*)
(def first-child sel/first-child)
(def first-of-type sel/first-of-type)
(def fullscreen sel/fullscreen)
(def focus sel/focus)
(def hover sel/hover)
(def indeterminate sel/indeterminate)
(def in-range sel/in-range)
(def invalid sel/invalid)
(def last-child sel/last-child)
(def last-of-type sel/last-of-type)
(def left sel/left)
(def links sel/links)
(def only-child sel/only-child)
(def only-of-type sel/only-of-type)
(def optional sel/optional)
(def out-of-range sel/out-of-range)
(def read-only sel/read-only)
(def read-write sel/read-write)
(def required sel/required)
(def right sel/right)
(def root sel/root)
(def scope sel/scope)
(def target sel/target)
(def valid sel/valid)
(def visited sel/visited)
(def after sel/after)
(def before sel/before)
(def first-letter sel/first-letter)
(def first-line sel/first-line)
(def selection sel/selection)


;; tornado.functions
(defmacro defcssfn [& args]
  `(functions/defcssfn ~@args))
(def translate functions/translate)
(def translate3d functions/translate3d)
(def translateX functions/translateX)
(def translateY functions/translateY)
(def translateZ functions/translateZ)
(def scale functions/scale)
(def min* functions/min*)
(def max* functions/max*)
(def rotate functions/rotate)
(def rotateX functions/rotateX)
(def rotateY functions/rotateY)
(def rotateZ functions/rotateZ)
(def rotate3d functions/rotate3d)
(def cubic-bezier functions/cubic-bezier)


;; tornado.at-rules
(defmacro defkeyframes [& args]
  `(at-rules/defkeyframes ~@args))
(def at-media at-rules/at-media)
(def at-font-face at-rules/at-font-face)