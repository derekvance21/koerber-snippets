(ns snippets.combo
  (:require
   [clojure.math.combinatorics :as combo]))


(defn indexed
  [rf]
  (let [i (volatile! -1)]
    (fn
      ([] (rf))
      ([result] (rf result))
      ([result _]
       (rf result (vswap! i inc))))))


(defn subsets
  "all subsets of max length n in coll"
  ([coll]
   (subsets coll nil))
  ([coll max]
   (subsets coll nil max))
  ([coll min max]
   (eduction
    indexed
    (map inc)
    (if max (take max) identity)
    (if min (drop (dec min)) identity)
    (mapcat #(combo/combinations coll %))
    coll)))


(defn permuted-subsets
  [coll & opts]
  (sequence
   (mapcat combo/permutations)
   (apply subsets coll opts)))


(comment
  (subsets [1 2 3])
  (subsets [1 2 3] 2)
  (subsets [1 2 3] 2 3)
  (subsets (range 10) 2)

  (count (into [] (mapcat combo/permutations) (subsets (range 33) 2)))

  (count (combo/permuted-combinations (range 33) 3))
  (take-last 5 (combo/permuted-combinations (range 33) 2))

  (combo/count-combinations (range 7) 3)
  
  (count (combo/permuted-combinations (range 7) 3))
  (count (combo/permuted-combinations (range 12) 2))

  (transduce #_(map combo/count-permutations)
   (map (constantly 1))
             +
             (subsets (range 12) 4))

  (combo/count-subsets (range 12))
  (count (sequence (subsets (range 12) 4)))

  (into [] (comp 
            (map inc)
            (map #(combo/count-combinations (range 12) %))) (range 12))
  
  (combo/count-combinations (range 12) 0)
  
  (count
   (into []
         (comp
          (mapcat combo/permutations)
          (distinct))
         (subsets (range 33) 3)))

  (count (distinct (subsets (range 33) 4)))
  (count (into [] (into [] (subsets (range 33) 3))))

  (combo/partitions (range 4))



  (combo/permuted-combinations [1 2 3] 2)

  (let [coll [1 2 3]]
    (mapcat combo/permutations (subsets coll 2)))
  )
