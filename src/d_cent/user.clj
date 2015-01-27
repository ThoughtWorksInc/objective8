(ns d-cent.user)

(defn find-email-address-for-user [store user-id]
  (get-in @store [user-id :email-address])) 

(defn store-email-address-for-user! [store user-id email-address]
  (swap! store assoc-in [user-id :email-address] email-address)
  store)
