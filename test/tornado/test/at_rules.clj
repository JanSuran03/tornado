(ns tornado.test.at-rules
  "It's almost impossible to write exact CSS representations with all the spaces
  so the tests are done with compressed CSS."
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [tornado.core :refer :all]
            [tornado.compression :refer [compress]]))

(deftest at-media*
  (are [x y] (= (compress (css x)) y)
             [:.class-1 {:width (px 500)}
              (at-media {:max-width (px 800)}
                [:& {:width           (px 400)
                     :text-decoration :none}]
                [:#nested-id {:color :red}])]
             ".class-1{width:500px;}@media(max-width:800px){.class-1{width:400px;text-decoration:none;}.class-1 #nested-id{color:#FF0000;}}"
             [:.class-1 {:width (px 500)}
              (at-media {:max-width (px 800)
                         :scren     :only
                         :speech    false}
                [:#nested-id {:color :red}])]
             ".class-1{width:500px;}@media(max-width:800px)and only scren and not speech{.class-1 #nested-id{color:#FF0000;}}"))


(defkeyframes kf1
              [0 {:color (rgb 255 0 0)}]
              [(percent 25) {:color (rgb 200 100 30)}]
              [(percent 60) {:color (rgb 100 200 95)}]
              [(percent 100) {:color (rgb 0 255 128)}])

(deftest at-keyframes*
  (are [x y] (= (compress (css x)) y)
             (list kf1
                   [:p {:some :param}
                    [:#anim-div-1 {:animation-name kf1}]])
             "@keyframes kf1{0{color:rgb(255,0,0);}25%{color:rgb(200,100,30);}60%{color:rgb(100,200,95);}100%{color:rgb(0,255,128);}}p{some:param;}p #anim-div-1{animation-name:kf1;}"))

(deftest at-font-face*
  (are [x y] (= (css x) y)
             (list (at-font-face {:src         [[(url "../webfonts/woff2/roboto.woff2") (css-format :woff2)]
                                                [(url "../webfonts/woff/roboto.woff") (css-format :woff)]]
                                  :font-family "Roboto"})
                   (at-font-face {:src [[(url "../webfonts/woff2/roboto.woff2") (css-format :woff2)]]}
                                 {:src         [[(url "../webfonts/woff/roboto.woff") (css-format :woff)]]
                                  :font-family "Roboto"})
                   [:#some-id
                    [:a {:href "https://clojure.org/"}]])
             (str/join \newline
                       ["@font-face {"
                        "    src: url(../webfonts/woff2/roboto.woff2) format(\"woff2\"), url(../webfonts/woff/roboto.woff) format(\"woff\");"
                        "    font-family: Roboto;"
                        "}"
                        ""
                        "@font-face {"
                        "    src: url(../webfonts/woff2/roboto.woff2) format(\"woff2\");"
                        "    src: url(../webfonts/woff/roboto.woff) format(\"woff\");"
                        "    font-family: Roboto;"
                        "}"
                        ""
                        "#some-id a {"
                        "    href: https://clojure.org/;"
                        "}"])))