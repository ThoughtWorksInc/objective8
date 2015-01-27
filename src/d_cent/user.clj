(ns d-cent.user)

(defn find-email-address-for-user [store user-id]
  (get-in store [user-id :email-address])) 










