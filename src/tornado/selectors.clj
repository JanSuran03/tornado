(ns tornado.selectors
  (:require [tornado.types]
            [tornado.util :as util])
  (:import (tornado.types CSSAttributeSelector CSSPseudoClass CSSCombinator
                          CSSPseudoClassFn CSSPseudoElement)))

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

(defmacro has-attr
  ([attr] (CSSAttributeSelector. nil nil attr nil))
  ([tag attr] (CSSAttributeSelector. nil tag attr nil)))