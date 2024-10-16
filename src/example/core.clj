(ns example.core
  (:require
   [ubergraph.core :as uber]
   [ubergraph.alg :as alg]
   [clojure.math.combinatorics :as combo]
   [clojure.set :as set]
   [clojure.string :as str]
   [cheshire.core :as json]
   [clojure.java.io :as io]))


(def g
  (uber/digraph
   [:sto {:table "t_stored_item"}]
   [:loc {:table "t_location"}]
   [:hum {:table "t_hu_master"}]
   [:pkd {:table "t_pick_detail"}]
   [:itm {:table "t_item_master"}]
   [:orm {:table "t_order"}]
   [:ord {:table "t_order_detail"}]
   [:ldm {:table "t_load_master"}]
   [:wkq {:table "t_work_q"}]
   [:wka {:table "t_work_q_assignment"}]
   [:uom {:table "t_item_uom"}]
   [:car {:table "t_carrier"}]
   [:alo {:table "t_allocation"}]
   [:pom {:table "t_po_master"}]
   [:pod {:table "t_po_detail"}]
   [:rec {:table "t_receipt"}]
   [:pkc {:table "t_pick_container"}]
   [:zlc {:table "t_zone_loca"}]
   [:zon {:table "t_zone"}]
   [:trn {:table "t_tran_log"}]
   ;; sto
   [:sto :pkd {:type :pick_id}]
   [:sto :loc {:wh_id :wh_id
               :location_id :location_id}]
   [:sto :hum {:wh_id :wh_id
               :hu_id :hu_id}]
   [:sto :itm {:wh_id :wh_id
               :item_number :item_number}]
   ;; loc
   ;; hum
   [:hum :loc {:wh_id :wh_id
               :location_id :location_id}]
   [:hum :orm {:wh_id :wh_id
               :control_number :order_number}]
   [:hum :ldm {:wh_id :wh_id
               :load_id :load_id}]
   ;; pkd
   [:pkd :orm {:wh_id :wh_id
               :order_number :order_number}]
   [:pkd :wkq {:wh_id :wh_id
               :work_q_id :work_q_id}]
   [:pkd :itm {:wh_id :wh_id
               :item_number :item_number}]
   [:pkd :loc {:wh_id :wh_id
               :pick_location :location_id}]
   [:pkd :ord {:wh_id :wh_id
               :order_number :order_number
               ;; :item_number :item_number ;; this is not order_detail UNIQUE key
               :line_number :line_number}]
   [:pkd :pkc {:wh_id :wh_id
               :container_id :container_id}]
   [:pkd :ldm {:wh_id :wh_id
               :load_id :load_id}]
   ;; itm
   ;; TODO this might be the wrong order! uom -> itm, instead
   [:itm :uom {:wh_id :wh_id
               :item_number :item_number
               :uom :uom}]
   ;; orm
   [:orm :car {:carrier_id :carrier_id}] ;; some use {:wh_id :wh_id :carrier :carrier_code}
   [:orm :ldm {:wh_id :wh_id
               :load_id :load_id}]
   ;; ord
   [:ord :orm {:wh_id :wh_id
               :order_number :order_number}]
   [:ord :itm {:wh_id :wh_id
               :item_number :item_number}]
   ;; ldm
   ;; wkq
   [:wkq :loc {:wh_id :wh_id
               :location_id :location_id}]
   [:wkq :itm {:wh_id :wh_id
               :item_number :item_number}]
   [:wkq :orm {:wh_id :wh_id
               :pick_ref_number :order_number}]
   [:wkq :ldm {:wh_id :wh_id
               :pick_ref_number :load_id}]
   [:wkq :wvm {:wh_id :wh_id
               :pick_ref_number :wave_id}]
   ;; wka
   [:wka :wkq {:wh_id :wh_id
               :work_q_id :work_q_id}]
   ;; car
   ;; alo
   [:alo :pkd {:pick_id :pick_id}]
   [:alo :itm {:wh_id :wh_id
               :item_number :item_number}]
   [:alo :loc {:wh_id :wh_id
               :pick_location :location_id}]
   ;; pom
   ;; pod
   [:pod :pom {:wh_id :wh_id
               :po_number :po_number}]
   [:pod :itm {:wh_id :wh_id
               :item_number :item_number}]
   ;; rec
   [:rec :hum {:wh_id :wh_id
               :hu_id :hu_id}]
   [:rec :itm {:wh_id :wh_id
               :item_number :item_number}]
   [:rec :pod {:wh_id :wh_id
               :po_number :po_number
               :line_number :line_number
               :schedule_number :schedule_number}]
   [:rec :uom {:wh_id :wh_id
               :item_number :item_number
               :receipt_uom :uom}]
   ;; pkc
   [:pkc :hum {:wh_id :wh_id
               :container_label :hu_id}]
   ;; zlc
   [:zlc :loc {:wh_id :wh_id
               :location_id :location_id}]
   [:zlc :zon {:wh_id :wh_id
               :zone :zone}]
   ;; zon
   ;; trn
   [:trn :itm {:wh_id :wh_id
               :item_number :item_number}]
   [:trn :hum {:wh_id :wh_id
               [:source_hu_id :destination_hu_id] :hu_id}]
   [:trn :loc {:wh_id :wh_id
               [:source_location_id :destination_location_id] :location_id}]
   [:trn :wkq {:wh_id :wh_id
               :work_q_id :work_q_id}]))

;; TODO, maybe - remove all the g arguments (everything points to the db schema graph) and enjoy all the cleanup

(defn all-paths
  ([g node]
   (all-paths g node []))
  ([g node path]
   (if-let [out-edges (seq (->> (uber/out-edges g node)
                                (map #(uber/edge-with-attrs g %))
                                (remove #(some #{%} path))  ;; don't allow cycles (two edges in same path)
                                ))]
     (mapcat
      (fn [edge]
        (all-paths g
                   (uber/dest edge)
                   (conj path edge)))
      out-edges)
     (if (empty? path)
       []
       [path]))))

(comment
  (all-paths g :pkd))

(defn paths-within
  [path]
  (map #(take (inc %) path) (range (count path)))
  #_(loop [path path
           paths ()]
      (if-let [s (seq path)]
        (recur (butlast s) (conj paths s))
        paths)))


(defn clause->str
  [src dest [f1 f2]]

  (str (if (coll? f1)
         (str (name dest) "." (name f2))
         (str (name src) "." (name f1)))
       (if (some coll? [f1 f2])
         " IN "
         " = ")
       (cond
         (coll? f1) (str "(" (str/join ", " (map #(str (name src) "." (name %)) f1)) ")")
         (coll? f2) (str "(" (str/join ", " (map #(str (name dest) "." (name %)) f2)) ")")
         :else (str (name dest) "." (name f2)))
       #_(if (coll? f2)
           (str "(" (str/join ", " (map #(str (name src) "." (name %)) f2)) ")")
           (str (name src) "." (name f2)))))

(clause->str :trn :hum [[:source_hu_id :destination_hu_id] :hu_id])
(clause->str :trn :hum [:wh_id :wh_id])


(defn edge->body
  [g [src dest join-map]]
  (let [table (uber/attr g dest :table)]
    (concat [(str "JOIN " table " " (name dest) " WITH (NOLOCK)")]
            (map str
                 (conj (repeat "\tAND ") "\tON ")
                 (map #(clause->str src dest %) join-map)))))

(comment
  (edge->body
   g [:orm :ord {:wh_id :wh_id :order :order}]))


(defn edges->snippet
  [g n edges]
  (let [path-snippet? (some #(not= n (uber/src %)) edges)
        prefix (str (name n)
                    (str/join (map
                               (fn [[src dest _]]
                                 (if (= src n) #_path-snippet?
                                   (name dest)
                                   (str "-" (name dest))))
                               edges)))]
    {prefix {:prefix prefix
             :description (str "Join an existing " (name n) " table to "
                               (->> edges
                                    (map
                                     (fn [[src dest _]]
                                       (if (= src n)
                                         (name dest)
                                         (str (name src) " to " (name dest)))))
                                    (str/join ", ")))
             :body (concat
                    (mapcat #(edge->body g %) edges)
                    ["$0"])}}))


(comment
  (edges->snippet g :orm [[:orm :wkq {:wh_id :wh_id, :order_number :pick_ref_number}]
                          [:orm :car {:carrier_id :carrier_id}]
                          [:orm :ldm {:wh_id :wh_id, :load_id :load_id}]])

  (edges->snippet g :pod [[:pod :itm {:wh_id :wh_id, :order_number :pick_ref_number}]
                          [:itm :uom {:carrier_id :carrier_id}]]))


(defn invert-edge
  [[src dest join]]
  [dest src (set/map-invert join)])

(comment
  (invert-edge [:sto :pkd {:type :pick_id}]))

(defn table-paths
  [g node]
  (->> (all-paths g node)
       (mapcat paths-within)
       (filter #(> (count %) 1))
       (distinct)))

(comment
  (->> (table-paths g :pkd)
       (take 10)
       (map #(edges->snippet g :pkd %))))

(defn table-edges
  [g node]
  (let [out-subsets (->> (uber/out-edges g node)
                         (map #(uber/edge-with-attrs g %))
                         (combo/subsets))
        in-edges (->> (uber/in-edges g node)
                      (map (partial uber/edge-with-attrs g))
                      (map invert-edge))]
    (concat out-subsets
            (for [out-subset out-subsets
                  in-edge in-edges]
              (conj out-subset in-edge)))))

(comment
  (table-edges g :orm))

;; TODO - probably should be snippet for generating the FROM, as well as the JOINs, in one snippet
;; *stopkd ->
;; FROM t_stored_item sto WITH (NOLOCK)
;; JOIN t_pick_detail pkd WITH (NOLOCK)
;;     ON sto.type = pkd.pick_id

(defn table-snippets
  [g node]
  (let [table (uber/attr g node :table)
        paths (table-paths g node)]
    (->> (table-edges g node)
         (remove empty?)
         (filter #(<= (count %) 4))
         (mapcat combo/permutations)
         (concat paths)
         (map #(edges->snippet g node %))
         (into {(name node) {:prefix (name node)
                             :description (str "Table " table " with alias: " (name node))
                             :body [(str "FROM " table " " (name node) " WITH (NOLOCK)")
                                    "$0"]}}))))

(comment
  (-> (uber/out-edges g :orm)
      (combo/subsets)
      (combo/cartesian-product (uber/in-edges g :orm))))


(defn graph-snippets
  [g]
  (->> (uber/nodes g)
       (map #(table-snippets g %))
       (into {})))

(comment
  (alg/bellman-ford g {:start-node :sto
                       :end-node :orm})

  (alg/bellman-ford g {:start-node :sto
                       :cost-fn (constantly 1)
                       :traverse false})

  (->> (alg/bf-traverse g :sto)
       (map #(alg/shortest-path g :sto %)))

  (alg/shortest-path g {:start-node :sto
                        :traverse true
                        :min-cost 1})

  (filter (comp #{:orm} :end) *1)
  (def sto-orm *1)

  (alg/edges-in-path (first sto-orm))

  (->> (alg/shortest-path g {:start-node :sto
                             :traverse true})
       (map alg/edges-in-path))
  
  (alg/topsort g :pkd)

  (alg/nodes-in-path (alg/longest-shortest-path g :sto))
  
  (uber/viz-graph (alg/paths->graph
   (alg/shortest-path g {:start-node :sto
                         })))
  (alg/path-to
   (alg/shortest-path g {:start-node :sto
                         })
   :orm)
  
  (uber/edge? (uber/edge-description->edge g [:sto :pkd ]))

  ;; for every subset, use those as :source-nodes to find shortest paths
  ;; to rest. That way, if subset is stohum, it'll go to orm through stohumorm
  ;; instead of stopkdorm
  
  (alg/shortest-path g {:start-node :rec})

  (->> (map #(vector % (alg/nodes-in-path (alg/longest-shortest-path g %))) (uber/nodes g)))
  

  (alg/all-destinations
   (alg/shortest-path g {:start-node :sto})
   )
  
  (alg/maximal-cliques g)
  
  
  (def sto-car (last *1))
  (alg/edges-in-path sto-car)


  (uber/viz-graph g {:auto-label false
                     :save {:filename "graph.png"
                            :format :png}})

  (table-snippets g :hum)

  (->> (uber/nodes g)
       (mapcat #(table-paths g %))
       (set)
       (count))

  (->> (graph-snippets g)
       (take-last 2))

  ;; one table
  (json/generate-stream
   (table-snippets g :hum)
   (io/writer "sql.json")
   {:pretty true})

  (reduce * 1 (range 1 4))

  ;; graph
  (json/generate-stream
   (graph-snippets g)
   (io/writer "sql.json")
   {:pretty true})
  )

;; directed graph!!!
;; that should really cut down on generated path lengths, I think.
;; I think it'll be rare to have strings of joins more than 3 tables.

;; so a table can have *one* primary key and multiple unique indexes.
;; so how am I supposed to give a key to each unique index? I suppose that's why they name indexes
;; ex: t_carrier has a primary key (carrier_id) and two unique indexes (scac_code and carrier_code)
;; also t_stored_item has sto_id as primary key clustered, as well as unique nonclustered :'|

;; does ormpkdhumord make sense? does it matter?
;; it's almost like there will be directed edges, or something
;; like you can go ormord, but once you commit to going one to many, you can only do one to one's from there, or something
;; you can do as many one-to-one's as you like, but you can only have one one-to-many!

;; so maybe you can have as many out-edges as you want, but only one in-edge?
;; orm has outs to car, ldm, cli
;; orm has in from ord, hum, pkd, etc.

;; here's a problematic example. does pkd join to ord?
;; order to order? neither is unique/primary key
;; order/line to order/line? niether is unique/primary
;; maybe you make a rule that its only joins that involve primary/unique keys? below system would help enforce that during data entry
;; does hum join to pkd? Both have order_number information...
;; but you'd never really do that join!!!
;; like why would you want to know - ok here's a hum record. Now give me all the pkd records with that are
;; assigned to the same order as this hum. Like doesn't really make sense.
;; I have to constrain the size of this graph, probably, or there's going to be *tons* of snippets
;; This rule will be fine. Not a big deal

;; if you can join hum to orm, then you can also join hum directly to ord and pkd and anything else that uses orm's PK
;; for some things that's not the case... like you can't go sto to orm. but you can go sto to pkd to orm
;; so it's like: edge means that src table can hit destination's PK.
;; sto can hit loc, hum, pkd, etc.'s PK.
;; so the table nodes should have their primary key as part of their attributes
;; and then the edge is [src dest {:fk [:k ...]}]
;; ex: [:sto :pkd {:fd [:type]}] - sto has an edge to pkd through its type field. 
;; that might limit me, but I want to roll with that for a bit
;; this makes the graph build ordering important.
;; [:ord :orm {:fk [:wh_id :order_number]}] is valid, where
;; [:orm :ord {:fk [:wh_id :order_number]}] is not - ord's PK is not wh_id, order_number
;; IMPORTANT - it doesn't have to be just PK, though, it could be UNIQUE INDEX!
;; ex: ord's PK is order_detail_id (but who uses that??), whereas its UNIQUE INDEX is (wh_id, order_number, line_number)

;; `sudo apt install graphviz`
(comment
  (uber/viz-graph g {:layout :dot
                     :save {:filename "graph.png"
                            :format :png}})

  (->> (alg/shortest-path g {:start-node :a
                             :traverse true
                             :cost-attr :weight})
       (map alg/edges-in-path))

  (alg/edges-in-path (alg/shortest-path g :a :c :weight))
  (alg/pprint-path (alg/shortest-path g :a :c :weight))

  (uber/out-edges g :a)

  (alg/connected-components g)

  (alg/bf-span g :a)
  (alg/bf-traverse g :a)

  (alg/pre-span g :a)

  (uber/edges g)

  (->> (uber/nodes g)
       (map
        #(->> [% (uber/neighbors g %)])))

  (uber/out-edges g :a)

  (uber/find-edge g :a :c))


