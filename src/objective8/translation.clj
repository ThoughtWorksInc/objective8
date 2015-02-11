(ns objective8.translation
  (:require [clojure.tools.logging :as log]
            [taoensso.tower :as tower]))

;;TODO move translations to external files

(def translation-config
  {:dictionary {:en { :base {:header-logo-text "Objective[8]"
                             :header-logo-title "Go to home page"
                             :project-status! "ALPHA: We are still in development and testing, <a href=\"/project-status\" title=\"Find out more about our project status\">find out more</a>"
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
                             :page-intro "This page you were trying to reach at this address doesn't seem to exist. This is usually the result of a bad or outdated link. We apologize for any inconvenience."
                             :page-content! "<h2>What can I do now?</h2><ul><li>Please return to the <a href=\"/\" title=\"Go to Home\">home page</a>.</li><li>If you would like to report an issue visit our <a href=\"https://github.com/ThoughtWorksInc/objective8/issues\">github issues page</a>.</li></ul>" }
                      :index {:doc-title "Objective[8]"
                             :doc-description ""
                             :index-welcome "Collaborative policy making for democratic organisations."
                             :index-intro "Gather community opinion, generate ideas, share, discuss, vote and collaborate with experts to draft new policy."
                             :index-get-started-signed-out "Get started"
                             :index-get-started-title-signed-out "Get started"
                             :index-get-started-signed-in "Create an Objective"
                             :index-get-started-title-signed-in "Create an Objective"
                             :index-learn-more "Learn more"
                             :index-learn-more-title "Learn more"}
                     :sign-in {:doc-title "Sign in | Objective[8]"
                               :doc-description "Sign in ..."
                               :page-title "Please sign in"
                               :twitter-sign-in-btn "Sign in with twitter"
                               :twitter-sign-in-title "Sign in with twitter"}
                     :project-status {:doc-title "Project status | Objective [8]"
                                      :doc-description ""
                                      :page-title "Alpha phase"
                                      :page-intro "We are developing and designing Objective[8] in the open. This allows the tool evolve as you use it, as you give feedback, and as the developers update and add content."
                                      :page-content! "<p>It is important to note that during this time data may be removed or destroyed as part of our process. Any of the views and data expressed here is to be used for example purposes only and does not accurately represent the real views of users.</p><h2>How to get involved</h2><p>This project is open source and we are inviting people to collaborate with us to build a better tool. For developers and you can find the code on github at <a href=\"https://github.com/ThoughtWorksInc/objective8\" title=\"Objective[8] on github\">https://github.com/ThoughtWorksInc/objective8</a>. We will also be adding details for upcoming usability sessions shortly.</p><h2>Have you got an idea?</h2><p>We are really interested in user feedback and are currently inviting people to leave comments our <a href=\"https://github.com/ThoughtWorksInc/objective8/issues\">github issues page</a>.</p>" }
                     :objective-create {:doc-title "Create an Objective | Objective[8]"
                                        :doc-description "Create an Objective ..."
                                        :page-title "Create an objective"
                                        :page-intro "Objective[8] centers it’s policy drafting process around objectives. An objective is a change that could be achieved by introducing new policy."
                                        :headline-label "Headline"
                                        :headline-title "3 characters minimum, 120 characters maximum"
                                        :headline-prompt "A headline is for discovery, encouraging contribution or discussion"
                                        :headline-placeholder "e.g, Reduce the number of cycling deaths in Central London"
                                        :goals-label "Goals"
                                        :goals-prompt "A goal is what you’d like your change to achieve."
                                        :goals-title "50 characters minimum, 200 characters maximum"
                                        :goals-placeholder "e.g, Better road safety for cyclists"
                                        :background-label "Background"
                                        :background-prompt "Use this area to provide further information on why this is important or necessary"
                                        :background-title "Enter optional background text up to 1000 characters"
                                        :end-date-label "End date"
                                        :end-date-prompt "The deadline for discussing this objective."
                                        :end-date-title "Please enter an end date"
                                        :end-date-placeholder"yyyy-mm-dd"
                                        :submit "Create"}
                     :objective-view {:doc-title "Objective | Objective[8]"
                                      :doc-description "Objective  ..."
                                      :background-label "Background"
                                      :goals-label "Goals"
                                      :end-date-label "End date"
                                      :created-message "Your objective has been created!"}
                     :comment-view {:created-message "Your comment has been added!"
                                    :comment-title "Comments"}
                     :comment-create {:comment-label "Comment"
                                      :comment-title "Comments are a maximum length of 500 characters"
                                      :post-button "Post"}
                     :users-email {:doc-title "Sign up | Objective[8]"
                                   :doc-description "Email  ..."
                                   :page-title "Add your email"
                                   :user-email-welcome "Adding your email address will let us notify you when you receive responses to comments, objectives and votes."
                                   :email-label "Email"
                                   :email-title "Add your email title"
                                   :button "Submit" }}
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
                             :page-intro "Spanish(This page you were trying to reach at this address doesn't seem to exist. This is usually the result of a bad or outdated link. We apologize for any inconvenience.)"
                             :page-content! "<h2>What can I do now?</h2><ul><li>Please return to the <a href=\"/\" title=\"Go to Home\">home page</a>.</li><li>If you would like to report an issue visit our <a href=\"https://github.com/ThoughtWorksInc/objective8/issues\">github issues page</a>.</li></ul>" }
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
                               :page-title "Por favor entra a tu cuenta"
                               :twitter-sign-in-btn "Entra con Twitter"
                               :twitter-sign-in-title "Entra con Twitter"}
                     :project-status {:doc-title "Spanish(Project status | Objective [8])"
                                      :doc-description ""
                                      :page-title "Spanish(Alpha phase)"
                                      :page-intro "Spanish(We are developing and designing Objective[8] in the open. This allows the tool evolve as you use it, as you give feedback, and as the developers update and add content.)"
                                      :page-content! "Spanish(<p>It is important to note that during this time data may be removed or destroyed as part of our process. Any of the views and data expressed here is to be used for example purposes only and does not accurately represent the real views of users.</p><h2>How to get involved</h2><p>This project is open source and we are inviting people to collaborate with us to build a better tool. For developers and you can find the code on github at <a href=\"https://github.com/ThoughtWorksInc/objective8\" title=\"Objective[8] on github\">https://github.com/ThoughtWorksInc/objective8</a>. We will also be adding details for upcoming usability sessions shortly.</p><h2>Have you got an idea?</h2><p>We are really interested in user feedback and are currently inviting people to leave comments our <a href=\"https://github.com/ThoughtWorksInc/objective8/issues\">github issues page</a>.</p>)" }
                     :objective-create {:doc-title "Crea un objetivo | Objective[8]"
                                        :doc-description "Crea un objetivo"
                                        :page-title "Crea un objetivo"
                                        :page-intro "Spanish(Objective[8] centers it’s policy drafting process around objectives. An objective is a change that could be achieved by introducing new policy.)"
                                        :headline-label "Spanish(Headline)"
                                        :headline-title "3 carateres mínimo, 120 carateres máximo"
                                        :headline-prompt "Spanish(A headline is for discovery, encouraging contribution or discussion)"
                                        :headline-placeholder "Spanish(e.  Reduce the number of cycling deaths in Central London)"
                                        :goals-label "Meta"
                                        :goals-prompt "Spanish(A goal is what you’d like your change to achieve.)"
                                        :goals-title "50 carateres mínimo, 200 carateres máximo"
                                        :goals-placeholder "Spanish(e.g, Better road safety for cyclists)"
                                        :background-label "Spanish(Background)"
                                        :background-prompt "Spanish(Use this area to provide further information on why this is important or necessary)"
                                        :background-title "Escribe una descripcion máximo de 1000 caracteres."
                                        :end-date-label "Fecha de vencimiento"
                                        :end-date-prompt "Spanish(The deadline for discussing this objective.)"
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
                                   :comment-title "Comentarios"}
                     :comment-create {:comment-label "Comentario"
                                      :comment-title "Los comentarios son máximo de 500 caracteres."
                                      :post-button "Crear"}
                     :users-email {:doc-title "Perfil | Objective[8]"
                                   :doc-description ""
                                   :page-title "Escribe tu cuenta de correo electrónico"
                                   :user-email-welcome "Tu correo electrónico nos permitira informarte cuando recivas respuestas a tus comentarios, objetivos y votos."
                                   :email-label "Correo electrónico"
                                   :email-title "Escribe tu cuenta de correo electrónico"
                                   :button "Crear"}}}
   :dev-mode? false
   :fallback-locale :en
   :log-missing-translations-function (fn [{:keys [locale ks scope]}]
                                        (log/warn (str "Missing translations! " locale ks scope)))})

(def t (tower/make-t translation-config))
