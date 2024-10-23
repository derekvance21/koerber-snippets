(ns snippets.core
  (:require
   [snippets.combo :as c]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [snippets.xml :as x]
   [snippets.defaults :as d]
   [snippets.graph :as g]
   [snippets.generate :as gen])
  (:gen-class))


(defn join-from-subset
  [[start & dests]]
  (let [edges (g/edges-to-destinations g/graph start dests)]
    (when-not (empty? edges)
      {:start start
       :dests dests
       :edges edges})))


(def joins
  (sequence
   (comp (map join-from-subset) (remove empty?))
   (c/permuted-subsets g/schema-nodes 2 d/max-join-length)))


(def default-snippets
  [(gen/->Snippet "dragon" "A dragon" d/dragon)
   (gen/->Snippet "dragoncow" "A dragon and a cow" d/dragon-cow)
   (gen/->Snippet "ifelse" "IF block and an ELSE block" d/if-else)
   (gen/->Snippet "btran" "Begin a transaction safely" d/transaction)])


(def snippets
  (into
   []
   cat
   [(eduction (map gen/node-snippet) g/node-descriptions)
    (eduction (map gen/join-snippet) joins)
    default-snippets]))


(comment
  (get (into {} (map (juxt :prefix identity)) snippets)
       "stohum")
  (get (into {} (map (juxt :prefix identity)) snippets)
       "stolocemp")
  )

(defn write-vscode-snippets
  [f]
  (json/generate-stream
   (into {} (map (juxt :prefix identity)) snippets)
   (io/writer f)
   {:pretty true}))


(defn write-xml-snippets
  [f]
  (with-open [w (io/writer f)]
    (binding [*out* w]
      (x/emit-code-snippets snippets))))


(defn -main
  [& _]
  (write-vscode-snippets "out/sql.json")
  (write-xml-snippets "out/snippets.snippet"))


(comment
  (-main)
  )



