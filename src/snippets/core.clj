(ns snippets.core
  (:require
   [ubergraph.core :as uber]
   [clojure.math.combinatorics :as combo]
   [clojure.string :as str]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [snippets.defaults :as d]
   [snippets.graph :as g]))


(defn snippet
  [prefix description body]
  {prefix {:prefix prefix
           :description description
           :body body}})


(comment
  (println (str/join \newline d/dragon))
  )


(defn value->str
  [table-alias f]
  (cond
    (coll? f) (str "(" (str/join ", " (mapv #(value->str table-alias %) f)) ")")
    (string? f) (str \' (str/replace f #"'" "''") \')
    (keyword? f) (str (name table-alias) "." (name f))))


(defn clause->str
  [src dest [f1 f2]]
  (let [f1-coll? (coll? f1)]
    (str (if f1-coll?
           (value->str dest f2)
           (value->str src f1))
         (if (some coll? [f1 f2])
           " IN "
           " = ")
         (if f1-coll?
           (value->str src f1)
           (value->str dest f2)))))


(comment
  (clause->str :trl :hum [[:source_hu_id :destination_hu_id] :hu_id])
  (clause->str :trl :hum [:wh_id :wh_id])
  (clause->str :loc :lkp ["t_location" :source])
  (clause->str :hum :hum2 [:parent_hu_id :hu_id])
  )


(defn table-source
  ([table]
   table)
  ([db table]
   (str (when db (str db "..")) table))
  ([db schema table]
   (str db "." schema "." table)))


(defn edge->body
  [g [src dest join-map]]
  (let [{:keys [db table]} (uber/attrs g dest)
        dest (if (= src dest)
               (keyword (str (name dest) "2"))
               dest)]
    (concat [(str "JOIN " (table-source db table) " " (name dest) " WITH (NOLOCK)")]
            (map str
                 (conj (repeat "\tAND ") "\tON ")
                 (map #(clause->str src dest %) join-map)))))


(comment
  (edge->body
   g/graph [:orm :ord {:wh_id :wh_id :order :order}])
  (edge->body
   g/graph [:hum :hum {:wh_id :wh_id :parent_hu_id :hu_id}])
  (edge->body
   g/graph [:loc :lkp {"t_location" :source
                 "1033" :locale_id
                 :type :text
                 "TYPE" :lookup_type}])
  )


(defn edges->snippet
  [g n edges]
  (let [path-snippet? (some #(not= n (uber/src %)) edges)
        prefix (str (name n)
                    (str/join (map
                               (fn [[src dest _]]
                                 (if (= src n) #_path-snippet?
                                     (name dest)
                                     (str "-" (name dest))))
                               edges)))
        description (str "Join an existing " (name n) " table to "
                         (->> edges
                              (map
                               (fn [[src dest _]]
                                 (if (= src n)
                                   (name dest)
                                   (str (name src) " to " (name dest)))))
                              (str/join ", ")))
        body (concat
              (mapcat #(edge->body g %) edges)
              ["$0"])]
    (snippet prefix description body)))


(comment
  (edges->snippet g/graph :orm [[:orm :wkq {:wh_id :wh_id, :order_number :pick_ref_number}]
                          [:orm :car {:carrier_id :carrier_id}]
                          [:orm :ldm {:wh_id :wh_id, :load_id :load_id}]])

  (edges->snippet g/graph :pod [[:pod :itm {:wh_id :wh_id, :order_number :pick_ref_number}]
                          [:itm :itu {:carrier_id :carrier_id}]])
  )


(comment
  (->> (g/table-paths g/graph :pkd)
       (take 10)
       (map #(edges->snippet g/graph :pkd %))))


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
         (map #(edges->snippet g node %))
         (into {(name node) {:prefix (name node)
                             :description (str "Table " table " with alias: " (name node))
                             :body [(str "FROM " (table-source db table) " " (name node) " WITH (NOLOCK)")
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


(defn write-vscode-snippets
  [f]
  (json/generate-stream
   (conj (graph-snippets g/graph)
         (snippet "dragon" "A dragon" d/dragon)
         (snippet "dragoncow" "A dragon and a cow" d/dragon-cow)
         (snippet "ifelse" "IF block and an ELSE block" d/if-else)
         (snippet "btran" "Begin a transaction safely" d/transaction))
   (io/writer f)
   {:pretty true}))


(comment
  (write-vscode-snippets "out/sql.json")
  )


(comment
  (->> (graph-snippets g/graph)
       (take-last 2))
  )



