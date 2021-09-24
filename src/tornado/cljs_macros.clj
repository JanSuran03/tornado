(ns tornado.cljs-macros
  (:require [tornado.types]
            [tornado.util :as util])
  (:import (tornado.types CSSAttributeSelector CSSCombinator CSSPseudoElement CSSPseudoClassFn CSSUnit CSSPseudoClass CSSFunction CSSAtRule)))

(defmacro defkeyframes
  [animation-name & frames]
  `(def ~animation-name (CSSAtRule. "keyframes" {:anim-name (str '~animation-name)
                                                 :frames    (list ~@frames)})))

(defmacro defcssfn
  ([fn-name]
   (let [compiles-to (str fn-name)]
     `(defcssfn ~fn-name ~compiles-to nil)))
  ([fn-name css-fn-or-fn-tail]
   (cond (string? css-fn-or-fn-tail) `(defcssfn ~fn-name ~css-fn-or-fn-tail nil)
         (list? css-fn-or-fn-tail) (let [compiles-to (str fn-name)]
                                     `(defcssfn ~fn-name ~compiles-to ~css-fn-or-fn-tail))
         (ifn? css-fn-or-fn-tail) (let [compiles-to (str fn-name)]
                                    `(defcssfn ~fn-name ~compiles-to ~css-fn-or-fn-tail))
         :else (util/exception
                 (str "Error defining a CSS function " fn-name " with arity(2):"
                      "\nThe second argument " css-fn-or-fn-tail " is"
                      " neither a string nor a function."))))
  ([clojure-fn-name compiles-to compile-fn]
   `(def ~clojure-fn-name (fn [& args#] (CSSFunction. ~compiles-to ~compile-fn args#)))))

(defmacro defattributeselector
  [selector-name compiles-to]
  `(do (def ~selector-name (fn
                             ([attr# subval#] (CSSAttributeSelector. ~compiles-to nil attr# subval#))
                             ([tag# attr# subval#] (CSSAttributeSelector. ~compiles-to tag# attr# subval#))))
       (alter-meta! #'~selector-name assoc :arglists '([~'attribute ~'subvalue]
                                                       [~'tag ~'attribute ~'subvalue]))))

(defmacro defpseudoclass
  ([pseudoclass]
   (let [compiles-to (str pseudoclass)]
     `(defpseudoclass ~pseudoclass ~compiles-to)))
  ([identifier css-pseudoclass]
   `(def ~identifier (CSSPseudoClass. ~css-pseudoclass))))

(defmacro defpseudoclassfn
  ([pseudoclass]
   (let [compiles-to (str pseudoclass)]
     `(defpseudoclassfn ~pseudoclass ~compiles-to)))
  ([pseudoclass compiles-to]
   `(def ~pseudoclass (fn [arg#] (CSSPseudoClassFn. ~compiles-to arg#)))))

(defmacro defpseudoelement
  ([pseudoelement]
   (let [compiles-to (str pseudoelement)]
     `(def ~pseudoelement (CSSPseudoElement. ~compiles-to)))))

(defmacro defcombinatorselector
  [selector-name compiles-to]
  `(def ~selector-name (fn [& children#] (CSSCombinator. ~compiles-to children#))))

(defmacro has-attr*
  ([attr] (CSSAttributeSelector. nil nil attr nil))
  ([tag attr] (CSSAttributeSelector. nil tag attr nil)))

(defmacro defunit
  ([unit]
   (let [compiles-to (str unit)]
     `(defunit ~unit ~compiles-to)))
  ([identifier css-unit]
   `(def ~identifier (fn [value#] (CSSUnit. ~css-unit value#)))))
