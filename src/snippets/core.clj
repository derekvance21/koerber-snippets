(ns snippets.core
  (:require
   [ubergraph.core :as uber]
   [clojure.math.combinatorics :as combo]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [snippets.xml :as x]
   [snippets.vscode :as v]
   [snippets.defaults :as d]
   [snippets.graph :as g]
   [snippets.generate :as gen]))


(def path-snippets
  (eduction (map gen/path-snippet) g/paths))


(def node-snippets
  (eduction (map gen/node-snippet) g/node-descriptions))


;; (defn table-snippets
;;   [g node]
;;   (let [{:keys [db table]} (uber/attrs g node)
;;         paths (g/table-paths g node)]
;;     (->> (g/table-edges g node)
;;          (remove empty?)
;;          (filter #(<= (count %) 2 #_3 #_4))
;;          (mapcat combo/permutations)
;;          (concat paths)
;;          (map #(gen/edges->snippet g node %))
;;          (into {(name node) }))))


(def default-snippets
  [(gen/->Snippet "dragon" "A dragon" d/dragon)
   (gen/->Snippet "dragoncow" "A dragon and a cow" d/dragon-cow)
   (gen/->Snippet "ifelse" "IF block and an ELSE block" d/if-else)
   (gen/->Snippet "btran" "Begin a transaction safely" d/transaction)])


(def snippets
  (into
   []
   cat
   [node-snippets
    path-snippets
    default-snippets]))


(comment
  (get (into {} (map (juxt :prefix identity)) snippets)
       "stohum")
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


(comment
  (write-vscode-snippets "out/sql.json")
  (write-xml-snippets "out/snippets.snippet")
  )



