(ns tornado.test.core
  (:require [clojure.test :refer :all]
            [tornado.test.at-rules]
            [tornado.test.colors]
            [tornado.test.common]
            [tornado.test.functions]
            [tornado.test.selectors]
            [tornado.test.units]))

(defn test-all []
  (run-tests 'tornado.test.at-rules
             'tornado.test.colors
             'tornado.test.functions
             'tornado.test.selectors
             'tornado.test.units
             'tornado.test.common))

(defn -main [& _]
  (test-all))