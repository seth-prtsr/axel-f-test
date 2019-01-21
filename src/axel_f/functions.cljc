(ns axel-f.functions
  (:require [clojure.string :as string]
            [axel-f.functions.core :refer [def-excel-fn *functions-store*]]
            axel-f.functions.math
            axel-f.functions.text
            axel-f.functions.stat
            axel-f.functions.logic))

(def clean
  ^{:desc "Returns the text with the non-printable ASCII characters removed."
    :args [{:desc "The text whose non-printable characters are to be removed."}]}
  (fn [text]
    (string/replace text #"[\x00-\x1F]" "")))

(def-excel-fn "CLEAN" clean)

(defn find-impl [fname]
  (get @*functions-store* fname))
