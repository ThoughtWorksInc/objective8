(ns d-cent.storage)

(defn gen-uuid [] (str (java.util.UUID/randomUUID)))

(def store (atom {}))

(defn retrieve
  "Retrieves a record from the in memory database"
  [collection id]
  (first (filter #(= (:_id %) id) (get @store collection))))

(defn store! 
  "Stores a record in the in memory database"
  [collection record]
  (let [record-to-save (assoc record :_id (gen-uuid))]
    (swap! store update-in [collection] conj record-to-save)
    record-to-save))

