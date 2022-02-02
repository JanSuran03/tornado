(ns tornado.test.selectors
  (:require [clojure.test :refer :all]
            [tornado.core :refer :all]
            [tornado.compiler :as c]))

(def c-selseq c/compile-selectors-sequence)

(defmacro try-test-sel-seq [as-code as-str]
  `(is (= ~as-code ~as-str)
       ~(str "Failed to test selectors sequences:\n\n" as-code "\n\"" as-str "\"")))

(deftest selectors-sequences
  (try-test-sel-seq (c-selseq [:a :.class-x :#id-y])
                    "a .class-x #id-y")
  (try-test-sel-seq (c/compile-selectors [[:p :div :#id-x] [:div :p :#id-y] [:.some-class :div :tr :td]])
                    "p div #id-x, div p #id-y, .some-class div tr td"))

(deftest selectors-nesting
  (are [x y] (= (css x) (css y))
             [:.a :#b
              [:.c :#d {:a :b}]]
             (list [:.a [:.c {:a :b}]]
                   [:.a [:#d {:a :b}]]
                   [:#b [:.c {:a :b}]]
                   [:#b [:#d {:a :b}]])

             [:p :h1 {:a :b}
              [:a {:a :c}]
              [:.class-1 {:d :e}
               [:#id-1 :#id-2 {:m :n}]]]
             (list [:p {:a :b}]
                   [:p [:a {:a :c}]]
                   [:p [:.class-1 {:d :e}]]
                   [:p [:.class-1 [:#id-1 {:m :n}]]]
                   [:p [:.class-1 [:#id-2 {:m :n}]]]
                   [:h1 {:a :b}]
                   [:h1 [:a {:a :c}]]
                   [:h1 [:.class-1 {:d :e}]]
                   [:h1 [:.class-1 [:#id-1 {:m :n}]]]
                   [:h1 [:.class-1 [:#id-2 {:m :n}]]])))

(deftest pseudoclassselectors
  (is (= (css [:a {:color :black} [hover {:color :red}]])
         "a {\n    color: #000000;\n}\n\na:hover {\n    color: #FF0000;\n}")
      "Failed to test pseudoclass selectors."))

(deftest psedoelementselectors
  (is (= (css [:div [first-letter {:font-size (css-rem 1.5)}]])
         "div::first-letter {\n    font-size: 1.5rem;\n}")
      "Failed to test pseudoelement selectors."))

(deftest attributeselectors
  (are [x y] (= (c-selseq x) y)
             [:a (has-attr :div) (has-attr :a :target)]
             "a [div] a[target]"

             [:div (contains-subs :id :ock) :tr (contains-word :td :class :index)]
             "div [id*=\"ock\"] tr td[class~=\"index\"]"))

(deftest pseudoclassfn
  (are [x y] (= x y)
             (c-selseq [:html (lang "en")])
             "html:lang(en)"

             (css [:.table-1 [(nth-child :odd) {:justify-self :right}]])
             ".table-1:nth-child(odd) {\n    justify-self: right;\n}"

             (c-selseq [:div (general-sibling :p :.class-1 :#id-1)])
             "div ~ p ~ .class-1 ~ #id-1"))