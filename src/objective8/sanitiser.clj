(ns objective8.sanitiser
  (:require [clojure.string :as string])
  (:import [org.owasp.html HtmlSanitizer
                        AttributePolicy
                        ElementPolicy
                        HtmlPolicyBuilder
                        HtmlSanitizer$Policy
                        PolicyFactory
                        Sanitizers]
                           ))
;(-> Sanitizers/BLOCKS (.and Sanitizers/FORMATTING) (.and Sanitizers/IMAGES) (.and Sanitizers/LINKS) (.and Sanitizers/STYLES))
(defn sanitise-html [html]
  (let [policy (-> (HtmlPolicyBuilder.)
                   (.allowElements  (into-array String ["html" "body"]))
                   (.allowCommonBlockElements )
                   (.allowCommonInlineFormattingElements)
                   (.allowStyling)
                   (.toFactory))]
    (.sanitize policy html)))
