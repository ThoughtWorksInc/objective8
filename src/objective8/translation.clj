(ns objective8.translation
  (:require [clojure.tools.logging :as log]
            [taoensso.tower :as tower]))

;;TODO move translations to external files

(def translation-config
  {:dictionary {:en { :base {:header-logo-text "Objective[8]"
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
                      :index {:doc-title "Objective[8]"
                             :doc-description ""
                             :index-welcome "Collaborative policy making for democratic organisations."
                             :index-intro "Gather community opinion, generate ideas, share, discuss, vote and collaborate with experts to draft new policy."
                             :index-get-started-signed-out "Join now"
                             :index-get-started-title-signed-out "Join now"
                             :index-get-started-signed-in "Create an Objective"
                             :index-get-started-title-signed-in "Create an Objective"
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
               
                                  <p><b>Writers</b> are experts responsible for drafting policy.  They use the crowdsourced questions, answers and comments to produce <b>policy drafts</b><span class=\"not-yet-implemented-footnote-marker\">*</span>.  These are further refined through cycles of feedback and redrafting.</p>

                                  <p>You can get involved and have your say by creating and sharing objectives, offering your opinion through <b>comments</b>, asking and answering <b>questions</b>, suggesting policy writers<span class=\"not-yet-implemented-footnote-marker\">*</span>, or even providing feedback on the policy drafts themselves.  The whole process is open and transparent.</p>
                                  
                                  <p class=\"helper-text\"> <span class=\"not-yet-implemented-footnote-marker\">*</span>As the site is still under development, these features have not yet been implemented. Check back soon!</p> "
                                  :get-started-button-title "Get started"
                                  :get-started-button-text "Get started"}
                     :objective-nav {:title "Navigation for"
                                     :details "Details"
                                     :questions "Questions"}  
                     :objective-list {:doc-title "Objectives | Objective[8]"
                                      :doc-description ""
                                      :page-title "Objectives"
                                      :create-button-title "Create an objective"
                                      :create-button-text "Create an objective"
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
                     :invitation {:doc-title "Proposed policy writers | Objective[8]"
                                     :doc-description "Proposed policy writers..." 
                                     :page-title "Invite a policy writer"
                                     :writer-name-label "Who"
                                     :writer-name-title "Writer names are a maximum length of 50 characters"
                                     :reason-label "Why"
                                     :reason-title "Reasons are a maximum length of 1000 characters"
                                     :submit "Invite"}
                     :invitation-sign-in {:please "Please"
                                              :sign-in "sign in"
                                              :to "to invite a policy writer."}
                     :users-email {:doc-title "Sign up almost there | Objective[8]"
                                   :doc-description ""
                                   :page-title "Almost there"
                                   :user-email-welcome "Adding your email address will let us notify you when you receive responses to comments, objectives and votes."
                                   :email-label "Email"
                                   :email-title "Add your email title"
                                   :button "Create account"}}

                :es {:base {:header-logo-text "Objective[8]"
                            :header-logo-title "Inicio"
                            :project-status! "Spanish(ALPHA: We are still in development and testing, <a href=\"/project-status\" title=\"Find out more about our project status\">find out more</a>)"
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
                     :error-404 {:doc-title "Spanish(Error 404 | Objective[8])"
                             :doc-description "Spanish(Sorry the page you requested can't be found.)"
                             :page-title "Spanish(Sorry the page you requested can't be found.)"
                             :page-intro "Spanish(The page you were trying to reach at this address doesn't seem to exist. This is usually the result of a bad or outdated link. We apologise for any inconvenience.)"
                             :page-content! "<h2>What can I do now?</h2><ul class=\"list-large\"><li>Please return to the <a href=\"/\" title=\"Go to Home\">home page</a>.</li><li>To report an issue visit our <a href=\"https://github.com/ThoughtWorksInc/objective8/issues\">github issues page</a>.</li></ul>" }
                     :index {:doc-title "Objective[8]"
                             :doc-description ""
                             :index-welcome "Politicos colaborando con organizaciones democráticas"
                             :index-intro "Recolectar la opinion de la comunidad, generar ideas, compartir, discutir, votar y colaborar con expertos para redactar nuevas politicas."
                             :index-get-started-signed-out "Pariticipa"
                             :index-get-started-title-signed-out "Pariticipa"
                             :index-get-started-signed-in "Crea un objetivo"
                             :index-get-started-title-signed-in "Crea un objetivo"
                             :index-learn-more "Más información"
                             :index-learn-more-title "Más información"}
                     :sign-in {:doc-title "Entrar | Objective[8]"
                               :doc-description "Entrar"
                               :page-title "Spanish(Sign in or Sign up)"
                               :twitter-sign-in-btn "Entra con Twitter"
                               :twitter-sign-in-title "Entra con Twitter"
                               :reassurance "Spanish(We will never post to Twitter without your permission.)"}
                     :project-status {:doc-title "Spanish(Project status | Objective [8])"
                                      :doc-description ""
                                      :page-title "Spanish(Alpha phase)"
                                      :page-intro "Spanish(We are developing and designing Objective[8] in the open. This allows the tool evolve as you use it, as you give feedback, and as the developers update and add content.)"
                                      :page-content! "Spanish(<p>It is important to note that during this time data may be removed or destroyed as part of our process. Any of the views and data expressed here is to be used for example purposes only and does not accurately represent the real views of users.</p><h2>How to get involved</h2><p>This project is open source and we are inviting people to collaborate with us to build a better tool. For developers and you can find the code on github at <a href=\"https://github.com/ThoughtWorksInc/objective8\" title=\"Objective[8] on github\">https://github.com/ThoughtWorksInc/objective8</a>. We will also be adding details for upcoming usability sessions shortly.</p><h2>Have you got an idea?</h2><p>We are really interested in user feedback and are currently inviting people to leave comments our <a href=\"https://github.com/ThoughtWorksInc/objective8/issues\">github issues page</a>.</p>)" }
                     :learn-more {:doc-title "Spanish(Learn more) | Objective[8]"
                                  :doc-description ""
                                  :page-title "Spanish(...)"
                                  :sub-title "Spanish(...)"
                                  :page-intro "Spanish(...)"
                                  :get-started-button-title "Spanish(...)"
                                  :get-started-button-text "Spanish(...)"
                                  :page-content! "Spanish(...)"}
                     :objective-nav {:title "Spanish(Navigation for)"
                                     :details "Spanish(Details)"}
                                     :questions "Spanish(Questions)"   
                     :objective-list {:doc-title "Spanish(Objectives | Objective[8])"
                                      :doc-description ""
                                      :page-title "Spanish(Objectives)"
                                      :create-button-title "Spanish(Create an objective)"
                                      :create-button-text "Spanish(Create an objective)"
                                      :subtitle "Spanish(Recently created objectives)"
                                      :drafting-begins "Spanish(Drafting begins on)"
                                      :objectives-intro-text "Spanish(...)"}
                     :objective-create {:doc-title "Crea un objetivo | Objective[8]"
                                        :doc-description "Crea un objetivo"
                                        :page-title "Crea un objetivo"
                                        :page-intro "Spanish(...)"
                                        :headline-label "Spanish(...)"
                                        :headline-title "3 carateres mínimo, 120 carateres máximo"
                                        :headline-prompt "Spanish(...)"
                                        :headline-placeholder "Spanish(...)"
                                        :goals-label "Meta"
                                        :goals-prompt "Spanish(...)"
                                        :goals-title "50 carateres mínimo, 200 carateres máximo"
                                        :goals-placeholder "Spanish(...)"
                                        :background-label "Spanish(...)"
                                        :background-prompt "Spanish(...)"
                                        :background-title "Escribe una descripcion máximo de 1000 caracteres."
                                        :end-date-label "Spanish(...)"
                                        :end-date-prompt "Spanish(...)"
                                        :drafting-not-yet-implemented "Spanish(...)"
                                        :end-date-title "Por favor escribe la fecha de vencimiento"
                                        :end-date-placeholder "yyyy-mm-dd"
                                        :submit "Crear"}
                     :objective-view {:doc-title "Objetivo | Objective[8]"
                                      :doc-description ""
                                      :background-label "Spanish(Background)"
                                      :goals-label "Meta"
                                      :end-date-label "Fecha de vencimiento"
                                      :created-message "Tu objetivo ha sido creado"}
                    :comment-view {:created-message "Tu comentario ha sido creado"
                                   :comment-title "Comentarios"
                                   :no-comments "Spanish(There are no comments yet.)"}
                    :comment-create {:comment-label "Spanish(Your comment)"
                                     :comment-title "Los comentarios son máximo de 500 caracteres."
                                     :post-button "Spanish(Add comment)"}
                     :comment-sign-in {:please "Spanish(Please)"
                                       :sign-in "Spanish(sign in)"
                                       :to "Spanish(to start commenting.)"}
                     :question-list {:questions-about "Spanish(Questions about)"
                                     :page-intro "Spanish(...)"
                                     :question-list-heading "Spanish(...)"
                                     :no-questions "Spanish(There are no questions yet.)" }
                     :question-create {:question-label "Spanish(Question)"
                                       :question-title "Spanish(Questions are a maximum length of 500 characters)"
                                       :post-button "Spanish(Add)"}
                    :question-view {:added-message "Spanish(Your question has been added!)"
                                    :added-answer-message "Spanish(Your answer has been added!)"}
                     :question-sign-in {:please "Spanish(Please)"
                                        :sign-in "Spanish(sign in)"
                                        :to "Spanish(to ask a question.)" }
                     :answer-view   {:created-message "Spanish(Your answer has been posted!)"
                                     :answer-title "Spanish(Answers)"
                                     :no-answers "Spanish(No one has answered this question yet.)"}
                     :answer-create {:answer-label "Spanish(Your answer)"
                                     :answer-title "Spanish(Answers are a maximum length of 500 characters)"
                                     :post-button "Spanish(Post your answer)"}
                     :answer-sign-in {:please "Spanish(Please)"
                                      :sign-in "Spanish(sign in)"
                                      :to "Spanish(to answer this question.)"}
                     :invitation {:doc-title "Spanish(...)"
                                     :doc-description "Spanish(...)"
                                     :page-title "Spanish(...)"
                                     :writer-name-label "Spanish(...)"
                                     :writer-name-title "Spanish(...)"
                                     :reason-label "Spanish(...)"
                                     :reason-title "Spanish(...)"
                                     :submit "Spanish(...)"}
                     :invitation-sign-in {:please "Spanish(...)"
                                              :sign-in "Spanish(...)"
                                              :to "Spanish(...)"}
                    :users-email {:doc-title "Perfil | Objective[8]"
                                  :doc-description ""
                                  :page-title "Spanish(Almost there)"
                                  :user-email-welcome "Tu correo electrónico nos permitira informarte cuando recivas respuestas a tus comentarios, objetivos y votos."
                                  :email-label "Correo electrónico"
                                  :email-title "Escribe tu cuenta de correo electrónico"
                                  :button "Spanish(Create account)"}}}
   :dev-mode? false
   :fallback-locale :en
   :log-missing-translations-function (fn [{:keys [locale ks scope]}]
                                        (log/warn (str "Missing translations! " locale ks scope)))})

(def t (tower/make-t translation-config))
