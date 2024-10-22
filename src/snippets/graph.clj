(ns snippets.graph
  (:require
   [ubergraph.core :as uber]
   [clojure.math.combinatorics :as combo]
   [clojure.set :as set]
   [ubergraph.alg :as alg]))


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
   [:wqa :emp {:user_assigned :id}]
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
   [:pkc :emp {:user_assigned :id}]
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


(def graph
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


(defn edge-description
  [g e]
  (let [[src dest attrs] (uber/edge-with-attrs g e)]
    [src (uber/node-with-attrs g dest) attrs]))


(defn path-description
  [g path]
  {:start (alg/start-of-path path)
   :end (alg/end-of-path path)
   :edges (mapv #(edge-description g %) (alg/edges-in-path path))})


(defn shortest-paths
  [g node]
  (let [;; shortest-path won't find self edges, so check for it manually
        self (uber/edge-description->edge g [node node])
        ps (alg/shortest-path
            g
            {:start-node node
             :traverse true
             :min-cost 1})]
    (into (if self
            [{:start node
              :end node
              :edges [(edge-description g self)]}]
            [])
          (map #(path-description g %))
          ps)))


(comment
  (shortest-paths graph :sto)
  (shortest-paths graph :wkq)
  (remove #(some (comp #{:wkq} first) (:edges %)) (shortest-paths graph :wqa))
  )


(def paths
  (sequence
   (mapcat #(shortest-paths graph %))
   (uber/nodes graph)))


(def node-descriptions
  (sequence
   (map #(uber/node-with-attrs graph %))
   (uber/nodes graph)))


(defn viz-graph
  ([]
   (viz-graph [:dot
               :neato :fdp :sfdp :twopi :circo]))
  ([layouts]
   (let [repository-nodes [] #_[:pob :pobd :clc :clcd :db :dbd :rsc :rscd :app]
         g (apply uber/remove-nodes graph repository-nodes)
         vg (reduce (fn [g node]
                      (let [{:keys [_db table]} (uber/attrs g node)]
                        (uber/add-attrs
                         g node {:label (str "{" (name node) #_(when db (str "|" db)) "|" table "}")
                                 :shape :Mrecord})))
                    g (uber/nodes g))]
     (doseq [layout layouts]
       (uber/viz-graph
        vg
        {:layout layout
         :save {:filename (str "images/" "graph-" (name layout) ".png")
                :format :png}})))))


(comment
  (viz-graph)
  )


;; ALL THIS BELOW DOWN HERE PROBABLY SUCKS


(defn paths-within
  [path]
  (map #(take (inc %) path) (range (count path))))


(defn table-paths
  [g node]
  (->> (all-paths g node)
       (mapcat paths-within)
       (filter #(> (count %) 1))
       (distinct)
       ))

(comment
  (table-paths graph :sto)
  )


(defn invert-edge
  [[src dest join]]
  [dest src (set/map-invert join)])


(comment
  (invert-edge [:sto :pkd {:type :pick_id}]))


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
  (table-edges graph :orm))


(comment
  (alg/bellman-ford graph {:start-node :sto
                           :end-node :orm
                           :cost-fn (constantly 1)})

  (alg/bellman-ford graph {:start-node :sto
                           :cost-fn (constantly 1)
                           :traverse false})

  (->> (alg/bf-traverse graph :sto)
       (map #(alg/shortest-path graph :sto %)))

  (alg/shortest-path graph {:start-node :sto
                            :traverse true
                            :min-cost 1})

  (->> (alg/shortest-path graph {:start-node :sto
                                 :traverse true})
       (map alg/edges-in-path)
       (map #(mapv (juxt uber/src uber/dest) %)))

  (->> (alg/shortest-path graph {:start-node :sto
                                 :traverse true})
       (mapv alg/nodes-in-path))

  (alg/topsort graph :sto)
  (alg/topsort graph)

  (alg/nodes-in-path (alg/longest-shortest-path graph :sto))

  (let [g (uber/add-attr graph [:sto :pkd] :weight 2)]
    (uber/viz-graph
     (alg/paths->graph
      (alg/shortest-path
       g
       {:start-node :hld
          ;; :cost-fn (fn [edge] (or (uber/attr g edge :weight) 1)) ;; uncomment to use weight
        }))))

  (uber/weight graph [:sto :pkd])

  (alg/nodes-in-path (alg/path-to (alg/shortest-path graph {:start-node :sto}) :orm))

  (uber/edge? (uber/edge-description->edge graph [:sto :pkd]))

    ;; for every subset, use those as :source-nodes to find shortest paths
    ;; to rest. That way, if subset is stohum, it'll go to orm through stohumorm
    ;; instead of stopkdorm

  (alg/shortest-path graph {:start-node :sto})

  (->> (map #(vector % (alg/nodes-in-path (alg/longest-shortest-path graph %))) (uber/nodes graph)))

  (alg/all-destinations
   (alg/shortest-path graph {:start-node :sto}))


  (uber/viz-graph graph {:auto-label false
                         :save {:filename "graph.png"
                                :format :png}})


  (->> (uber/nodes graph)
       (mapcat #(table-paths graph %))
       (set)
       (count))
  )


(comment

  (->> (alg/shortest-path graph {:start-node :a
                                 :traverse true
                                 :cost-attr :weight})
       (map alg/edges-in-path))

  (alg/edges-in-path (alg/shortest-path graph :a :c :weight))
  (alg/pprint-path (alg/shortest-path graph :a :c :weight))

  (uber/out-edges graph :a)

  (alg/connected-components graph)

  (alg/bf-span graph :a)
  (alg/bf-traverse graph :a)

  (alg/pre-span graph :a)

  (uber/edges graph)

  (->> (uber/nodes graph)
       (map
        #(->> [% (uber/neighbors graph %)])))

  (uber/out-edges graph :a)

  (uber/find-edge graph :a :c)
  )
