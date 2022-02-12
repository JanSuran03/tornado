(ns tornado.test.nesting
  (:require [clojure.test :refer :all]
            [tornado.core :refer :all]))

(defmacro test-all-equal [& xs]
  (let [as-css (mapv #(list css %) xs)]
    `(is (= ~@as-css))))

(deftest map-for-expansion
  (let [both-map-and-for
        [:body {:color :font-black}
         (map #(vector (css-class (str "color-" (name %)))
                       {:color %}
                       (for [width [400 600 800]]
                         (at-media {:min-width (px width)}
                                   [:& {:font-size (em (/ width 700))}])))
              [:red :blue :green])]
        expanded-map
        [:body {:color :font-black}
         [:.color-red {:color :red}
          (for [width [400 600 800]]
            (at-media {:min-width (px width)}
                      [:& {:font-size (em (/ width 700))}]))]
         [:.color-blue {:color :blue}
          (for [width [400 600 800]]
            (at-media {:min-width (px width)}
                      [:& {:font-size (em (/ width 700))}]))]
         [:.color-green {:color :green}
          (for [width [400 600 800]]
            (at-media {:min-width (px width)}
                      [:& {:font-size (em (/ width 700))}]))]]
        expanded-for
        [:body {:color :font-black}
         (map #(vector (css-class (str "color-" (name %)))
                       {:color %}
                       (at-media {:min-width (px 400)}
                                 [:& {:font-size (em 4/7)}])
                       (at-media {:min-width (px 600)}
                                 [:& {:font-size (em 6/7)}])
                       (at-media {:min-width (px 800)}
                                 [:& {:font-size (em 8/7)}]))
              [:red :blue :green])]
        expanded-map-and-for
        [:body {:color :font-black}
         [:.color-red {:color :red}
          (at-media {:min-width (px 400)}
                    [:& {:font-size (em 4/7)}])
          (at-media {:min-width (px 600)}
                    [:& {:font-size (em 6/7)}])
          (at-media {:min-width (px 800)}
                    [:& {:font-size (em 8/7)}])]
         [:.color-blue {:color :blue}
          (at-media {:min-width (px 400)}
                    [:& {:font-size (em 4/7)}])
          (at-media {:min-width (px 600)}
                    [:& {:font-size (em 6/7)}])
          (at-media {:min-width (px 800)}
                    [:& {:font-size (em 8/7)}])]
         [:.color-green {:color :green}
          (at-media {:min-width (px 400)}
                    [:& {:font-size (em 4/7)}])
          (at-media {:min-width (px 600)}
                    [:& {:font-size (em 6/7)}])
          (at-media {:min-width (px 800)}
                    [:& {:font-size (em 8/7)}])]]]
    (test-all-equal both-map-and-for
                    expanded-map
                    expanded-for
                    expanded-map-and-for)))