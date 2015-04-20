#Objective8 Refactoring and Style Guide

##Keep the left hand side of let statements simple

###Prefer

```Clojure

(let [username (:username (get-user-info 123))
      last-log-in (:last-online (get-user-stats 123))]
  (welcome-message username last-log-in))

```

###To

```Clojure

(let [{username :username} (get-user-info 123)
      {last-log-in :last-online} (get-user-stats 123)]
  (welcome-message username last-log-in))

```

##Replace state building in let statements with threading

Use the `->` or `some->` macros when the desired effect is transformation.

###Replace

```Clojure

(defn perform-action [a b c]
  (let [one (create-one a)
        two (create-two-from-one one)
        three (create-three-from-two two)]
    (make-result three b c)))

```

###With

```Clojure

(defn perform-action [a b c]
  (-> (create-one a)
      create-two-from-one
      create-three-from-two
      (make-result b c)))

```

