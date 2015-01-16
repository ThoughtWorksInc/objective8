(ns d-cent.proposals)

(defn request->proposal
  "Returns a map of a proposal if all the parts are in the
  request params. Otherwise returns nil"
  [{:keys [params]}]
  (let [proposal (select-keys params [:title :description :objectives])]
    (when (= 3 (count proposal)) proposal)))
