(ns tornado.selectors
  ""
  (:require [tornado.types]
            [tornado.util :as util]
            [clojure.string :as str])
  (:import (tornado.types CSSSelector CSSPseudoClass CSSPseudoElement)))

(def selector-keys->selectors
  {:child        " "
   :direct-child ">"})

(defn make-pseudoclass-fn
  ""
  [pseudoclass parent]
  (CSSPseudoClass. pseudoclass parent))

(defmacro defpseudoclass
  ([pseudoclass]
   (let [compiles-to (str pseudoclass)]
     `(defpseudoclass ~pseudoclass ~compiles-to)))
  ([identifier css-pseudoclass]
   `(def ~identifier (partial ~make-pseudoclass-fn ~css-pseudoclass))))

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

(defn selector? [x]
  (instance? CSSSelector x))

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

(defn make-pseudoelement-fn
  ""
  [pseudoelement parent]
  (CSSPseudoElement. pseudoelement parent))

(defmacro defpseudoelement
  ""
  ([pseudoelement]
   (let [compiles-to (str pseudoelement)]
     `(defpseudoelement ~pseudoelement ~compiles-to)))
  ([identifier css-pseudoelement]
   `(def ~identifier (partial ~make-pseudoelement-fn ~css-pseudoelement))))

(defpseudoelement after)
(defpseudoelement before)
(defpseudoelement first-letter)
(defpseudoelement first-line)
(defpseudoelement selection)

(defn child-selector
  ""
  [el1 el2]
  (CSSSelector. :child (list el1 el2)))

(defn direct-child-selector [])

(def +> child-selector)
(def >> direct-child-selector)