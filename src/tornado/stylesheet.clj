(ns tornado.stylesheet
  (:require [tornado.types]
            [tornado.props-vals :as pv])
  (:import (tornado.types CSSUnit CSSFunction CSSAtRule CSSColor)))

(defn at-media [rules value]
  (CSSAtRule. "media" {:rules   rules
                       :changes value}))

(defn at-font-face [props-map]
  (CSSAtRule. "font-face" props-map))

(defn at-keyframes [name anim-class-or-ids props]
  (CSSAtRule. "keyframes" nil))

(defn check-valid
  "Checks, whether the given prop-or-val of a type \"property\"/\"value\" is a known CSS element of the
  given type. Always returns the given prop-or-val, just gives us a warning in the REPL. If the prop-or-val
  is a string or a record, always proceeds without logging anything - only checks for keywords."
  ([string-or-record]
   (check-valid "none" string-or-record))
  ([prop-or-val type]
   (cond (record? prop-or-val) prop-or-val
         (string? prop-or-val) prop-or-val

         (= type "none")
         (do (println (str "Warning: Function tornado.stylesheet/check-valid was called with a single parameter, "
                           "supposing the parameter being a record or a string, but it is neither of them."))
             prop-or-val)

         :else
         (let [check-set (case type "property" pv/css-properties
                                    "value" pv/css-values
                                    (println "Warning: not a valid identifier for a CSS-property or a CSS-value set:" type))]
           (if check-set
             (do (when-not (contains? check-set prop-or-val)
                   (println (str "Warning: Unknown " type ": " prop-or-val)))
                 prop-or-val)
             prop-or-val)))))

(defn validate-map
  "Applies \"check-valid\" on each element of the map. Returns the given map. If the argument
  is not a map, gives us a warning."
  [m]
  (if (map? m)
    (->> (for [[k v] m]
           [(check-valid k "property")
            (check-valid v "value")])
         (into {}))
    m))