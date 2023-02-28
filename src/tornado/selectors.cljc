(ns tornado.selectors
  "Everything related to CSS selectors - standard selectors, attribute selectors,
   pseudoelement selectors, functions for the compiler etc."
  (:require [tornado.types :as t]
            [tornado.util :as util]
            [clojure.string :as str]
            #?(:clj [tornado.macros :refer [defattributeselector defpseudoclass defpseudoclassfn
                                            defpseudoelement defcombinatorselector]]))
  #?(:clj  (:import (tornado.types CSSPseudoClass CSSPseudoElement
                                   CSSAttributeSelector CSSCombinator CSSPseudoClassFn))
     :cljs (:require-macros [tornado.macros :refer [defattributeselector defpseudoclass defpseudoclassfn
                                                    defpseudoelement defcombinatorselector]])))

;; Lists of special selectors can be found on https://www.w3schools.com/css/css_selectors.asp
;; On https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors, you can find even more of them
;; I did not include all of them because I do not think they are all needed, but if you would like me
;; to include more of them, you can contact me via e-mail: see https://github.com/JanSuran03/tornado/#contact

(defn cssattributeselector [compiles-to tag attr subval]
  #?(:clj  (CSSAttributeSelector. compiles-to tag attr subval)
     :cljs (t/CSSAttributeSelector. compiles-to tag attr subval)))

(defn has-attr
  "An attribute selector which selects all elements which have a given
  attribute with any value, or all html elements on/below the current
  nested selectors level which have a given attribute with any value."
  ([attribute] (cssattributeselector nil nil attribute nil))
  ([tag attribute] (cssattributeselector nil tag attribute nil)))

(defattributeselector has-val "=")
(defattributeselector contains-word "~=")
(defattributeselector starts-with-word "|=")
(defattributeselector starts-with "^=")
(defattributeselector ends-with "$=")
(defattributeselector contains-subs "*=")

(defpseudoclass active)
(defpseudoclass checked)
(defpseudoclass default)
(defpseudoclass disabled)
(defpseudoclass css-empty "empty")
(defpseudoclass enabled)
(defpseudoclass css-first "first")
(defpseudoclass first-child)
(defpseudoclass first-of-type)
(defpseudoclass fullscreen)
(defpseudoclass focus)
(defpseudoclass hover)
(defpseudoclass indeterminate)
(defpseudoclass in-range)
(defpseudoclass invalid)
(defpseudoclass last-child)
(defpseudoclass last-of-type)
(defpseudoclass links)
(defpseudoclass only-child)
(defpseudoclass only-of-type)
(defpseudoclass optional)
(defpseudoclass out-of-range)
(defpseudoclass read-only)
(defpseudoclass read-write)
(defpseudoclass required)
(defpseudoclass root)
(defpseudoclass scope)
(defpseudoclass target)
(defpseudoclass valid)
(defpseudoclass visited)

(defpseudoclassfn lang)
(defpseudoclassfn css-not "not")
(defpseudoclassfn nth-child)
(defpseudoclassfn nth-last-child)
(defpseudoclassfn nth-last-of-type)
(defpseudoclassfn nth-of-type)

(def html-tags
  (set (map name #{:html-tag/a
                   :html-tag/abbr
                   :html-tag/address
                   :html-tag/area
                   :html-tag/article
                   :html-tag/aside
                   :html-tag/audio
                   :html-tag/b
                   :html-tag/base
                   :html-tag/bdi
                   :html-tag/bdo
                   :html-tag/blockquote
                   :html-tag/body
                   :html-tag/br
                   :html-tag/button
                   :html-tag/canvas
                   :html-tag/caption
                   :html-tag/cite
                   :html-tag/code
                   :html-tag/col
                   :html-tag/colgroup
                   :html-tag/command
                   :html-tag/datalist
                   :html-tag/dd
                   :html-tag/del
                   :html-tag/details
                   :html-tag/dfn
                   :html-tag/div
                   :html-tag/dl
                   :html-tag/dt
                   :html-tag/em
                   :html-tag/embed
                   :html-tag/fieldset
                   :html-tag/figcaption
                   :html-tag/figure
                   :html-tag/footer
                   :html-tag/form
                   :html-tag/h1
                   :html-tag/h2
                   :html-tag/h3
                   :html-tag/h4
                   :html-tag/h5
                   :html-tag/h6
                   :html-tag/head
                   :html-tag/header
                   :html-tag/hgroup
                   :html-tag/hr
                   :html-tag/html
                   :html-tag/i
                   :html-tag/iframe
                   :html-tag/img
                   :html-tag/input
                   :html-tag/ins
                   :html-tag/kbd
                   :html-tag/keygen
                   :html-tag/label
                   :html-tag/legend
                   :html-tag/li
                   :html-tag/link
                   :html-tag/map
                   :html-tag/mark
                   :html-tag/math
                   :html-tag/menu
                   :html-tag/meta
                   :html-tag/meter
                   :html-tag/nav
                   :html-tag/noscript
                   :html-tag/object
                   :html-tag/ol
                   :html-tag/optgroup
                   :html-tag/option
                   :html-tag/output
                   :html-tag/p
                   :html-tag/param
                   :html-tag/pre
                   :html-tag/progress
                   :html-tag/q
                   :html-tag/rp
                   :html-tag/rt
                   :html-tag/ruby
                   :html-tag/s
                   :html-tag/samp
                   :html-tag/script
                   :html-tag/section
                   :html-tag/select
                   :html-tag/small
                   :html-tag/source
                   :html-tag/span
                   :html-tag/strong
                   :html-tag/style
                   :html-tag/sub
                   :html-tag/summary
                   :html-tag/sup
                   :html-tag/svg
                   :html-tag/table
                   :html-tag/tbody
                   :html-tag/td
                   :html-tag/textarea
                   :html-tag/tfoot
                   :html-tag/th
                   :html-tag/thead
                   :html-tag/time
                   :html-tag/title
                   :html-tag/tr
                   :html-tag/track
                   :html-tag/u
                   :html-tag/ul
                   :html-tag/var
                   :html-tag/video
                   :html-tag/wbr})))
(def ^:private special-sels {:* "A selector for selecting all descendants."
                             :& "A selector for selecting the current element."})
(def special-selectors (->> special-sels keys (map name) set))

(defn selector?
  "Returns true if x is a selector of any kind (attribute, combinator, pseudoclass,
  pseudoclassfn, pseudoelement, special selector"
  [x]
  (or (util/some-instance? x #?(:clj  CSSAttributeSelector
                                :cljs t/CSSAttributeSelector)
                           #?(:clj  CSSCombinator
                              :cljs t/CSSCombinator)
                           #?(:clj  CSSPseudoClass
                              :cljs t/CSSPseudoClass)
                           #?(:clj  CSSPseudoClassFn
                              :cljs t/CSSPseudoClassFn)
                           #?(:clj  CSSPseudoElement
                              :cljs t/CSSPseudoElement))
      (and (util/named? x)
           (contains? special-selectors (name x)))))

(defn class-or-id-str-form [x]
  (and x
       (util/named? x)
       (name x)))

(defn css-class?
  "Returns true if the argument is a keyword, a string or a symbol
  and (name argument) starts with \".\"."
  [x]
  (when-let [s (class-or-id-str-form x)]
    (and (str/starts-with? s ".")
         (> (count s) 1))))

(defn css-id?
  "Returns true if the argument is a keyword, a string or a symbol
  and (name argument) starts with \"#\"."
  [x]
  (when-let [s (class-or-id-str-form x)]
    (and (str/starts-with? s "#")
         (> (count s) 1))))

(defn html-tag?
  "Returns true if the argument is a keyword, a string or a symbol
  and represents an existing html tag."
  [x]
  (->> x util/get-str-form (contains? html-tags)))

(defn id-class-tag?
  "Returns true if the argument is a keyword, a string or a symbol
  and represents some of a css-class, css-id or an html-tag."
  [x]
  ((some-fn css-class? css-id? html-tag?) x))

(defpseudoelement after)
(defpseudoelement before)
(defpseudoelement first-letter)
(defpseudoelement first-line)
(defpseudoelement marker)
(defpseudoelement selection)

(defcombinatorselector child-selector ">")
(defcombinatorselector adjacent-sibling "+")
(defcombinatorselector general-sibling "~")