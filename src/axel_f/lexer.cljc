(ns axel-f.lexer
  (:refer-clojure :exclude [read])
  (:require [clojure.edn :as edn]
            [clojure.string :as string]))

(def ^:dynamic ^:private *throw* (fn [ex] (throw ex)))

(defn- whitespace? [{::keys [v]}]
  (contains? #{\space \tab \newline} v))

(defn- newline? [{::keys [v] :as e}]
  (= \newline (or v e)))

(defn- number-literal? [{::keys [v]}]
  (contains? (set "0123456789") v))

(defn- string-literal? [{::keys [v]}]
  (contains? (set "\"'#") v))

(defn- comment-literal? [{::keys [v]}]
  (= \; v))

(defn- punctuation-literal? [{::keys [v]}]
  (contains? (set ".,()[]{}") v))

(defn- operator-literal? [{::keys [v]}]
  (contains? (set ":+-*/&=<>^!%") v))

(defn- escape-char? [{::keys [v type]}]
  (= type ::escaped))

(defn- escape-str [args]
  (string/replace (apply str args) #"\\(.)" "$1"))

(defmulti read-next* (fn [[e & ex]]
                       (cond
                         (whitespace? e) ::whitespace
                         (number-literal? e) ::number-literal
                         (string-literal? e) ::string-literal
                         (punctuation-literal? e) ::punctuation-literal
                         (operator-literal? e) ::operator-literal
                         (comment-literal? e) ::comment
                         (nil? e) ::eof
                         :otherwise ::symbol-literal)))

(defmethod read-next* ::whitespace [ex]
  (loop [e (first ex) ex ex]
    (if (whitespace? e)
      (let [ex (next ex)]
        (recur (first ex) ex))
      [nil ex])))

(defn- read-natural [[e & ex]]
  (loop [acc [] e' e ex' ex ex'' ex]
    (if (number-literal? e')
      (recur (conj acc e') (first ex') (next ex') ex')
      [acc ex''])))

(defmethod read-next* ::number-literal [ex]
  (let [{::keys [l c]} (first ex)]
    (loop [acc [] ex ex part ::natural]
      (let [[acc' ex']
            (update-in
             (case part

               ::natural
               (read-natural ex)

               ::fract
               (if (= \. (::v (first ex)))
                 (update-in (read-natural (next ex)) [0] (partial cons (first ex)))
                 ['() ex])

               ::scientific
               (if (contains? (set "eE") (::v (first ex)))
                 (let [signed? (contains? (set "+-") (::v (second ex)))]
                   (update-in (read-natural (if signed? (nnext ex) (next ex)))
                              [0]
                              (partial concat (if signed? (list (first ex) (second ex)) (list (first ex))))))
                 ['() ex]))
             [0] (partial concat acc))
            next-number-part ({::natural ::fract
                               ::fract ::scientific}
                              part)]
        (if next-number-part
          (recur acc' ex' next-number-part)
          [{::type ::number
            ::value (edn/read-string (->> acc' (map ::v) (apply str)))
            ::line l
            ::col c
            ::length (count acc')}
           ex'])))))

(defmethod read-next* ::string-literal [ex]
  (let [ex (if (= \# (-> ex first ::v)) (next ex) ex)
        {literal-t ::v
         line ::l
         col ::c} (first ex)]
    (loop [acc [] {::keys [v] :as e} (second ex) ex (nnext ex)]
      (if (and (= v literal-t)
               (not (escape-char? (last acc))))
        [{::type ::string
          ::value (->> acc (map ::v) escape-str)
          ::line line
          ::col col
          ::length (+ 2 (count acc))}
         ex]
        (recur (conj acc (if (and (= \\ (::v e))
                                  (not (escape-char? (last acc))))
                           (assoc e ::type ::escaped)
                           e))
               (first ex)
               (next ex))))))

(defmethod read-next* ::punctuation-literal [[{::keys [v l c]} & ex]]
  [{::type ::punct
    ::value (str v)
    ::line l
    ::col c
    ::length 1}
   ex])

(defmethod read-next* ::operator-literal [[{::keys [v l c]} & ex]]
  (let [compound? (contains? (set ["<=" ">=" "<>"]) (str v (::v (first ex))))
        op (str v (when compound? (::v (first ex))))]
    [{::type ::operator
      ::value op
      ::line l
      ::col c
      ::length (count op)}
     (if compound? (next ex) ex)]))

(defmethod read-next* ::symbol-literal [ex]
  (let [{::keys [l c]} (first ex)]
    (loop [acc [] ex ex]
      (let [{::keys [v] :as e'} (first ex)]
        (if (and (or (empty? ex)
                     (whitespace? e')
                     (contains? (set ",.[](){}") v))
                 (not (escape-char? (last acc))))
          [{::type ::symbol
            ::value (->> acc (map ::v) escape-str)
            ::line l
            ::col c
            ::length (count acc)}
           ex]
          (recur (conj acc (if (and (= \\ (::v e'))
                                    (not (escape-char? (last acc))))
                             (assoc e' ::type ::escaped)
                             e'))
                 (next ex)))))))

(defn- drop-inline-comment [ex line]
  (if (= line (::l (first ex)))
    (recur (next ex) line)
    ex))

(defn- drop-block-comment [ex]
  (let [f (::v (first ex))
        s (::v (second ex))]
    (cond
      (= "~;" (str f s))
      (nnext ex)

      (not s)
      (*throw* (ex-info "Unclosed comment block" (first ex)))

      :otherwise
      (recur (next ex)))))

(defmethod read-next* ::comment [[{::keys [l]} & ex]]
  (let [{::keys [v]} (first ex)]
    [nil (if (= \~ v)
           (drop-block-comment (next ex))
           (drop-inline-comment (next ex) l))]))

(defmethod read-next* :default [{::keys [v] :as e} & _]
  (*throw* (ex-info (str "Can't handle character: " v) e)))

(defmethod read-next* ::eof [ex]
  [{::type ::eof} nil])

(defn- str->stream
  ([s] (str->stream s 0 1))
  ([s col line]
   (lazy-seq
    (when-let [e (first s)]
      (cons {::v e
             ::l line
             ::c (inc col)}
            (str->stream (next s)
                         (if (newline? e) 0 (inc col))
                         (if (newline? e) (inc line) line)))))))

(defn- read-next [tokens]
  (lazy-seq
   (let [[token tokens] (read-next* tokens)]
     (cond
       (= (::type token) ::eof)
       (list token)

       token
       (cons token (read-next tokens))

       :otherwise
       (if (not-empty tokens)
         (read-next tokens)
         (list {::type ::eof}))))))

(defn read [s]
  (read-next (str->stream s)))
