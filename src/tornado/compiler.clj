(ns tornado.compiler)

(def ^:dynamic
  *flags* {:indent-length 4
           :pretty-print? true
           :output-to     nil})

(def ^:dynamic *media-query-parents*
  nil)

(def ^:dynamic *at-media-indent*
  "")

(def ^:dynamic *keyframes-indent*
  "")

(def ^:dynamic *in-params-context* false)


(defn indent
  "The actual globally used indent in a string form of *X* spaces."
  []
  (apply str (repeat (:indent-length *flags*) " ")))

(defmacro with-custom-flags
  "Given custom-flags & body, temporarily merges default *flags* with the given flags
  and executes the body."
  [flags & body]
  `(binding [*flags* (merge *flags* ~flags)]
     ~@body))

(defmacro with-media-query-parents
  "Temporarily stores current parents paths for compiling @media and adds
 + 1* globally used indent."
  [-parents- & body]
  `(binding [*media-query-parents* ~-parents-
             *at-media-indent* ~(indent)]
     ~@body))

(defmacro in-keyframes-context
  "Temporarily adds extra + 1* globally used indent for compiling @keyframes."
  [& body]
  `(binding [*keyframes-indent* ~(indent)]
     ~@body))

(defmacro in-params-context
  "A macro to bind *in-params-context* to true, which causes a css at-rule keyframes
  record to be compiled to {:anim-name} (assuming it is for :animation-name)"
  [& body]
  `(binding [*in-params-context* true]
     ~@body))