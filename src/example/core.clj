(ns example.core
  (:require
   [ubergraph.core :as uber]
   [ubergraph.alg :as alg]
   [clojure.math.combinatorics :as combo]
   [clojure.set :as set]
   [clojure.string :as str]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.xml :as xml]))



(defn snippet
  [prefix description body]
  {prefix {:prefix prefix
           :description description
           :body body}})


(def dragon
  (snippet
   "dragon"
   "Dragon"
   ["/*"
    "                          / \\  //\\"
    "            |\\___/|      /   \\//  \\\\"
    "            /0  0  \\__  /    //  | \\ \\    "
    "           /     /  \\/_/    //   |  \\  \\  "
    "           @_^_@'/   \\/_   //    |   \\   \\ "
    "           //_^_/     \\/_ //     |    \\    \\"
    "        ( //) |        \\///      |     \\     \\"
    "      ( / /) _|_ /   )  //       |      \\     _\\"
    "    ( // /) '/,_ _ _/  ( ; -.    |    _ _\\.-~        .-~~~^-."
    "  (( / / )) ,-{        _      `-.|.-~-.           .~         `."
    " (( // / ))  '/\\      /                 ~-. _ .-~      .-~^-.  \\"
    " (( /// ))      `.   {            }                   /      \\  \\"
    "  (( / ))     .----~-.\\        \\-'                 .~         \\  `. \\^-."
    "             ///.----..>        \\             _ -~             `.  ^-`  ^-_"
    "               ///-._ _ _ _ _ _ _}^ - - - - ~                     ~-- ,.-~"
    "                                                                  /.-~)"
    "*/"
    "$0"]))


(def dragon-cow
  (snippet
   "dragoncow"
   "Dragon with cow"
   ["/*",
    "                                ^    /^",
    "                               / \\  // \\",
    "                 |\\___/|      /   \\//  .\\",
    "                 /O  O  \\__  /    //  | \\ \\           *----*",
    "                /     /  \\/_/    //   |  \\  \\          \\   |",
    "                @___@`    \\/_   //    |   \\   \\         \\/\\ \\",
    "               0/0/|       \\/_ //     |    \\    \\         \\  \\",
    "           0/0/0/0/|        \\///      |     \\     \\       |  |",
    "        0/0/0/0/0/_|_ /   (  //       |      \\     _\\     |  /",
    "     0/0/0/0/0/0/`/,_ _ _/  ) ; -.    |    _ _\\.-~       /   /",
    "                 ,-}        _      *-.|.-~-.           .~    ~",
    "\\     \\__/        `/\\      /                 ~-. _ .-~      /",
    " \\____(oo)           *.   }            {                   /",
    " (    (--)          .----~-.\\        \\-`                 .~",
    " //__\\\\  \\__ Ack!   ///.----..<        \\             _ -~",
    "//    \\\\               ///-._ _ _ _ _ _ _{^ - - - - ~",
    "*/"
    "$0"]))

(def tran-snippet
  (snippet
   "btran"
   "Transaction inside a stored procedure"
   ["DECLARE @trancount INT = @@TRANCOUNT,",
    "\t@savepoint NVARCHAR(32) = '$1';",
    "BEGIN TRY",
    "\tIF @trancount = 0",
    "\t\tBEGIN TRANSACTION;",
    "\tELSE",
    "\t\tSAVE TRANSACTION @savepoint;",
    "",
    "\t$2;",
    "",
    "\tIF @trancount = 0",
    "\t\tCOMMIT TRANSACTION;",
    "END TRY",
    "BEGIN CATCH",
    "\tDECLARE @xact_state INT = XACT_STATE();",
    "\tIF @xact_state = -1",
    "\t\tROLLBACK TRANSACTION;",
    "\tIF @xact_state = 1 AND @trancount = 0",
    "\t\tROLLBACK TRANSACTION;",
    "\tIF @xact_state = 1 AND @trancount > 0",
    "\t\tROLLBACK TRANSACTION @savepoint;",
    "END CATCH"
    "$0"]))

(def if-else
  (snippet
   "ifelse" "IF and ELSE statements"
   ["IF ${1:condition}",
    "BEGIN",
    "\t${2:PRINT ''};",
    "END"
    "ELSE",
    "BEGIN",
    "\t${3:PRINT ''};",
    "END",
    "$0"]))


(comment
  (println (str/join \newline (:body (first (vals dragon)))))
  )

;; KoerberOneCore tables...

;; TODO - add primary/unique keys to each table.
;; then there'll be snippets like wloc -> WHERE loc.wh_id = $1 AND loc.location_id = $2
;; and for edges, could have wstoloc -> WHERE sto.wh_id = $1 AND sto.location_id = $2 <- maybe above suffices??
(def nodes
  [[:sto {:table "t_stored_item"}]
   [:loc {:table "t_location"}]
   [:hum {:table "t_hu_master"}]
   [:pkd {:table "t_pick_detail"}]
   [:itm {:table "t_item_master"}]
   [:orm {:table "t_order"}]
   [:ord {:table "t_order_detail"}]
   [:ldm {:table "t_load_master"}]
   [:wkq {:table "t_work_q"}]
   [:wkt {:table "t_work_q_types"}]
   [:wqa {:table "t_work_q_assignment"}]
   [:itu {:table "t_item_uom"}]
   [:car {:table "t_carrier"}]
   [:alo {:table "t_allocation"}]
   [:pom {:table "t_po_master"}]
   [:pod {:table "t_po_detail"}]
   [:rcp {:table "t_receipt"}]
   [:pkc {:table "t_pick_container"}]
   [:znl {:table "t_zone_loca"}]
   [:zon {:table "t_zone"}]
   [:trl {:table "t_tran_log"}]
   [:hld {:table "t_holds"}]
   [:stp {:table "t_stop"}]
   [:emp {:table "t_employee"}]
   [:pka {:table "t_pick_area"}]
   [:lkp {:table "t_lookup"}]
   [:trn {:table "t_transaction"}]
   [:mnu {:table "t_menu"}]
   [:rea {:table "t_reason"}]
   [:wvm {:table "t_wave_master"}]
   [:ppm {:table "t_pick_put_master"}]
   [:ppr {:table "t_pick_put_rules"}]
   [:ppd {:table "t_pick_put_detail"}] ;; FK to rules and master
   [:eil {:table "t_emp_input_log"}]
   [:pob {:db "REPOSITORY" :table "t_app_process_object"}]
   [:pobd {:db "REPOSITORY" :table "t_app_process_object_detail"}]
   [:clc {:db "REPOSITORY" :table "t_act_calculate"}]
   [:clcd {:db "REPOSITORY" :table "t_act_calculate_detail"}]
   [:db {:db "REPOSITORY" :table "t_act_database"}]
   [:dbd {:db "REPOSITORY" :table "t_act_database_detail"}]
   [:rsc {:db "REPOSITORY" :table "t_resource"}]
   [:rscd {:db "REPOSITORY" :table "t_resource_detail"}]
   [:app {:db "REPOSITORY" :table "t_application_development"}]])

(def edges
  [;; ppm
   ;; ppr
   ;; ppd
   [:ppd :ppr {:type :type
               :rule_id :rule_id}]
   [:ppd :ppm {:pick_put_id :pick_put_id}]
   ;; pob
   [:pob :app {:application_id :application_id}]
   ;; pobd
   [:pobd :pob {:id :id :version :version}]
   #_[:pobd :pob {:action_type "1"
                  :action_id :id}]
   [:pobd :clc {:action_type "3"
                :action_id :id}]
   [:pobd :db {:action_type "5"
               :action_id :id}]
   ;; clc
   [:clc :app {:application_id :application_id}]
   ;; clcd
   [:clcd :clc {:id :id :version :version}]
   [:clcd :rsc {[:operand1_id :operand2_id] :id}]
   ;; db
   [:db :app {:application_id :application_id}]
   ;; dbd
   [:dbd :db {:id :id :version :version}]
   ;; rsc
   [:rsc :app {:application_id :application_id}]
   ;; rscd
   [:rscd :rsc {:id :id :version :version}]
   ;; mnu
   [:mnu :pob {:process :name}]
   ;; sto
   [:sto :pkd {:type :pick_id}]
   [:sto :loc {:wh_id :wh_id
               :location_id :location_id}]
   [:sto :hum {:wh_id :wh_id
               :hu_id :hu_id}]
   [:sto :itm {:wh_id :wh_id
               :item_number :item_number}]
   ;; pka
   [:pka :wkt {:work_type :work_type}]
   ;; hld
   [:hld :sto {:sto_id :sto_id}]
   [:hld :rea {:reason_id :reason_id
               :reason_type :type}]
   [:emp :pka {:wh_id :wh_id
               :pick_area :pick_area}]
   ;; loc
   [:loc :emp {:type "F"
               :c1 :id}]
   [:loc :lkp {"t_location" :source
               "1033" :locale_id
               :type :text
               "TYPE" :lookup_type}]
   [:loc :pka {:wh_id :wh_id
               :pick_area :pick_area}]
   ;; hum
   ;; hum.control_number can potentially also join to ASN, receiver, or receipt identifier
   [:hum :loc {:wh_id :wh_id
               :location_id :location_id}]
   [:hum :orm {:wh_id :wh_id
               :control_number :order_number}]
   [:hum :ldm {:wh_id :wh_id
               :load_id :load_id}]
   [:hum :lkp {"t_hu_master" :source
               "1033" :locale_id
               :type :text
               "TYPE" :lookup_type}]
   ;; adding a cycle! Ah!
   [:hum :hum {:wh_id :wh_id
               :parent_hu_id :hu_id}]
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
               :line_number :line_number}]
   [:pkd :pkc {:wh_id :wh_id
               :container_id :container_id}]
   [:pkd :ldm {:wh_id :wh_id
               :load_id :load_id}]
   [:pkd :emp {:user_assigned :id}]
   [:pkd :wkt {:wh_id :wh_id
               :work_type :work_type}]
   [:pkd :lkp {"t_pick_detail" :source
               "1033" :locale_id
               :type :text
               "TYPE" :lookup_type}]
   [:pkd :pka {:wh_id :wh_id
               :pick_area :pick_area}]
   ;; itm
   [:itm :ppm {:pick_put_id :pick_put_id}]
   ;; itu
   [:itu :itm {:wh_id :wh_id
               :item_number :item_number}]
   [:itu :ppm {:pick_put_id :pick_put_id}]
   ;; orm
   [:orm :car {:carrier_id :carrier_id}] ;; some use {:wh_id :wh_id :carrier :carrier_code}
   [:orm :ldm {:wh_id :wh_id
               :load_id :load_id}]
   [:orm :lkp {:type_id :lookup_id}]
   ;; ord
   [:ord :orm {:order_id :order_id}]
   [:ord :itm {:wh_id :wh_id
               :item_number :item_number}]
   ;; ldm
   ;; stp
   [:stp :ldm {:wh_id :wh_id
               :load_id :load_id}]
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
   [:wkq :wkt {:wh_id :wh_id
               :work_type :work_type}]
   ;; wkt
   ;; wqa
   [:wqa :wkq {:wh_id :wh_id
               :work_q_id :work_q_id}]
   ;; car
   ;; alo
   [:alo :pkd {:pick_id :pick_id}]
   [:alo :itm {:wh_id :wh_id
               :item_number :item_number}]
   [:alo :loc {:wh_id :wh_id
               :pick_location :location_id}]
   [:alo :pka {:wh_id :wh_id
               :pick_area :pick_area}]
   [:alo :wkt {:wh_id :wh_id
               :work_type :work_type}]
   [:alo :rea {:hold_reason_id :reason_id}]
   [:alo :ppr {:pick_rule :rule
               "PICK" :type}]
   ;; pom
   [:pom :lkp {:type_id :lookup_id}]
   ;; pod
   [:pod :pom {:wh_id :wh_id
               :po_number :po_number}]
   [:pod :itm {:wh_id :wh_id
               :item_number :item_number}]
   ;; rcp
   [:rcp :hum {:wh_id :wh_id
               :hu_id :hu_id}]
   [:rcp :itm {:wh_id :wh_id
               :item_number :item_number}]
   [:rcp :pod {:wh_id :wh_id
               :po_number :po_number
               :line_number :line_number
               :schedule_number :schedule_number}]
   [:rcp :itu {:wh_id :wh_id
               :item_number :item_number
               :receipt_uom :itu}]
   ;; pkc
   [:pkc :hum {:wh_id :wh_id
               :container_label :hu_id}]
   ;; znl
   [:znl :loc {:wh_id :wh_id
               :location_id :location_id}]
   [:znl :zon {:wh_id :wh_id
               :zone :zone}]
   ;; zon
   ;; trl
   [:trl :itm {:wh_id :wh_id
               :item_number :item_number}]
   [:trl :hum {:wh_id :wh_id
               [:source_hu_id :destination_hu_id] :hu_id}]
   [:trl :loc {:wh_id :wh_id
               [:source_location_id :destination_location_id] :location_id}]
   [:trl :wkq {:wh_id :wh_id
               :work_q_id :work_q_id}]
   [:trl :trn {:tran_type :tran_type}]
   ;; eil
   [:eil :emp {:id :id}]
   [:eil :loc {:wh_id :wh_id
               :fork_id :location_id}]])


(def g
  (apply uber/digraph (into nodes edges)))


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
  (all-paths g :pkd)
  )

(defn paths-within
  [path]
  (map #(take (inc %) path) (range (count path))))


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
   g [:orm :ord {:wh_id :wh_id :order :order}])
  (edge->body
   g [:hum :hum {:wh_id :wh_id :parent_hu_id :hu_id}])
  (edge->body
   g [:loc :lkp {"t_location" :source
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
  (edges->snippet g :orm [[:orm :wkq {:wh_id :wh_id, :order_number :pick_ref_number}]
                          [:orm :car {:carrier_id :carrier_id}]
                          [:orm :ldm {:wh_id :wh_id, :load_id :load_id}]])

  (edges->snippet g :pod [[:pod :itm {:wh_id :wh_id, :order_number :pick_ref_number}]
                          [:itm :itu {:carrier_id :carrier_id}]])
  )


(defn invert-edge
  [[src dest join]]
  [dest src (set/map-invert join)])


(comment
  (invert-edge [:sto :pkd {:type :pick_id}])
  )


(defn table-paths
  [g node]
  (->> (all-paths g node)
       (mapcat paths-within)
       (filter #(> (count %) 1))
       (distinct)))

(comment
  (->> (table-paths g :pkd)
       (take 10)
       (map #(edges->snippet g :pkd %)))
  )

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
  (table-edges g :orm)
  )




;; TODO - probably should be snippet for generating the FROM, as well as the JOINs, in one snippet
;; *stopkd ->
;; FROM t_stored_item sto WITH (NOLOCK)
;; JOIN t_pick_detail pkd WITH (NOLOCK)
;;     ON sto.type = pkd.pick_id
(defn table-snippets
  [g node]
  (let [{:keys [db table]} (uber/attrs g node)
        paths (table-paths g node)]
    (->> (table-edges g node)
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
  (-> (uber/out-edges g :orm)
      (combo/subsets)
      (combo/cartesian-product (uber/in-edges g :orm)))
  )


(defn graph-snippets
  [g]
  (->> (uber/nodes g)
       (map #(table-snippets g %))
       (into {})))


(defn write-vscode-snippets
  [f]
  (json/generate-stream
   (conj (graph-snippets g)
         dragon dragon-cow if-else tran-snippet)
   (io/writer f)
   {:pretty true}))


(comment
  (write-vscode-snippets "out/sql.json")
  )


(comment
  (alg/bellman-ford g {:start-node :sto
                       :end-node :orm
                       :cost-fn (constantly 1)})

  (alg/bellman-ford g {:start-node :sto
                       :cost-fn (constantly 1)
                       :traverse false})

  (->> (alg/bf-traverse g :sto)
       (map #(alg/shortest-path g :sto %)))

  (alg/shortest-path g {:start-node :sto
                        :traverse true
                        :min-cost 1})

  (->> (alg/shortest-path g {:start-node :sto
                             :traverse true})
       (map alg/edges-in-path)
       (map #(mapv (juxt uber/src uber/dest) %)))

  (->> (alg/shortest-path g {:start-node :sto
                             :traverse true})
       (mapv alg/nodes-in-path))

  (alg/topsort g :sto)
  (alg/topsort g)

  (alg/nodes-in-path (alg/longest-shortest-path g :sto))

  (let [g (uber/add-attr g [:sto :pkd] :weight 2)]
    (uber/viz-graph
     (alg/paths->graph
      (alg/shortest-path
       g
       {:start-node :hld
        ;; :cost-fn (fn [edge] (or (uber/attr g edge :weight) 1)) ;; uncomment to use weight
        }))))

  (uber/weight g [:sto :pkd])

  (alg/nodes-in-path (alg/path-to (alg/shortest-path g {:start-node :sto}) :orm))

  (uber/edge? (uber/edge-description->edge g [:sto :pkd]))

  ;; for every subset, use those as :source-nodes to find shortest paths
  ;; to rest. That way, if subset is stohum, it'll go to orm through stohumorm
  ;; instead of stopkdorm

  (alg/shortest-path g {:start-node :sto})

  (->> (map #(vector % (alg/nodes-in-path (alg/longest-shortest-path g %))) (uber/nodes g)))

  (alg/all-destinations
   (alg/shortest-path g {:start-node :sto}))


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

  ;; graph
  (json/generate-stream
   (conj (graph-snippets g)
         dragon dragon-cow if-else tran-snippet)
   (io/writer "out/sql.json")
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

  (let [layouts [:dot
                 :neato :fdp :sfdp :twopi :circo]
        repository-nodes [] #_[:pob :pobd :clc :clcd :db :dbd :rsc :rscd :app]
        aad-g (apply uber/remove-nodes g repository-nodes)
        vg (reduce (fn [g node]
                     (let [{:keys [db table]} (uber/attrs g node)]
                       (uber/add-attrs
                        g node {:label (str "{" (name node) #_(when db (str "|" db)) "|" table "}")
                                :shape :Mrecord})))
                   aad-g (uber/nodes aad-g))]
    (doseq [layout layouts]
      (uber/viz-graph
       vg
       {:layout layout
        :save {:filename (str "images/" "graph-" (name layout) ".png")
               :format :png}})))


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

  (uber/find-edge g :a :c)
  )
