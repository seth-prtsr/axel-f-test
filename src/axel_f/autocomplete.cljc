(ns axel-f.autocomplete
  (:require [axel-f.lexer :as lexer]
            [axel-f.parser :as parser]
            [clj-fuzzy.metrics :as fuzzy]
            [clojure.string :as string])
  #?(:clj (:import [clojure.lang ExceptionInfo])))

(defn arg->doc [arg opts]
  (let [{:keys [doc]} (meta arg)]
    (merge {:desc doc} opts)))

(defn arglist->doc [arglist]
  (loop [acc [] opts {} arg (first arglist) arglist (rest arglist)]
    (if arg
      (if (= arg '&)
        (recur acc (assoc opts :opt true :repeatable true) (first arglist) (rest arglist))
        (if (vector? arg)
          (recur acc (dissoc opts :repeatable) (first arg) (concat (rest arg) (rest arglist)))
          (recur (conj acc (arg->doc arg opts)) {} (first arglist) (rest arglist))))
      acc)))

(defn ref-meta->doc [{:keys [doc arglists]}]
  {:desc doc
   :args (arglist->doc (first arglists))})

(defn flatten
  "Transform a nested map into a seq of [keyseq leaf-val] pairs"
  [m]
  (when m
    ((fn flatten-helper [keyseq m]
       (when m
         (cond
           (map? m)
           (concat
            [[keyseq m]]
            (mapcat
             (fn [[k v]]
               (flatten-helper (concat keyseq (list k)) v))
             m))

           (and (sequential? m)
                (indexed? m))
           (concat
            [[(concat keyseq (list "*")) m]]
            (mapcat
             (fn [i v]
               (concat
                (flatten-helper (concat keyseq (list i)) v)
                (when (or (map? v)
                          (and (sequential? v)
                               (indexed? v)))
                  (flatten-helper (concat keyseq (list "*")) v))))
             (range)
             m))

           :else
           [[keyseq m]])))
     '() m)))

(defn env->index [env]
  (loop [acc {} paths (flatten env)]
    (if (empty? paths)
      acc
      (let [[path v] (first paths)]
        (recur
         (if ((some-fn fn? var?) v)
           (assoc acc path (ref-meta->doc (meta v)))
           acc)
         (rest paths))))))

(defn context->index [context]
  (loop [acc {} paths (flatten context)]
    (if (empty? paths)
      acc
      (let [[path v] (first paths)
            visited? (contains? acc path)]
        (recur (if visited?
                 (let [{:keys [sub-type value]} (get acc path)]
                   (case sub-type
                     nil
                     (assoc acc path {:type :REF
                                      :sub-type :INDEX
                                      :value [value v]})
                     :INDEX
                     (update-in acc [path :value] conj v)))
                 (assoc acc path {:type :REF
                                  :value v}))
               (rest paths))))))

(defn index [obj]
  (let [env (dissoc obj :axel-f.runtime/context)
        context (:axel-f.runtime/context obj)]
    (merge (env->index env)
           (context->index context))))

(defn ->string [s]
  (if (keyword? s)
    (string/join "/" (filter identity ((juxt namespace name) s)))
    (str s)))

(defn distance [s1 s2]
  (if (empty? s2)
    1 (fuzzy/jaccard s1 s2)))

(defn search-index [index path]
  (sort-by
   (fn [[_ {:keys [distance]}]]
     distance)
   (sequence
    (comp
     (filter (fn [[ik _]]
               (and (= (count ik) (count path))
                    (= (map ->string (butlast ik)) (map ->string (butlast path)))
                    (or (= (->string (last path)) (->string (last ik)))
                        (> (/ 2 3) (distance (->string (last path)) (->string (last ik))))))))
     (map (fn [[ik v]]
            [ik (assoc v :distance (distance (->string (last path)) (->string (last ik))))])))
    index)))
