{:user {:source-paths ["dm"]
        :repl-options {:init-ns user}
        :dependencies [[alembic "0.3.2"]
                       [org.clojure/tools.namespace "0.2.9"]]
        :plugins [[com.palletops/lein-shorthand "0.4.0"]
                  [cider/cider-nrepl "0.9.0-SNAPSHOT"]]
        :shorthand {. [alembic.still/distill alembic.still/lein]}}}
