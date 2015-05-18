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
(defn sanitise-html [html]
  (let [policy 
        ;(-> (HtmlPolicyBuilder.) (.allowCommonBlockElements) (.allowCommonInlineFormattingElements) (.allowStyling) (.toFactory))
        (-> Sanitizers/BLOCKS (.and Sanitizers/FORMATTING) (.and Sanitizers/LINKS) (.and Sanitizers/STYLES))]
    (.sanitize policy html)))
