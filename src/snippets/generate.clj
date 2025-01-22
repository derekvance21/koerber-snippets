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


(defn node-snippet
  [[node {:keys [db table]}]]
  (let [alias (name node)]
    {:prefix alias
     :description (str "Table " table " with alias: " alias)
     :body [(str "FROM " (table-source db table) " " alias " WITH (NOLOCK)")
            "$0"
            ;; for t_tran_log, add in an automatic ORDER BY trl.tran_log_id DESC
            ]}))
