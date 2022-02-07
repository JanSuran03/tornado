(ns tornado.macros
  (:require [tornado.types])
  (:import (tornado.types CSSUnit CSSAttributeSelector CSSPseudoClass CSSPseudoClassFn
                          CSSFunction CSSCombinator CSSPseudoElement CSSAtRule)
           (clojure.lang PersistentList IFn)))

(defmacro defkeyframes
  "Defines a CSS @keyframes animation. The animation name should have a unique symbol
  for later reference to it and then animation frames in a format [progress params]:

  (defkeyframes fade-in-opacity
                [(u/percent 0) {:opacity 0}]
                [(u/percent 25) {:opacity 0.1}]
                [(u/percent 50) {:opacity 0.25}]
                [(u/percent 75) {:opacity 0.5}]
                [(u/percent 100) {:opacity 1}])

  Then, refer it the CSS hiccup list to make tornado compile it for later usage:

  (def styles
     (list
        fade-in-opacity
        ...))

  After that, you can assign this animation to whatever element you want:

  (def styles
     (list
        fade-in-opacity
        [:.some-element {:animation-duration (u/ms 500)
                         :animation-name     fade-in-opacity)}]
        [:#another-element {:animation-name  fade-in-opacity
                            :animation-delay (u/s 1.5)}]))

  With defkeyframes you can also define from & to progress animations:

  (defkeyframes translate-animation
                [:from {:transform (f/translate (u/px 100) (u/px 200)}]
                [:to {:transform (f/translate (u/px 200) (u/px 400)}])"
  [animation-name & frames]
  `(def ~animation-name (CSSAtRule. "keyframes" {:anim-name (str '~animation-name)
                                                 :frames    (list ~@frames)})))

(defmacro defunit
  "Creates a unit function which takes 1 argument and creates a CSSUnit record for future
   compilation. Defunit can take 1 arg: (defunit px)
                             or 2 args: (defunit percent \"%\").

   Usage of the defined units: (px 15)       ...  compiles to \"15px\"
                               (percent 33)  ...  compiles to \"33%\"

   CSSUnits can be added, subtracted, multiplied or divided by using function calc (you can
   also use these 4 keywords - they are defined just for better search in code:
   :add, :sub, :mul, :div

   E.g. (calc (px 500) :add 3 :mul (vw 5)) ... \"calc(500px + 3 * 5vw)\"."
  ([unit]
   `(defunit ~unit ~(str unit)))
  ([identifier css-unit]
   `(defn ~identifier [value#] (CSSUnit. ~css-unit value#))))

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
  `(do (defn ~selector-name
         ([attr# subval#] (CSSAttributeSelector. ~compiles-to nil attr# subval#))
         ([tag# attr# subval#] (CSSAttributeSelector. ~compiles-to tag# attr# subval#)))
       (alter-meta! #'~selector-name assoc :arglists '([~'attribute ~'subvalue]
                                                       [~'tag ~'attribute ~'subvalue]))
       #'~selector-name))

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
   `(defpseudoclass ~pseudoclass ~(str pseudoclass)))
  ([identifier css-pseudoclass]
   `(def ~identifier (CSSPseudoClass. ~css-pseudoclass))))

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
   `(defpseudoclassfn ~pseudoclass ~(str pseudoclass)))
  ([pseudoclass compiles-to]
   `(defn ~pseudoclass [arg#] (CSSPseudoClassFn. ~compiles-to arg#))))

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
   `(def ~pseudoelement (CSSPseudoElement. ~(str pseudoelement)))))

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
  `(defn ~selector-name [& children#] (CSSCombinator. ~compiles-to children#)))

(defmacro defcssfn
  "Defines a CSS function. In most cases, you do NOT need to define a special compile-fn
  function - it should always be enough to use one of single-arg, space-join, comma-join.
  All of them compile the params, but: Single-arg gives you a warning if you give it more
  than 1 argument and compiles the args like comma-join. Comma-join compiles all its args
  and str/joins them with a comma. Space-join compiles all its args and str/joins them
  with a space. All these function also take the compiles-to argument and put it in front
  of a bracket enclosing the str/joined arguments.
  You can give this function 1, 2 or 3 arguments:

  (defcssfn translate)   (the default compile-fn is comma-join)
  (translate (u/px 80) (u/css-rem 6))   ... compiles to    \"translate(80px, 6rem)\"

  (defcssfn css-min \"min\")
  (css-min (u/px 500) (u/vw 40) (u/cm 20))   ... compiles to   \"min(500px, 40vw, 20cm)\"

  (defcssfn calc space-join)
  (calc (u/px 200) add 3 mul (u/percent 20))   ... compiles to   \"calc(200px + 3 * 20%)\"

  The arity(3) can be used like this to combine both previous features of the arity(2):
  (defcssfn my-clj-fn \"css-fn\" space-join)
  (my-clj-fn (u/s 20) (u/ms 500))   ... compiles to   \"css-fn(20s 500ms)\""
  ([fn-name]
   `(defcssfn ~fn-name ~(str fn-name) nil))
  ([fn-name css-fn-or-fn-tail]
   (condp instance? css-fn-or-fn-tail String `(defcssfn ~fn-name ~css-fn-or-fn-tail nil)
                                      PersistentList `(defcssfn ~fn-name ~(str fn-name) ~css-fn-or-fn-tail)
                                      IFn `(defcssfn ~fn-name ~(str fn-name) ~css-fn-or-fn-tail)
                                      (throw (IllegalArgumentException.
                                               (str "Error defining a CSS function " fn-name " with arity(2):"
                                                    "\nThe second argument " css-fn-or-fn-tail " is"
                                                    " neither a string nor a function.")))))
  ([clojure-fn-name compiles-to compile-fn]
   `(defn ~clojure-fn-name [& args#] (CSSFunction. ~compiles-to ~compile-fn args#))))

(defmacro cartesian-product
  "Given any number of seqs, this function returns a lazy sequence of all possible
  combinations of taking 1 element from each of the input sequences."
  [& seqs]
  (let [with-bindings (map #(vector (gensym) %) seqs)
        binding-symbols (mapv first with-bindings)
        for-bindings (vec (apply concat with-bindings))]
    `(for ~for-bindings ~binding-symbols)))