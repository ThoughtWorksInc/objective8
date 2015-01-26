(ns d-cent.objectives)

(defn request->objective
  "Returns a map of an objective if all the parts are in the
  request params. Otherwise returns nil"
  [{:keys [params]}]
  (let [objective (select-keys params [:title :actions :description])]
    (when (= 3 (count objective)) objective)))
