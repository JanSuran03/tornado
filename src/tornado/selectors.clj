(ns tornado.selectors
  ""
  (:require [tornado.types]
            [tornado.util :as util])
  (:import (tornado.types CSSSelector CSSPseudoClass CSSPseudoElement)))

(def selector-keys->selectors
  {:child        " "
   :direct-child ">"})

(defn selector? [x]
  (instance? CSSSelector x))

(defn make-pseudoclass-fn
  ""
  [pseudoclass]
  (fn [element]
    (CSSPseudoClass. pseudoclass element)))

(defmacro defpseudoclass
  ([pseudoclass]
   (let [compiles-to (str pseudoclass)]
     `(defpseudoclass ~pseudoclass ~compiles-to)))
  ([identifier css-pseudoclass]
   `(def ~identifier (make-pseudoclass-fn ~css-pseudoclass))))

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
  (set (map str #{:a
                  :abbr
                  :address
                  :area
                  :article
                  :aside
                  :audio
                  :b
                  :base
                  :bdi
                  :bdo
                  :blockquote
                  :body
                  :br
                  :button
                  :canvas
                  :caption
                  :cite
                  :code
                  :col
                  :colgroup
                  :command
                  :datalist
                  :dd
                  :del
                  :details
                  :dfn
                  :div
                  :dl
                  :dt
                  :em
                  :embed
                  :fieldset
                  :figcaption
                  :figure
                  :footer
                  :form
                  :h1
                  :h2
                  :h3
                  :h4
                  :h5
                  :h6
                  :head
                  :header
                  :hgroup
                  :hr
                  :html
                  :i
                  :iframe
                  :img
                  :input
                  :ins
                  :kbd
                  :keygen
                  :label
                  :legend
                  :li
                  :link
                  :map
                  :mark
                  :math
                  :menu
                  :meta
                  :meter
                  :nav
                  :noscript
                  :object
                  :ol
                  :optgroup
                  :option
                  :output
                  :p
                  :param
                  :pre
                  :progress
                  :q
                  :rp
                  :rt
                  :ruby
                  :s
                  :samp
                  :script
                  :section
                  :select
                  :small
                  :source
                  :span
                  :strong
                  :style
                  :sub
                  :summary
                  :sup
                  :svg
                  :table
                  :tbody
                  :td
                  :textarea
                  :tfoot
                  :th
                  :thead
                  :time
                  :title
                  :tr
                  :track
                  :u
                  :ul
                  :var
                  :video
                  :wbr})))

(defn css-id? [x]
  (and (util/valid? x)
       (-> x util/get-valid (subs 1) (= \#))))

(defn css-class? [x]
  (and (util/valid? x)
       (-> x util/get-valid (subs 1) (= \.))))

(defn html-tag? [x]
  (and (util/valid? x)
       (->> x util/get-valid (contains? html-tags))))

(defn make-pseudoelement-fn
  ""
  [pseudoelement]
  (fn [element]
    (CSSPseudoElement. pseudoelement element)))

(defmacro defpseudoelement
  ""
  ([pseudoelement]
   (let [compiles-to (str pseudoelement)]
     `(defpseudoelement ~pseudoelement ~compiles-to)))
  ([identifier css-pseudoelement]
   `(def ~identifier (make-pseudoelement-fn ~css-pseudoelement))))

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