(ns tornado.selectors
  "Everything related to CSS selectors - standard selectors, attribute selectors,
   pseudoelement selectors, functions for the compiler etc."
  (:require [tornado.types]
            [tornado.util :as util]
            [clojure.string :as str])
  (:import (tornado.types CSSPseudoClass CSSPseudoElement
                          CSSAttributeSelector CSSCombinator CSSPseudoClassFn)))

;; Lists of special selectors can be found on https://www.w3schools.com/css/css_selectors.asp
;; On https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Selectors, you can find even more of them
;; I did not include all of them because I do not think they are all needed, but if you would like me
;; to include more of them, you can contact me via e-mail: see https://github.com/JanSuran03/tornado/#contact

(defn attribute-selector-fn
  "Creates a CSSAttributeSelector record."
  ([compiles-to attribute subvalue]
   (CSSAttributeSelector. compiles-to nil attribute subvalue))
  ([compiles-to tag attribute subvalue]
   (CSSAttributeSelector. compiles-to tag attribute subvalue)))

(defmacro defattributeselector
  "Attribute selectors select all descendant elements containing a given attribute,
  of which the value matches a given substring. All attribute selectors have
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
  `(do (def ~selector-name (partial ~attribute-selector-fn ~compiles-to))
       (alter-meta! #'~selector-name assoc :arglists '([~'attribute ~'subvalue]
                                                       [~'tag ~'attribute ~'subvalue]))))

(defn has-attr
  "An attribute selector which selects all elements which have a given
  attribute with any value, or all html elements on/below the current
  nested selectors level which have a given attribute with any value."
  ([attribute] (CSSAttributeSelector. nil nil attribute nil))
  ([tag attribute] (CSSAttributeSelector. nil tag attribute nil)))

(defattributeselector has-val "=")
(defattributeselector contains-word "~=")
(defattributeselector starts-with-word "|=")
(defattributeselector starts-with "^=")
(defattributeselector ends-with "$=")
(defattributeselector contains-subs "*=")

(defmacro defpseudoclass
  "Defines a CSS pseudoclass. A CSS pseudoclass can activate some CSS properties on
  a css-class/css-id/html-element based on some current special state of the element.

  For example, hover: (defpseudoclass hover)
  When compiling a selectors sequence, e.g. [:.abc :#def hover], the resulting CSS
  selectors sequence will look like this: \".abc #def:hover\".

  So, what does it even do? We can give the element a special value on hover:
  ... [:a hover {:color :blue} ...] - when we hover over a link with our mouse, the
  text color of the link will turn blue until we put our mouse away.

  Defpseudoclass can also take 2 parameters, where the 2nd one will be the translation
  to CSS to avoid collisions between Clojure and CSS -
  e.g.(defpseudolass css-empty \"empty\")."
  ([pseudoclass]
   (let [compiles-to (str pseudoclass)]
     `(defpseudoclass ~pseudoclass ~compiles-to)))
  ([identifier css-pseudoclass]
   `(def ~identifier (CSSPseudoClass. ~css-pseudoclass))))

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

(defn create-pseudoclassfn-record
  "Given a CSS pseudoclass for compilation and an argument, creates a CSSPseudoclassFn
  record with the pseudoclass and argument."
  [pseudoclass argument]
  (CSSPseudoClassFn. pseudoclass argument))

(defmacro defpseudoclassfn
  "Creates a special CSS pseudoclass function, which compiles similarly as a standard
  CSS pseudoclass, but it is pseudoclass function with an argument.

  For example. if you wanted to only select every n-th argument:
  (defpseudoclassfn nth-child)
  (nth-child :odd)     ... compiles to   \"<parent>:nth-child(odd)\"
  (nth-child \"3n+1\")   ... compiles to   \"<parent>:nth-child(3n+1)\"

  Or if you wanted to show something based on the current language of the browser:
  (defpseudoclass lang)
  (lang \"en\") ... compiles to   \"<parent>:lang(en)\"

  To avoid collisions with some Clojure functions, you can give a second argument
  to defpseudoclassfn for a different translation to CSS:
  (defpseudoclass css-not \"not\")
  (css-not :p) ... compiles-to   \"not(p)\", which selects all descendants which are
  not a paragraph."
  ([pseudoclass]
   (let [compiles-to (str pseudoclass)]
     `(defpseudoclassfn ~pseudoclass ~compiles-to)))
  ([pseudoclass compiles-to]
   `(def ~pseudoclass (partial ~create-pseudoclassfn-record ~compiles-to))))

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
  (or (util/some-instance? x CSSAttributeSelector CSSCombinator
                           CSSPseudoClass CSSPseudoClassFn CSSPseudoElement)
      (and (util/valid? x)
           (contains? special-selectors (name x)))))

(defn css-class?
  "Returns true if the argument is a keyword, a string or a symbol
  and (name argument) starts with \".\"."
  [x]
  (and (-> x util/get-str-form (or "") (str/starts-with? "."))
       (> (count (name x)) 1)))

(defn css-id?
  "Returns true if the argument is a keyword, a string or a symbol
  and (name argument) starts with \"#\"."
  [x]
  (and (-> x util/get-str-form (or "") (str/starts-with? "#"))
       (> (count (name x)) 1)))

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

(defmacro defpseudoelement
  "Defines a CSS pseudoelement. A CSS pseudoelement activates some CSS properties on
  a special part of a css-class/css-id/html-element.

  For example, first-letter: (defpseudoclass first-letter)
  When compiling a selectors sequence, e.g. [:.abc :#def first-letter], the resulting CSS
  selectors sequence will look like this: \".abc #def::first-letter\".

  So, what does it even do? We can give the first letter of an element a special value:
  ... [:.abc :p first-letter {:font-size (u/px 60)} ...] - this causes the first letter
  of every paragraph in an element with class .abc to have the first letter significantly
  bigger than the rest of the paragraph."
  ([pseudoelement]
   (let [compiles-to (str pseudoelement)]
     `(def ~pseudoelement (CSSPseudoElement. ~compiles-to)))))

(defpseudoelement after)
(defpseudoelement before)
(defpseudoelement first-letter)
(defpseudoelement first-line)
(defpseudoelement marker)
(defpseudoelement selection)

(defn make-combinator-fn
  "Creates a CSSCombinator record."
  [compiles-to & children]
  (CSSCombinator. compiles-to children))

(defmacro defcombinatorselector
  "Defines a combinator selector function which describes relationships between its
  arguments depending on the selector type:

  :#abc :.def is the default combinator selector - descendant selector. Affects all
  children with a class .def.

  child-selector \">\": is active when the given selectors are every of them a direct
  child of the previous one.

  adjacent-sibling (selector) \"+\": is active when the given html blocks elements or
  elements with a given class/id connected with the \"+\" sign are adjacent siblings.

  general-sibling (selector) \"~\" is active when the given selectors are on the same
  level of nesting; they do not have to be adjacent necessarily.

  Usage: [:.abc
           [:.def (child-selector :p :#ghi)]]
  compiles to   \".abc .def, .abc > p > #ghi\""
  [selector-name compiles-to]
  `(def ~selector-name (partial ~make-combinator-fn ~compiles-to)))

(defcombinatorselector child-selector ">")
(defcombinatorselector adjacent-sibling "+")
(defcombinatorselector general-sibling "~")