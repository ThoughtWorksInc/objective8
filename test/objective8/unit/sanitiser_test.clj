(ns objective8.unit.sanitiser-test
  (:require [midje.sweet :refer :all]
            [objective8.sanitiser :refer :all]))

(fact "sanitiser gets rid of scripts in html"
      (sanitise-html "<h1>Title</h1><script>EVIL</script>") => "<h1>Title</h1>")

(fact "sanitiser allows html and body tags" 
      (sanitise-html "<html><body>BODY</body></html>") => "<html><body>BODY</body></html>")

      


