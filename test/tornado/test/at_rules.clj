(ns tornado.test.at-rules
  "It's almost impossible to write exact CSS representations with all the spaces
  so the tests are done with compressed CSS."
  (:require [clojure.test :refer :all]
            [tornado.core :refer :all]
            [tornado.compression :refer [compress]]))

(deftest at-media*
  (are [x y] (= (compress (css x)) y)
             [:.class-1 {:width (px 500)}
              (at-media {:max-width (px 800)}
                        [:& {:width           (px 400)
                             :text-decoration :none}]
                        [:#nested-id {:color :red}])]
             "@media(max-width:800px){.class-1{width:400px;text-decoration:none;}.class-1 #nested-id{color:#FF0000;}}.class-1{width:500px;}"
             [:.class-1 {:width (px 500)}
              (at-media {:max-width (px 800)
                         :scren     :only
                         :speech    false}
                        [:#nested-id {:color :red}])]
             "@media(max-width:800px)and only scren and not speech{.class-1 #nested-id{color:#FF0000;}}.class-1{width:500px;}"))


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

;; FIXME: Very ugly hack  because of unpredictability? of the order of the components. Should be changed in the future.
(deftest at-font-face*
  (let [css-hiccup (list (at-font-face {:src         [[(url "../webfonts/woff2/roboto.woff2") (css-format :woff2)]
                                                      [(url "../webfonts/woff/roboto.woff") (css-format :woff)]]
                                        :font-family "Roboto"})
                         (at-font-face {:src [[(url "../webfonts/woff2/roboto.woff2") (css-format :woff2)]]}
                                       {:src         [[(url "../webfonts/woff/roboto.woff") (css-format :woff)]]
                                        :font-family "Roboto"})
                         [:#some-id
                          [:a {:href "https://clojure.org/"}]])
        compressed (compress (css css-hiccup))
        [url-1 url-2] ["url(../webfonts/woff2/roboto.woff2)format(\"woff2\")"
                       "url(../webfonts/woff/roboto.woff)format(\"woff\")"]
        [src-url-3 src-url-4] ["src:url(../webfonts/woff/roboto.woff)format(\"woff\")"
                               "src:url(../webfonts/woff2/roboto.woff2)format(\"woff2\")"]]
    (binding [*out* *err*]
      (is (some (fn [x]
                  (println x)
                  (println compressed)
                  (newline)
                  (= x compressed))
                (for [[url-1 url-2] [[url-1 url-2] [url-2 url-1]]
                      [src-url-3 src-url-4] [[src-url-3 src-url-4] [src-url-4 src-url-3]]
                      [ff1 ff2] [[(str "@font-face{src:" url-1 "," url-2 ";font-family:Roboto;}")
                                  (str "@font-face{" src-url-3 ";" src-url-4 ";font-family:Roboto;}")]
                                 [(str "@font-face{" src-url-3 ";" src-url-4 ";font-family:Roboto;}")
                                  (str "@font-face{src:" url-1 "," url-2 ";font-family:Roboto;}")]]]
                  (str ff1 ff2 "#some-id a{href:https://clojure.org/;}")))))))