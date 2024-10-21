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


;; TODO - probably should be snippet for generating the FROM, as well as the JOINs, in one snippet
;; *stopkd ->
;; FROM t_stored_item sto WITH (NOLOCK)
;; JOIN t_pick_detail pkd WITH (NOLOCK)
;;     ON sto.type = pkd.pick_id
(defn table-snippets
  [g node]
  (let [{:keys [db table]} (uber/attrs g node)
        paths (g/table-paths g node)]
    (->> (g/table-edges g node)
         (remove empty?)
         (filter #(<= (count %) 2 #_3 #_4))
         (mapcat combo/permutations)
         (concat paths)
         (map #(gen/edges->snippet g node %))
         (into {(name node) {:prefix (name node)
                             :description (str "Table " table " with alias: " (name node))
                             :body [(str "FROM " (gen/table-source db table) " " (name node) " WITH (NOLOCK)")
                                    "$0"]}}))))

(comment
  (-> (uber/out-edges g/graph :orm)
      (combo/subsets)
      (combo/cartesian-product (uber/in-edges g/graph :orm)))
  )


(defn graph-snippets
  [g]
  (->> (uber/nodes g)
       (map #(table-snippets g %))
       (into {})))


(comment
  (->> (graph-snippets g/graph)
       (take-last 2))
  )


(def snippets
  (conj (graph-snippets g/graph)
        (v/snippet "dragon" "A dragon" d/dragon)
        (v/snippet "dragoncow" "A dragon and a cow" d/dragon-cow)
        (v/snippet "ifelse" "IF block and an ELSE block" d/if-else)
        (v/snippet "btran" "Begin a transaction safely" d/transaction)))


(defn write-vscode-snippets
  [f]
  (json/generate-stream
   snippets
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



