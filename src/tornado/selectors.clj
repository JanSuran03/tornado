(ns tornado.selectors
  (:require [tornado.types]
            [tornado.util :as util]
            [clojure.string :as str])
  (:import (tornado.types CSSPseudoClass CSSPseudoElement
                          CSSAttributeSelector CSSCombinator CSSPseudoClassFn)))

(defn make-attribute-selector-fn
  "Creates an attribute selector record."
  ([compiles-to attribute subvalue]
   (CSSAttributeSelector. compiles-to nil attribute subvalue))
  ([compiles-to tag attribute subvalue]
   (CSSAttributeSelector. compiles-to tag attribute subvalue)))

(defmacro defattributeselector
  "Attribute selectors select all descendant elements containing a given attribute,
  of which the value matches with a given substring. All attribute selectors have
  different conditions for matching. Dashes count as words separators.
  By attributes, it is meant html attributes, e.g.:    div[class~=\"info\"]

  Usage:
     (defattributeselector contains-word \"~=\")
     => #'tornado.selectors/contains-word
     (contains-word :direction \"reverse\")
     => #tornado.types.CSSAttributeSelector{:compiles-to \"~=\"
                                            :attribute   :direction
                                            :subvalue    \"reverse\"}


  [attribute=\"value\"]:
     Selects all descendant elements which have a given parameter with a given value.
     In code: <has-val>
  - - - - - - - - - - - -
  With an html tag:
  a[attribute=\"value\"]:
     Selects all descendants of a html tag which have a given parameter
     with a given value.

  [attribute~=\"value\"]:
     Selects all descendant elements which have a given parameter with a value
     containing a given word (not substring, word).
     In code: <contains-word>

  [attribute|=\"value\"]:
     Selects all descendant elements which have a given parameter with a value
     starting with a given word (not substring, word).
     In code: <starts-with-word>

  [attribute^=\"value\"]:
     Selects all descendant elements which have a given parameter with a value
     starting with a given substring (unlike the \"|=\" selector, the substring
     does not have to be a whole word.
     In code: <starts-with>

  [attribute$=\"value\"]:
     Selects all descendant elements which have a given parameter with a value
     ending with a given substring. The substring does not have to be a whole word.
     In code: <ends-with>

  [attribute*=\"value\"]:
     Selects all descendant elements which have a given parameter with a value
     containing a given substring. (unlike the \"~=\" selector, the substring does
     not have to be a whole word).
     In code: <contains-subs>"
  [selector-name compiles-to]
  `(do (def ~selector-name (partial ~make-attribute-selector-fn ~compiles-to))
       (alter-meta! #'~selector-name assoc :arglists '([~'attribute ~'subvalue]
                                                       [~'tag ~'attribute ~'subvalue]))))

(defn has-attr
  "An attribute selector which selects all `tag` elements which "
  [tag attribute])
(defattributeselector has-val "=")
(defattributeselector contains-word "~=")
(defattributeselector starts-with-word "|=")
(defattributeselector starts-with "^=")
(defattributeselector ends-with "$=")
(defattributeselector contains-subs "*=")

(defmacro defpseudoclass
  ([pseudoclass]
   (let [compiles-to (str pseudoclass)]
     `(defpseudoclass ~pseudoclass ~compiles-to)))
  ([identifier css-pseudoclass]
   `(def ~identifier (CSSPseudoClass. ~css-pseudoclass))))

(defpseudoclass active)
(defpseudoclass checked)
(defpseudoclass default)
(defpseudoclass disabled)
(defpseudoclass empty* "empty")
(defpseudoclass enabled)
(defpseudoclass first* "first")
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
(defpseudoclass left)
(defpseudoclass links)
(defpseudoclass only-child)
(defpseudoclass only-of-type)
(defpseudoclass optional)
(defpseudoclass out-of-range)
(defpseudoclass read-only)
(defpseudoclass read-write)
(defpseudoclass required)
(defpseudoclass right)
(defpseudoclass root)
(defpseudoclass scope)
(defpseudoclass target)
(defpseudoclass valid)
(defpseudoclass visited)

(defn make-pseudoclassfn-fn
  ""
  [pseudoclass arg]
  (CSSPseudoClassFn. pseudoclass arg))

(defmacro defpseudoclassfn
  ""
  ([pseudoclass]
   (let [compiles-to (str pseudoclass)]
     `(defpseudoclassfn ~pseudoclass ~compiles-to)))
  ([pseudoclass compiles-to]
   `(def ~pseudoclass (partial ~make-pseudoclassfn-fn ~compiles-to))))

(defpseudoclassfn lang)
(defpseudoclassfn not* "not")
(defpseudoclassfn nth-child)
(defpseudoclassfn nth-last-child)
(defpseudoclassfn nth-last-of-type)
(defpseudoclassfn nth-of-type)
(defpseudoclassfn nth-of-type)
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

(defn selector? [x]
  (or (instance? CSSAttributeSelector x)
      (instance? CSSCombinator x)
      (instance? CSSPseudoClass x)
      (instance? CSSPseudoElement x)
      (instance? CSSPseudoClassFn x)
      (and (util/valid? x)
           (contains? special-selectors (name x)))))

(defn css-class? [x]
  (and (util/valid? x)
       (-> x util/get-valid (str/starts-with? "."))))

(defn css-id? [x]
  (and (util/valid? x)
       (-> x util/get-valid (str/starts-with? "#"))))

(defn html-tag? [x]
  (and (util/valid? x)
       (->> x util/get-valid (contains? html-tags))))

(defn id-class-tag? [x]
  ((some-fn css-class? css-id? html-tag?) x))

(defmacro defpseudoelement
  ""
  ([pseudoelement]
   (let [compiles-to (str pseudoelement)]
     `(defpseudoelement ~pseudoelement ~compiles-to)))
  ([identifier css-pseudoelement]
   `(def ~identifier (CSSPseudoElement. ~css-pseudoelement))))

(defpseudoelement after)
(defpseudoelement before)
(defpseudoelement first-letter)
(defpseudoelement first-line)
(defpseudoelement selection)

(defn make-combinator-fn
  ""
  [compiles-to & children]
  (CSSCombinator. compiles-to children))

(defmacro defcombinatorselector
  ""
  [selector-name compiles-to]
  `(def ~selector-name (partial ~make-combinator-fn ~compiles-to)))

(defcombinatorselector child-selector ">")
(defcombinatorselector adjacent-sibling "+")
(defcombinatorselector general-sibling "~")