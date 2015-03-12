(ns objective8.translation
  (:require [clojure.tools.logging :as log]
            [taoensso.tower :as tower]))

;;TODO move translations to external files

(def english-translations
  {:base {:header-logo-text "Objective[8]"
          :header-logo-title "Go to home page"
          :project-status! "ALPHA: We are still testing, <a href=\"/project-status\" title=\"Find out more about our project status\">find out more</a>"
          :browsehappy! "You are using an <strong>outdated</strong> browser. Please <a href=\"http://browsehappy.com/\">upgrade your browser</a> to improve your experience."}
   :navigation-global {:sign-in-text "Sign in"
                       :sign-in-title "Go to sign in"
                       :sign-out-text "Sign out"
                       :sign-out-title "Go to sign out"
                       :profile-text "Profile"
                       :profile-title "Go to user profile"}
   :share-widget {:title "Share this page:"
                  :facebook-title "Share on Facebook..."
                  :facebook-text "Share on Facebook"
                  :google-plus-title "Plus one this page on Google"
                  :google-plus-text "Google +1"
                  :twitter-title "Share this page on Twitter"
                  :twitter-text "Tweet"
                  :linkedin-title "Share this page on LinkedIn"
                  :linkedin-text "Share on LinkedIn"
                  :reddit-title "Submit this page to Reddit"
                  :reddit-text "Submit to Reddit"
                  :url-label "URL to share"
                  :url-title "The link to this page"}
   :error-404 {:doc-title "Error 404 | Objective[8]"
               :doc-description "Sorry the page you requested can't be found."
               :page-title "Sorry the page you requested can't be found."
               :page-intro "The page you were trying to reach at this address doesn't seem to exist. This is usually the result of a bad or outdated link. We apologise for any inconvenience."
               :page-content! "<h2>What can I do now?</h2><ul class=\"list-large\"><li>Please return to the <a href=\"/\" title=\"Go to Home\">home page</a>.</li><li>To report an issue visit our <a href=\"https://github.com/ThoughtWorksInc/objective8/issues\">github issues page</a>.</li></ul>" }
   :notifications {:drafting-has-started "Drafting has started on this objective!"
                   :drafting-started-helper-text "This means that you can no longer ask or answer questions, comment, or invite new writers.  However, you can follow and contribute to the drafting progress "
                   :go-to-drafting "here."}
   :index {:doc-title "Objective[8]"
           :doc-description ""
           :index-welcome "Collaborative policy making for democratic organisations."
           :index-intro "Gather community opinion, generate ideas, share, discuss, vote and collaborate with experts to draft new policy."
           :index-get-started "Objectives"
           :index-get-started-title "Objectives"
           :index-learn-more "Learn more"
           :index-learn-more-title "Learn more"}
   :sign-in {:doc-title "Sign in or Sign up | Objective[8]"
             :doc-description "Sign in or Sign up to Objective [8]"
             :page-title "Sign in or Sign up"
             :twitter-sign-in-btn "Sign in with twitter"
             :twitter-sign-in-title "Sign in with twitter"
             :reassurance "We will never post to Twitter without your permission."}
   :project-status {:doc-title "Project status | Objective[8]"
                    :doc-description ""
                    :page-title "Alpha phase"
                    :page-intro "We are developing and designing Objective[8] in the open. This allows the tool evolve as you use it, as you give feedback, and as the developers update and add content."
                    :page-content! "<p>It is important to note that during this time data may be removed or destroyed as part of our process. Any of the views and data expressed here is to be used for example purposes only and does not accurately represent the real views of users.</p><h2>How to get involved</h2><p>This project is open source and we are inviting people to collaborate with us to build a better tool. For developers and you can find the code on github at <a href=\"https://github.com/ThoughtWorksInc/objective8\" title=\"Objective[8] on github\">https://github.com/ThoughtWorksInc/objective8</a>. We will also be adding details for upcoming usability sessions shortly.</p><h2>Have you got an idea?</h2><p>We are really interested in user feedback and are currently inviting people to leave comments our <a href=\"https://github.com/ThoughtWorksInc/objective8/issues\">github issues page</a>.</p>" }
   :learn-more {:doc-title "Learn more | Objective[8]"
                :doc-description ""
                :page-title "Objective[8] 101"
                :sub-title "What are the basics?"
                :page-intro "Objective8 is a platform to enable collaborative policy making for democratic organisations."
                :page-content! "
                                  <p><b>Policy</b> is used by governments and organisations to make consistent and fair decisions in order to achieve desired outcomes.</p>
           
                                  <p>An <b>objective</b> is a change that could be achieved by introducing new policy.  For example: improving access to public housing, or increasing the safety of our roads.</p>
               
                                  <p><b>Writers</b> work with each other and the community to draft policy.  They use the crowdsourced questions, answers and comments to produce <b>policy drafts</b><span class=\"not-yet-implemented-footnote-marker\">*</span>.  These are further refined through cycles of feedback and redrafting.</p>

                                  <p>You can get involved and have your say by creating and sharing objectives, offering your opinion through <b>comments</b>, asking and answering <b>questions</b>, suggesting policy writers, or even providing feedback on the policy drafts themselves.  The whole process is open and transparent.</p>
                                  
                                  <p class=\"helper-text\"> <span class=\"not-yet-implemented-footnote-marker\">*</span>As the site is still under development, these features have not yet been implemented. Check back soon!</p> "
                :get-started-button-title "Get started"
                :get-started-button-text "Get started"}
   :objective-nav {:title "Navigation for"
                   :details "Details"
                   :questions "Questions"
                   :writers "Writers"}  
   :objective-list {:doc-title "Objectives | Objective[8]"
                    :doc-description ""
                    :page-title "Objectives"
                    :create-button-title "Create an objective"
                    :create-button-text "Create an objective"
                    :please "Please"
                    :sign-in "sign in"
                    :to "to create an objective."
                    :subtitle "Recently created objectives"
                    :drafting-begins "Drafting begins on"
                    :objectives-intro-text "Objective[8] centers its policy drafting process around objectives. An objective is a change that could be achieved by introducing new policy." }
   :objective-create {:doc-title "Create an Objective | Objective[8]"
                      :doc-description "Create an Objective ..."
                      :page-title "Create an objective"
                      :page-intro "An objective is a change that could be achieved by introducing new policy. For example: improving access to public housing, or increasing the safety of our roads."
                      :headline-label "Headline"
                      :headline-title "3 characters minimum, 120 characters maximum"
                      :headline-prompt "A headline should encourage contribution or discussion"
                      :goals-label "Goals"
                      :goals-prompt "A goal is what you’d like your change to achieve"
                      :goals-title "50 characters minimum, 200 characters maximum"
                      :background-label "Background"
                      :background-prompt "Provide further information on why achieving this objective is important or necessary"
                      :background-title "Enter optional background text up to 1000 characters"
                      :end-date-label "Drafting start date"
                      :end-date-prompt "On this date, the drafting phase begins. You should give participants enough time to generate useful information, and to recruit and vote for potential policy writers."
                      :drafting-not-yet-implemented "(Please note: since this site is still under development, the drafting phase has not yet been implemented)"
                      :end-date-title "Please enter an end date"
                      :end-date-placeholder"yyyy-mm-dd"
                      :submit "Create"}
   :objective-view {:doc-title "Objective | Objective[8]"
                    :doc-description "Objective  ..."
                    :background-label "Background"
                    :goals-label "Goals"
                    :end-date-label "Drafting begins: "
                    :created-message "Your objective has been created!"}
   :comment-view {:created-message "Your comment has been added!"
                  :comment-title "Comments"
                  :no-comments "There are no comments yet."}
   :comment-create {:comment-label "Your comment"
                    :comment-title "Comments are a maximum length of 500 characters"
                    :post-button "Add comment"}
   :comment-sign-in {:please "Please"
                     :sign-in "sign in"
                     :to "to start commenting."}
   :question-list {:questions-about "Questions about"
                   :page-intro "Gather background information such as opinions and experiences by asking a question.  Your question should be focussed and specific, as the answers will influence the policy drafting process."
                   :question-list-heading "Recently asked questions"
                   :no-questions "There are no questions yet."}
   :question-create {:add-a-question "Add a question to this objective"
                     :question-label "Question"
                     :question-title "Questions are a maximum length of 500 characters"
                     :post-button "Add"}
   :question-view {:added-message "Your question has been added!"
                   :added-answer-message "Your answer has been added!"}
   :question-sign-in {:please "Please"
                      :sign-in "sign in"
                      :to "to add a question." }
   :answer-view   {:created-message "Your answer has been posted!"
                   :answer-title "Answers"
                   :no-answers "No one has answered this question yet."}
   :answer-create {:answer-label "Your answer"
                   :answer-title "Answers are a maximum length of 500 characters"
                   :post-button "Post your answer"}
   :answer-sign-in {:please "Please"
                    :sign-in "sign in"
                    :to "to answer this question."}
   :invitation {:doc-title "Candidate policy writers | Objective[8]"
                :doc-description "Candidate policy writers..."
                :page-title "Invite a policy writer"
                :page-intro "Do you know someone with the enthusiasm and expertise to help draft this policy?"
                :writer-name-label "Writer name: "
                :writer-name-title "Writer names are a maximum length of 50 characters"
                :reason-label "They should help draft this policy, because: "
                :reason-title "Reasons are a maximum length of 1000 characters"
                :submit "Create an invite"}
   :invitation-sign-in {:please "Please"
                        :sign-in "sign in"
                        :to "to invite a policy writer."}
   :invitation-response {:doc-title "Invitation to draft | Objective[8]"
                         :doc-description "Invitation to help draft policy"
                         :page-title "Can you help us to draft some policy?"
                         :page-intro "You've been invited to help draft some policy!"
                         :for-objective-text "Help write policy to achieve this objective: "
                         :rsvp-text "If you're interested, you can sign in and accept the invitation below."
                         :sign-in-to-accept "Sign in to accept"
                         :accept "Accept"
                         :decline "Decline"}
   :invitation-banner {:message "Respond to your invitation to help draft policy"}
   :candidate-list {:doc-title "Candidate policy writers | Objective[8]"
                    :doc-description "Candidate policy writers..."
                    :page-intro "Writers work with each other and the community to draft policy."
                    :candidate-list-heading "Recently added candidate writers"
                    :no-candidates "No candidate writers have been invited yet."}
   :current-draft {:page-intro "Current draft"
                   :no-drafts "There are currently no drafts for this objective"}
   :draft-detail {:doc-title "Policy draft | Objective[8]"
                  :doc-description "Draft policy for objective ... "}
   :edit-draft {:doc-title "Edit draft | Objective[8]"
                :doc-description "Draft policy"
                :preview "Preview"
                :submit "Submit"}
   :sign-up {:doc-title "Sign up almost there | Objective[8]"
             :doc-description ""
             :page-title "Almost there"
             :welcome "Adding your email address will let us notify you when you receive responses to comments, objectives and votes."
             :username-label "Username"
             :username-title "Create your username"
             :not-unique "Username already exists"
             :not-well-formed "Username must be 1-16 characters in length, containing only letters and numbers"
             :email-label "Email"
             :email-title "Add your email title"
             :button "Create account"}})

(def spanish-translations
  {:base {:header-logo-text "Objective[8]"
          :header-logo-title "Inicio"
          #_:project-status! #_"Spanish(ALPHA: We are still in development and testing, <a href=\"/project-status\" title=\"Find out more about our project status\">find out more</a>)"
          :browsehappy! "Por favor <a href='http://browsehappy.com/' target='_blank' title='Visita browsehappy para aprender más sobre las últimas versiones de tu navegador'>actualiza tu navegador</a> para que tengas una mejor experiencia."}
   :navigation-global {:sign-in-text "Entrar"
                       :sign-in-title "Iniciar sesión"
                       :sign-out-text "Salir"
                       :sign-out-title "Cerrar sesión"
                       :profile-text "Perfil"
                       :profile-title "Perfil"}
   :share-widget {:title "Compartir"
                  :facebook-title "Compartir en Facebook"
                  :facebook-text "Compartir en Facebook"
                  :google-plus-title "Compartir en Google+"
                  :google-plus-text "Compartir en Google+"
                  :twitter-title "Compartir en Twitter"
                  :twitter-text "Compartir en Twitter"
                  :linkedin-title "Compartir en LinkedIn"
                  :linkedin-text "Compartir en LinkedIn"
                  :reddit-title "Compartir en Reddit"
                  :reddit-text "Compartir en Reddit"
                  :url-label "Comparte el link"
                  :url-title "Comparte el link"}
   :error-404 {#_:doc-title #_"Spanish(Error 404 | Objective[8])"
               #_:doc-description #_"Spanish(Sorry the page you requested can't be found.)"
               #_:page-title #_"Spanish(Sorry the page you requested can't be found.)"
               #_:page-intro #_"Spanish(The page you were trying to reach at this address doesn't seem to exist. This is usually the result of a bad or outdated link. We apologise for any inconvenience.)"
               :page-content! "<h2>What can I do now?</h2><ul class=\"list-large\"><li>Please return to the <a href=\"/\" title=\"Go to Home\">home page</a>.</li><li>To report an issue visit our <a href=\"https://github.com/ThoughtWorksInc/objective8/issues\">github issues page</a>.</li></ul>" }
   :notifications {#_:drafting-has-started #_"Spanish(...)"
                   #_:drafting-started-helper-text #_"Spanish(...)"
                   #_:go-to-drafting #_"Spanish(...)"}
   :index {:doc-title "Objective[8]"
           #_:doc-description #_""
           :index-welcome "Politicos colaborando con organizaciones democráticas"
           :index-intro "Recolectar la opinion de la comunidad, generar ideas, compartir, discutir, votar y colaborar con expertos para redactar nuevas politicas."
           :index-get-started "Objetivo"
           :index-get-started-title "Objetivo"
           :index-learn-more "Más información"
           :index-learn-more-title "Más información"}
   :sign-in {:doc-title "Entrar | Objective[8]"
             :doc-description "Entrar"
             #_:page-title #_"Spanish(Sign in or Sign up)"
             :twitter-sign-in-btn "Entra con Twitter"
             :twitter-sign-in-title "Entra con Twitter"
             #_:reassurance #_"Spanish(We will never post to Twitter without your permission.)"}
   :project-status {#_:doc-title #_"Spanish(Project status | Objective [8])"
                    #_:doc-description #_""
                    #_:page-title #_"Spanish(Alpha phase)"
                    #_:page-intro #_"Spanish(We are developing and designing Objective[8] in the open. This allows the tool evolve as you use it, as you give feedback, and as the developers update and add content.)"
                    #_:page-content! #_"Spanish(<p>It is important to note that during this time data may be removed or destroyed as part of our process. Any of the views and data expressed here is to be used for example purposes only and does not accurately represent the real views of users.</p><h2>How to get involved</h2><p>This project is open source and we are inviting people to collaborate with us to build a better tool. For developers and you can find the code on github at <a href=\"https://github.com/ThoughtWorksInc/objective8\" title=\"Objective[8] on github\">https://github.com/ThoughtWorksInc/objective8</a>. We will also be adding details for upcoming usability sessions shortly.</p><h2>Have you got an idea?</h2><p>We are really interested in user feedback and are currently inviting people to leave comments our <a href=\"https://github.com/ThoughtWorksInc/objective8/issues\">github issues page</a>.</p>)" }
   :learn-more {#_:doc-title #_"Spanish(Learn more) | Objective[8]"
                #_:doc-description #_""
                #_:page-title #_"Spanish(...)"
                #_:sub-title #_"Spanish(...)"
                #_:page-intro #_"Spanish(...)"
                #_:get-started-button-title #_"Spanish(...)"
                #_:get-started-button-text #_"Spanish(...)"
                #_:page-content! #_"Spanish(...)"}
   :objective-nav {#_:title #_"Spanish(Navigation for)"
                   #_:details #_"Spanish(Details)"
                   #_:questions #_"Spanish(Questions)"
                   #_:writers #_"Spanish(Writers)"}   
   :objective-list {#_:doc-title #_"Spanish(Objectives | Objective[8])"
                    #_:doc-description #_""
                    #_:page-title #_"Spanish(Objectives)"
                    #_:create-button-title #_"Spanish(Create an objective)"
                    #_:create-button-text #_"Spanish(Create an objective)"
                    #_:please #_"Spanish(Please)"
                    #_:sign-in #_"Spanish(sign in)"
                    #_:to #_"Spanish(to create an objective.)"
                    #_:subtitle #_"Spanish(Recently created objectives)"
                    #_:drafting-begins #_"Spanish(Drafting begins on)"
                    #_:objectives-intro-text #_"Spanish(...)"}
   :objective-create {:doc-title "Crea un objetivo | Objective[8]"
                      :doc-description "Crea un objetivo"
                      :page-title "Crea un objetivo"
                      #_:page-intro #_"Spanish(...)"
                      #_:headline-label #_"Spanish(...)"
                      :headline-title "3 carateres mínimo, 120 carateres máximo"
                      #_:headline-prompt #_"Spanish(...)"
                      #_:headline-placeholder #_"Spanish(...)"
                      :goals-label "Meta"
                      #_:goals-prompt #_"Spanish(...)"
                      :goals-title "50 carateres mínimo, 200 carateres máximo"
                      #_:goals-placeholder #_"Spanish(...)"
                      #_:background-label #_"Spanish(...)"
                      #_:background-prompt #_"Spanish(...)"
                      :background-title "Escribe una descripcion máximo de 1000 caracteres."
                      #_:end-date-label #_"Spanish(...)"
                      #_:end-date-prompt #_"Spanish(...)"
                      #_:drafting-not-yet-implemented #_"Spanish(...)"
                      :end-date-title "Por favor escribe la fecha de vencimiento"
                      :end-date-placeholder "yyyy-mm-dd"
                      :submit "Crear"}
   :objective-view {:doc-title "Objetivo | Objective[8]"
                    #_:doc-description #_""
                    #_:background-label #_"Spanish(Background)"
                    :goals-label "Meta"
                    :end-date-label "Fecha de vencimiento"
                    :created-message "Tu objetivo ha sido creado"}
   :comment-view {:created-message "Tu comentario ha sido creado"
                  :comment-title "Comentarios"
                  #_:no-comments #_"Spanish(There are no comments yet.)"}
   :comment-create {#_:comment-label #_"Spanish(Your comment)"
                    :comment-title "Los comentarios son máximo de 500 caracteres."
                    #_:post-button #_"Spanish(Add comment)"}
   :comment-sign-in {#_:please #_"Spanish(Please)"
                     #_:sign-in #_"Spanish(sign in)"
                     #_:to #_"Spanish(to start commenting.)"}
   :question-list {#_:questions-about #_"Spanish(Questions about)"
                   #_:page-intro #_"Spanish(...)"
                   #_:question-list-heading #_"Spanish(...)"
                   #_:no-questions #_"Spanish(There are no questions yet.)" }
   :question-create {#_:question-label #_"Spanish(Question)"
                     #_:question-title #_"Spanish(Questions are a maximum length of 500 characters)"
                     #_:post-button #_"Spanish(Add)"}
   :question-view {#_:added-message #_"Spanish(Your question has been added!)"
                   #_:added-answer-message #_"Spanish(Your answer has been added!)"}
   :question-sign-in {#_:please #_"Spanish(Please)"
                      #_:sign-in #_"Spanish(sign in)"
                      #_:to #_"Spanish(to ask a question.)" }
   :answer-view   {#_:created-message #_"Spanish(Your answer has been posted!)"
                   #_:answer-title #_"Spanish(Answers)"
                   #_:no-answers #_"Spanish(No one has answered this question yet.)"}
   :answer-create {#_:answer-label #_"Spanish(Your answer)"
                   #_:answer-title #_"Spanish(Answers are a maximum length of 500 characters)"
                   #_:post-button #_"Spanish(Post your answer)"}
   :answer-sign-in {#_:please #_"Spanish(Please)"
                    #_:sign-in #_"Spanish(sign in)"
                    #_:to #_"Spanish(to answer this question.)"}
   :invitation {#_:doc-title #_"Spanish(...)"
                #_:doc-description #_"Spanish(...)"
                #_:page-title #_"Spanish(...)"
                #_:page-intro #_"Spanish(...)"
                #_:writer-name-label #_"Spanish(...)"
                #_:writer-name-title #_"Spanish(...)"
                #_:reason-label #_"Spanish(...)"
                #_:reason-title #_"Spanish(...)"
                #_:submit #_"Spanish(...)"}
   :invitation-response {#_:doc-title #_"Spanish(...)"
                         #_:doc-description #_"Spanish(...)"
                         #_:page-title #_"Spanish(...)"
                         #_:page-intro #_"Spanish(...)"
                         #_:for-objective-text #_"Spanish(...)"
                         #_:rsvp-text #_"Spanish(...)"
                         #_:sign-in-to-accept #_"Spanish(...)"
                         #_:accept #_"Spanish(...)"
                         #_:decline #_"Spanish(...)"}
   :invitation-sign-in {#_:please #_"Spanish(...)"
                        #_:sign-in #_"Spanish(...)"
                        #_:to #_"Spanish(...)"}
   :invitation-banner {#_:message #_"Spanish(...)"}
   :candidate-list {#_:page-intro #_"Spanish(...)"
                    #_:candidate-list-heading #_"Spanish(...)"
                    #_:no-candidates #_"Spanish(...)"}
   :current-draft {#_:page-intro #_"Spanish(...)"
                   #_:no-drafts #_"Spanish(...)"}
   :edit-draft {#_:doc-title #_"Spanish(...)"
                #_:doc-description #_"Spanish(...)"
                #_:preview #_"Spanish(...)"
                #_:submit #_"Spanish(...)"}
   :sign-up {:doc-title "Perfil | Objective[8]"
             #_:doc-description #_""
             #_:page-title #_""
             :welcome  "Tu correo electrónico nos permitira informarte cuando recivas respuestas a tus comentarios, objetivos y votos."
             #_:username-label #_""
             #_:username-title #_""
             #_:not-unique #_""
             #_:not-well-formed #_""
             :email-label  "Correo electrónico"
             :email-title  "Escribe tu cuenta de correo electrónico"
             #_:button #_""}})

(def translation-config
  {:dictionary {:en english-translations
                :es spanish-translations}
   
   :dev-mode? false
   :fallback-locale :en
   :log-missing-translations-function (fn [{:keys [locale ks scope]}]
                                        (log/warn (str "Missing translations! " locale ks scope)))})

(def t (tower/make-t translation-config))
