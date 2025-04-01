(ns snippets.generate
  (:require
   [clojure.string :as str]))


(defrecord Snippet [prefix description body])


(defn value->str
  [table-alias f]
  (cond
    (coll? f) (str "(" (str/join ", " (mapv #(value->str table-alias %) f)) ")")
    (string? f) (str \' (str/replace f #"'" "''") \')
    (keyword? f) (str (name table-alias) "." (name f))))


(defn clause->str
  ([src dest fs]
   (clause->str src dest fs nil))
  ([src dest [f1 f2] collate?]
   (let [f1-coll? (coll? f1)]
     (str (if f1-coll?
            (value->str dest f2)
            (value->str src f1))
          (when collate?
            (str " COLLATE DATABASE_DEFAULT"))
          (if (some coll? [f1 f2])
            " IN "
            " = ")
          (if f1-coll?
            (value->str src f1)
            (value->str dest f2))))))


(comment
  (clause->str :trl :hum [[:source_hu_id :destination_hu_id] :hu_id])
  (clause->str :trl :hum [:wh_id :wh_id])
  (clause->str :loc :lkp ["t_location" :source])
  (clause->str :hum :hum2 [:parent_hu_id :hu_id]))


(defn table-source
  ([table]
   table)
  ([db table]
   (str (when db (str db "..")) table))
  ([db schema table]
   (str db "." schema "." table)))


(defn edge->body
  ([src dest join-map]
   (edge->body src dest join-map))
  ([src [dest {:keys [db table]}] join-map collate?]
   (let [dest (if (= src dest)
                (keyword (str (name dest) "2"))
                dest)]
     (concat [(str "JOIN " (table-source db table) " " (name dest) " WITH (NOLOCK)")]
             (map str
                  (cons "\tON " (repeat "\tAND "))
                  (map #(clause->str src dest % collate?) join-map))))))


(defn edge->semi-join-body
  [src [dest {:keys [db table]}] join-map collate?]
  (let [dest (if (= src dest)
               (keyword (str (name dest) "2"))
               dest)]
    (concat ["EXISTS ("
             "\tSELECT 1"
             (str "\tFROM " (table-source db table) " " (name dest) " WITH (NOLOCK)")
             "\tWHERE"]
            (map str
                 (cons "\t\t" (repeat "\t\tAND "))
                 (map #(clause->str src dest % collate?) join-map))
            [")"
             "$0"])))


(defn edge->apply-join-body
  [src [dest {:keys [db table]}] join-map collate?]
  (let [dest (if (= src dest)
               (keyword (str (name dest) "2"))
               dest)
        dest-keys (vals join-map)
        group-bys
        (-> []
            (into (map #(str "\t\t" (value->str dest %) ",")) (butlast dest-keys)) ;; trailing commas
            (conj (str "\t\t" (value->str dest (last dest-keys)))) ;; no trailing comma
            )]
    (concat ["CROSS APPLY ("
             "\tSELECT"
             "\t\t${1:COUNT(*)}"
             (str "\tFROM " (table-source db table) " " (name dest) " WITH (NOLOCK)")
             "\tWHERE"]
            (map str
                 (cons "\t\t" (repeat "\t\tAND "))
                 (map #(clause->str src dest % collate?) join-map))
            ["\tGROUP BY"]
            group-bys
            [(str ") " (name dest))
             "$0"])))


(comment
  (edge->body
   :orm
   [:ord {:table "t_order_detail"}]
   {:wh_id :wh_id
    :order_number :order_number})
  (edge->body
   :hum
   [:hum {:table "t_hu_master"}]
   {:wh_id :wh_id
    :parent_hu_id :hu_id})
  (edge->body
   :loc
   [:lkp {:table "t_lookup"}]
   {"t_location" :source
    "1033" :locale_id
    :type :text
    "TYPE" :lookup_type})
  (edge->semi-join-body
   :loc
   [:sto {:table "t_stored_item"}]
   {:wh_id :wh_id
    :location_id :location_id}
   false)
  )


(defn edges->description
  [start edges]
  (str "Join an existing " (name start) " table to "
       (str/join
        ", "
        (eduction
         (map
          (fn [[src [dest _] _]]
            (if (= src start)
              (name dest)
              (str (name src) " to " (name dest)))))
         edges))))


(defn join-prefix
  [start dests]
  (str/join (eduction (map name) (into [start] dests))))


(defn join-snippet
  [{:keys [start dests edges]}]
  (let [prefix (join-prefix start dests)
        description (edges->description start edges)
        body (conj (into [] (mapcat #(apply edge->body %)) edges) "$0")]
    (->Snippet prefix description body)))


(defn semi-join-snippet
  [[start [dest dest-attrs] edge collate?]]
  (let [prefix (str (name start) (name dest) "sj")
        description (str "Semi join an existing " (name start) " table to " (name dest))
        body (edge->semi-join-body start [dest dest-attrs] edge collate?)]
    (->Snippet prefix description body)))


(defn apply-join-snippet
  [[start [dest dest-attrs] edge collate?]]
  (let [prefix (str (name start) (name dest) "aj")
        description (str "Apply join an existing " (name start) " table to " (name dest))
        body (edge->apply-join-body start [dest dest-attrs] edge collate?)]
    (->Snippet prefix description body)))


;; TODO!
;; (defn outer-apply-snippet
;;   [{:keys [start [dest] edges]}]
;;   (let [prefix (str (name start) (name dest) "aj")
;;         description (str "OUTER APPLY an existing" (name start) " table to " (name dest))
;;         body ""]))


(defn node-snippet
  [[node {:keys [db table]}]]
  (let [alias (name node)]
    {:prefix alias
     :description (str "Table " table " with alias: " alias)
     :body [(str "FROM " (table-source db table) " " alias " WITH (NOLOCK)")
            "$0"
            ;; for t_tran_log, add in an automatic ORDER BY trl.tran_log_id DESC
            ]}))
