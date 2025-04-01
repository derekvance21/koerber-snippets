(ns snippets.core
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [snippets.defaults :as d]
   [snippets.generate :as gen]
   [snippets.graph :as g]
   [snippets.xml :as x]
   [ubergraph.core :as uber])
  (:gen-class))

(defn join-from-subset
  [[start & dests]]
  (let [edges (g/edges-to-destinations g/schema start dests)]
    (when-not (empty? edges)
      {:start start
       :dests dests
       :edges edges})))


(def joins
  (sequence
   (map (fn [[src dest :as edge]]
          {:start src
           :dests [dest]
           :edges [(g/edge-description g/schema edge)]}))
   g/schema-edges))

(def default-snippets
  (into
   []
   (map (partial apply gen/->Snippet))
   [["dragon" "A dragon" d/dragon]
    ["dragoncow" "A dragon and a cow" d/dragon-cow]
    ["ifelse" "IF block and an ELSE block" d/if-else]
    ["sel" "A select top 1000 * statement" d/select-1000]
    ["btran" "Begin a transaction safely" d/transaction]
    ["btry" "Begin a try catch block" d/try-catch-tran]
    ["crsr" "Begin a local cursor" d/cursor]]))


(def snippets
  (into
   []
   cat
   [(eduction (map gen/node-snippet) g/node-descriptions)
    (eduction (map gen/join-snippet) joins)
    (eduction (map gen/semi-join-snippet) g/one-to-many-edges)
    (eduction (map gen/apply-join-snippet) g/one-to-many-edges)
    default-snippets]))


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
