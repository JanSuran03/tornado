(ns tornado.context)

(def ^:dynamic *flags*
  "The current flags for a tornado build compilation:

  :indent-length - Specifies, how many indentation spaces should be in the compiled
                   CSS file after any nesting in @rule or params map. Defaults to 4.

  :pretty-print? - Specifies, whether the compiled CSS should be pretty printed.
                   Defaults to true. If set to false, the CSS file will be compressed
                   after compilation (removal of unnecessary characters like spaces
                   and newlines) to make the CSS file a bit smaller.

  :output-to     - Specifies, where the compiled CSS file should be saved."
  {:indent-length 4
   :pretty-print? true
   :output-to     nil})

(def ^:dynamic *indent* (apply str (repeat (:indent-length *flags*) " ")))

(def ^:dynamic *media-query-parents*
  "Current parents Used for compiling @media to temporarily store parents
  paths for compiling @media changes."
  nil)

(def ^:dynamic *at-media-indent*
  "Extra indentation when nested inside a media query."
  "")

(def ^:dynamic *keyframes-indent*
  "Extra indentation when nested inside keyframes."
  "")

(def ^:dynamic *in-params-context* false)

(def ^:dynamic *compress?*
  "Moved from this ns to util in version 0.2.10 to prevent cyclic dependency needed in `ns-kw->str`."
  false)
