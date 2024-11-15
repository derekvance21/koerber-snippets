(ns snippets.graph
  (:require
   [ubergraph.core :as uber]
   [clojure.set :as set]
   [snippets.defaults :as d]
   [ubergraph.alg :as alg]))


;; subgraph of g with only nodes
#_(defn induced
  [g nodes]
  ())




(def aad-nodes
  [[:sch {:table "t_schema_history"}]
   [:ctl {:table "t_control"}]
   [:whs {:table "t_whse_control"}]
   [:sto {:table "t_stored_item"}]
   [:loc {:table "t_location"}]
   [:hum {:table "t_hu_master"}]
   [:pkd {:table "t_pick_detail"}]
   [:itm {:table "t_item_master"}]
   [:orm {:table "t_order"}]
   [:ord {:table "t_order_detail"}]
   [:ldm {:table "t_load_master"}]
   [:wkq {:table "t_work_q"}]
   [:wkt {:table "t_work_types"}]
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
   [:rea {:table "t_reason"}]
   [:wvm {:table "t_wave_master"}]
   [:ppm {:table "t_pick_put_master"}]
   [:ppr {:table "t_pick_put_rules"}]
   [:ppd {:table "t_pick_put_detail"}]
   [:eil {:table "t_emp_input_log"}]
   [:mnu {:table "t_menu"}]])

;; lkp should be a directed edge
(def aad-edges
  [;; ppm
   ;; ppr
   ;; ppd
   [:ppd :ppr {:type :type
               :rule_id :rule_id}]
   [:ppd :ppm {:pick_put_id :pick_put_id}]
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
   [:loc :emp {:c1 :id}]
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
   #_[:hum :hum {:wh_id :wh_id
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
   ;; TODO I want to make all these have greater than 1 weight!
   ;; This shouldn't be winning in a tie!
   [:trl :itm {:wh_id :wh_id
               :item_number :item_number} {:weight 1.1}]
   [:trl :hum {:wh_id :wh_id
               [:source_hu_id :destination_hu_id] :hu_id} {:weight 1.1}]
   [:trl :loc {:wh_id :wh_id
               [:source_location_id :destination_location_id] :location_id} {:weight 1.1}]
   [:trl :wkq {:wh_id :wh_id
               :work_q_id :work_q_id} {:weight 1.1}]
   [:trl :trn {:tran_type :tran_type} {:weight 1.1}]
   [:trl :orm {:wh_id :wh_id
               :outbound_order_number :order_number} {:weight 1.1}]
   [:trl :pkd {:pick_id :pick_id} {:weight 1.1}]
   ;; eil
   [:eil :emp {:id :id}]
   [:eil :loc {:wh_id :wh_id
               :fork_id :location_id}]])


(defn assoc-db
  [db nodes]
  (mapv
   (fn [[n attrs]]
     [n (assoc attrs :db db)])
   nodes))



;; KoerberOneCore tables...
(def koerber-one-core-nodes
  (assoc-db
   "KoerberOneCore"
   [[:usr {:table "[User]"}]
    [:uic {:table "UserIdentityClaim"}]
    [:icl {:table "IdentityClaim"}]
    [:rol {:table "[Role]"}]
    [:url {:table "UserRoles"}]
    [:ric {:table "RoleIdentityClaim"}]]))

(def koerber-one-core-edges
  [[:uic :usr {:UserId :Id}]
   [:uic :icl {:IdentityClaimId :Id}]
   [:url :usr {:UserId :Id}]
   [:url :rol {:RoleId :Id}]
   [:ric :rol {:RoleId :Id}]
   [:ric :icl {:IdentityClaimId :Id}]
   [:usr :emp {:LogOnName :id} {:collate? true}]])

(def repository-nodes
  (assoc-db
   "REPOSITORY"
   [[:pob {:table "t_app_process_object"}]
    [:pobd {:table "t_app_process_object_detail"}]
    [:clc {:table "t_act_calculate"}]
    [:clcd {:table "t_act_calculate_detail"}]
    [:db {:table "t_act_database"}]
    [:dbd {:table "t_act_database_detail"}]
    [:rsc {:table "t_resource"}]
    [:rscd {:table "t_resource_detail"}]
    [:apd {:table "t_application_development"}]]))

(def repository-edges
  [;; pob
   [:pob :apd {:application_id :application_id}]
   ;; pobd
   [:pobd :pob {:id :id :version :version}]
   #_[:pobd :pob {;; :action_type "1"
                  :action_id :id}]
   [:pobd :clc {;; :action_type "3"
                :action_id :id}]
   [:pobd :db {;; :action_type "5"
               :action_id :id}]
   ;; clc
   [:clc :apd {:application_id :application_id}]
   ;; clcd
   [:clcd :clc {:id :id :version :version}]
   [:clcd :rsc {[:operand1_id :operand2_id] :id}]
   ;; db
   [:db :apd {:application_id :application_id}]
   ;; dbd
   [:dbd :db {:id :id :version :version}]
   ;; rsc
   [:rsc :apd {:application_id :application_id}]
   ;; rscd
   [:rscd :rsc {:id :id :version :version}]
   ;; mnu
   [:mnu :pob {:process :name}]])


(def adv-nodes
  (assoc-db
   "ADV"
   [[:app {:table "t_application"}]
    [:dev {:table "t_device"}]
    [:dvt {:table "t_device_type"}]
    [:sol {:table "t_solution"}]
    [:srv {:table "t_server"}]
    [:lgm {:table "t_log_message"}]]))

(def adv-edges
  [[:dev :pob {:process_object_id :id}]
   [:dev :dvt {:device_type_id :device_type_id}]
   [:dev :sol {:solution_id :solution_id}]
   [:sol :apd {:application_id :application_id}]
   [:sol :srv {:server_id :server_id}]
   [:lgm :emp {:user_id :id}]
   ])

(defn edge->init
  [[src dest join attrs]]
  [src dest (merge {:weight 1} attrs {:join join})])


(def schema
  (let [nodes (into [] cat [aad-nodes koerber-one-core-nodes repository-nodes adv-nodes])
        edges (into [] (comp cat (map edge->init)) [aad-edges koerber-one-core-edges repository-edges adv-edges])
        inits (into nodes edges)]
    (apply uber/graph inits)))


(defn edge-description
  [g e]
  (let [[src dest {:keys [join collate?]}] (uber/edge-with-attrs g e)
        mirror? (uber/mirror-edge? e)]
    [src
     (uber/node-with-attrs g dest)
     (if mirror?
       (set/map-invert join)
       join)
     collate?]))


(defn shortest-paths-to-destinations
  ([g node dests]
   ;; TODO - you might want to even decrement max-jumps here. Some of these joins are kind of crazy.
   ;; if you decrement, then no jumps would be allowed
   ;; or you could just change the <= in the every? form to <
   (shortest-paths-to-destinations g node dests (- d/max-join-length (count dests))))
  ([g node dests max-jumps]
   (let [dest-set (set dests)
         cost-fn (fn [e] (if (contains? dest-set (uber/dest e))
                           0
                           (uber/attr g e :weight)))
         edge-filter (fn [e]
                       ;; lkp can't be in the middle of a path
                       (let [src (uber/src e)
                             dest (uber/dest e)]
                         (and
                          (or (= node :lkp)
                              (not= :lkp src))
                          (or (= node :trl)
                              (not= :trl src)
                              (= :trn dest)))))
         ps (alg/shortest-path g {:start-node node
                                  :cost-fn cost-fn
                                  :edge-filter edge-filter})
         paths (into [] (map #(alg/path-to ps %)) dests)]
     ;; all the paths to each dest in dests need to be under the max-jumps
     ;; TODO maybe also the sum of the path costs needs to be under max-jumps?
     ;; but really that's something I need to communicate with the shortest-path calculation...
     (when (every? #(when-let [cost (:cost %)]
                      (<= cost max-jumps)) paths)
       paths))))


(defn edges-to-destinations
  [g node dests]
  (let [paths-descriptions (shortest-paths-to-destinations g node dests)]
    (into
     []
     (comp
      (mapcat alg/edges-in-path)
      (distinct)
      (map #(edge-description g %)))
     (sort-by :cost paths-descriptions) ;; short paths should be first in the join
     )))


(comment
  (shortest-paths-to-destinations schema :sto [:loc :orm])
  (shortest-paths-to-destinations schema :uic [:usr])

  (edges-to-destinations schema :sto [:loc :orm])
  (edges-to-destinations schema :sto [:loc :orm])
  
  (edges-to-destinations schema :hum [:pkd :wkq])
  (shortest-paths-to-destinations schema :hum [:pkd :wkq])
  (shortest-paths-to-destinations schema :zon [:loc :alo])
  (shortest-paths-to-destinations schema :hld [:sto :car])
  (shortest-paths-to-destinations schema :hld [:sto :itm])
  (edges-to-destinations schema :zon [:alo])
  (edges-to-destinations schema :zon [:loc :alo :sto :pkd :itm :ppm])
  (edges-to-destinations schema :zon [:loc :alo :ppm])
  (edges-to-destinations schema :ppm [:hld])
  (edges-to-destinations schema :sto [:emp])
  (edges-to-destinations schema :sto [:loc :emp])
  (edges-to-destinations schema :pkd [:emp :loc :ppm])
  (edges-to-destinations schema :pkd [:loc :ord :orm])
  (edges-to-destinations schema :sto [:orm :loc :emp])
  (edges-to-destinations schema :sto [:pkd :hum])
  (edges-to-destinations schema :alo [:orm :emp])
  (edges-to-destinations schema :alo [:orm :emp])
)


(def schema-nodes
  (uber/nodes schema))


(def node-descriptions
  (sequence
   (map #(uber/node-with-attrs schema %))
   (uber/nodes schema)))


(defn viz-graph
  ([]
   (viz-graph [:dot :neato :fdp :sfdp :twopi :circo]))
  ([layouts]
   (let [repository-nodes [] #_[:pob :pobd :clc :clcd :db :dbd :rsc :rscd :apd]
         g (apply uber/remove-nodes schema repository-nodes)
         vg (reduce (fn [g node]
                      (let [{:keys [db table]} (uber/attrs g node)]
                        (uber/add-attrs
                         g node {:label (str "{" (name node) (when db (str "|" db)) "|" table "}")
                                 :shape :Mrecord})))
                    g (uber/nodes g))]
     (doseq [layout layouts]
       (uber/viz-graph
        vg
        {:layout layout
         :save {:filename (str "images/" "graph-" (name layout) ".png")
                :format :png}})))))


(comment
  (viz-graph [:dot])

  )

