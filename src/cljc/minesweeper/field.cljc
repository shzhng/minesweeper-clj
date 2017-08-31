(ns minesweeper.field
  (:import clojure.lang.PersistentQueue))

(defn create-and-rig-field
  [nrows ncols nmines]
  ; TODO make this a reader conditional
  ;(if (< (* nrows ncols) nmines) (throw (Exception. "more mines than tiles")))
  (let [tiles (repeat (* nrows ncols) {:contains-mine?     false
                                       :exposed?           false
                                       :marked?            false
                                       :num-neighbor-mines 0})]

    (->> (concat (map #(assoc % :contains-mine? true) (take nmines tiles))
                 (drop nmines tiles))
         (shuffle)
         (partition nrows)
         (map vec)
         (vec))))

(defn get-neighboring-pos
  [x y ncols nrows]
  (let [around-x (remove nil? [(if (> x 0) (dec x)) x (if (< x (dec ncols)) (inc x))])
        around-y (remove nil? [(if (> y 0) (dec y)) y (if (< y (dec nrows)) (inc y))])]
    (for [x around-x y around-y] [x y])))

(defn mark-neighbors
  [nrows ncols field pos]
  (let [x (first pos)
        y (second pos)
        neighbors (get-neighboring-pos x y ncols nrows)]
    (if (get-in field (conj pos :contains-mine?))
      (reduce #(update-in %1
                          (conj %2 :num-neighbor-mines)
                          inc)
              field neighbors)
      field)))

(defn tally-neighbors
  [nrows ncols field]
  (let [all-pos (for [x (range ncols) y (range nrows)] [x y])]
    (reduce (partial mark-neighbors nrows ncols)
            field
            all-pos)))

(defn get-field
  [nrows ncols nmines]
  (let [field (create-and-rig-field nrows ncols nmines)]
    (tally-neighbors nrows ncols field)))

(defn mark-tile
  [[x y] field]
  (assoc-in field [x y :marked?] true))

(defn queue-more
  [field [x y] queue]
  (let [tile (get-in field [x y])
        ncols (count field)
        nrows (count (get field 1))]
    (if (not= 0 (tile :neighbor-mines))
      queue
      (conj queue (get-neighboring-pos x y ncols nrows)))))

(defn bfs-vacant
  ([field pos]
   (let [queue (conj (PersistentQueue/EMPTY) pos)
         traversed #{}]
     (bfs-vacant field queue traversed)))
  ([field queue traversed]
   (let [curr (peek queue)
         queue (pop queue)]
     (if (nil? curr)
       ; we've recursively opened a field of empty tiles
       field
       (if (contains? traversed curr)
         ; move on, not our first rodeo
         (bfs-vacant field queue traversed)
         (let [field (assoc-in field (conj curr :exposed?) true)
               queue (queue-more field curr queue)
               traversed (conj traversed curr)]
           (bfs-vacant field queue traversed)))))))



