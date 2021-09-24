(ns tornado.functions
  "Everything related to CSS functions."
  (:require [tornado.types]
            [tornado.util :as util]
            [tornado.compiler :refer [compile-expression]]
            #?(:clj [tornado.macros :refer [defcssfn]]))
  #?(:cljs (:require-macros [tornado.macros :refer [defcssfn]])))

(defn comma-join
  "A CSSFunction util/str-commajoin compile function. Compiles the
  function to a form <fn-name>(arg1, arg2, arg3, ...),"
  [{:keys [compiles-to args]}]
  (str compiles-to "(" (->> args (map compile-expression)
                            util/str-commajoin) ")"))

(defn space-join
  "A CSSFunction util/str-spacejoin compile function. Compiles the
  function to a form <fn-name>(arg1 arg2 arg3 ...),"
  [{:keys [compiles-to args]}]
  (str compiles-to "(" (->> args (map compile-expression)
                            util/str-spacejoin) ")"))

(defn single-arg
  "A CSSFunction compile function. Presumes that only one arg is given.
  If not, calls commajoin function above and gives us a warning instead."
  [{:keys [compiles-to args] :as cssfn}]
  (if (= (count args) 1)
    (str compiles-to "(" (-> args first compile-expression) ")")
    (do (println (str "Warning: A CSSFunction \"" compiles-to "\" expects to have"
                      " 1 argument, instead got arguments: " args))
        (comma-join cssfn))))

(defn css-format-fn
  "A special function for css-format which also puts additional quotes around
  the parameter to enable usage of keywords."
  [{:keys [args] :as cssfn}]
  (if (= (count args) 1)
    (str "format(\"" (-> args first identity #_compile-expression) "\")")
    (do (println (str "Warning: A CSSFunction \"css-format\" expects to have"
                      " 1 argument, instead got arguments: " args))
        (comma-join cssfn))))

; https://www.quackit.com/css/functions/

;; single arg functions

(defcssfn blur single-arg)
(defcssfn brightness single-arg)
(defcssfn contrast single-arg)
(defcssfn css-format "format" css-format-fn)
(defcssfn grayscale single-arg)
(defcssfn hue-rotate single-arg)
(defcssfn invert single-arg)
(defcssfn perspective single-arg)
(defcssfn rotate single-arg)
(defcssfn rotateX single-arg)
(defcssfn rotateY single-arg)
(defcssfn rotateZ single-arg)
(defcssfn sepia single-arg)
(defcssfn skewX single-arg)
(defcssfn skewY single-arg)
(defcssfn scaleX single-arg)
(defcssfn scaleY single-arg)
(defcssfn scaleZ single-arg)
(defcssfn translateX single-arg)
(defcssfn translateY single-arg)
(defcssfn translateZ single-arg)

;; comma-join functions

(defcssfn attr comma-join)
(defcssfn counter comma-join)
(defcssfn counters comma-join)
(defcssfn cubic-bezier comma-join)
(defcssfn css-filter "filter" comma-join)
(defcssfn hwb comma-join)
(defcssfn linear-gradient comma-join)
(defcssfn matrix comma-join)
(defcssfn matrix3d comma-join)
(defcssfn css-max "max" comma-join)
(defcssfn css-min "min" comma-join)
(defcssfn polygon comma-join)
(defcssfn radial-gradient comma-join)
(defcssfn repeating-linear-gradient comma-join)
(defcssfn repeating-radial-gradient comma-join)
(defcssfn rotate3d comma-join)
(defcssfn scale comma-join)
(defcssfn scale3d comma-join)
(defcssfn skew comma-join)
(defcssfn translate comma-join)
(defcssfn translate3d comma-join)
(defcssfn url comma-join)
(defcssfn css-var "var" comma-join)

;; space-join functions

(defcssfn calc space-join)
(defcssfn circle space-join)
(defcssfn drop-shadow space-join)
(defcssfn ellipse space-join)
(defcssfn image space-join)
(defcssfn inset space-join)
(defcssfn symbols space-join)