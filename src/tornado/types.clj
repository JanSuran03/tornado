(ns tornado.types
  "Internal CSS records used by tornado.")

(defrecord CSSUnit [compiles-to value])

(defrecord CSSFunction [compiles-to compile-fn args])

(defrecord CSSAtRule [identifier value])

(defrecord CSSColor [type value])

(defrecord CSSCombinator [compiles-to element])

(defrecord CSSAttributeSelector [compiles-to tag attribute subvalue])

(defrecord CSSPseudoClass [pseudoclass])

(defrecord CSSPseudoElement [pseudoelement])