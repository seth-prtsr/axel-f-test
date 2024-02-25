(ns build
  (:require [clojure.tools.build.api :as b]))

(def class-dir "classes")

(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn clean [_]
  (b/delete {:path class-dir}))

(defn compile-java [_]
  (b/javac {:src-dirs ["src"]
            :class-dir class-dir
            :basis @basis
            :javac-opts ["-Xlint:-options"]}))
