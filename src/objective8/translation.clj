(ns objective8.translation
  (:require [clojure.tools.logging :as log]
            [taoensso.tower :as tower]))

;;TODO move translations to external files

(def translation-config
  {:dictionary {:en { :base {:header-logo-text "Objective[8]"
                             :header-logo-title "Go to home page"
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
                     :objective-create {:doc-title "Create an Objective | Objective[8]"
                                        :doc-description "Create an Objective ..."
                                        :page-title "Create an objective"
                                        :page-intro "Objective8 centers it’s policy drafting process around Objectives. We define an objectives as 'a desired outcome achieved by introducing new policy'. "
                                        :title-label "Title"
                                        :title-title "3 characters minimum, 120 characters maximum"
                                        :title-placeholder "e.g, Reduce city traffic"
                                        :goals-label "Goals"
                                        :goals-title "50 characters minimum, 500 characters maximum"
                                        :goals-placeholder "e.g, Encourage cycling, walking and greener methods or transport"
                                        :description-label "Description"
                                        :description-title "Enter an optional description up to 1000 characters"
                                        :end-date-label "End date"
                                        :end-date-title "Please enter an end date"
                                        :end-date-placeholder"yyyy-mm-dd"
                                        :submit "Create"}
                     :objective-view {:doc-title "Objective | Objective[8]"
                                      :doc-description "Objective  ..."
                                      :description-label "Description"
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
                     :objective-create {:doc-title "Crea un objetivo | Objective[8]"
                                        :doc-description "Crea un objetivo"
                                        :page-title "Crea un objetivo"
                                        :page-intro "Spanish(Objective8 centers it’s policy drafting process around Objectives. We define an objectives as 'a desired outcome achieved by introducing new policy'.)"
                                        :title-label "Título"
                                        :title-title "3 carateres mínimo, 120 carateres máximo"
                                        :title-placeholder "e. Reducir trafico en la ciudad"
                                        :goals-label "Meta"
                                        :goals-title "50 carateres mínimo, 500 carateres máximo"
                                        :goals-placeholder "Spanish(e.g, Encourage cycling, walking and greener methods or transport)"
                                        :description-label "Descripción"
                                        :description-title "Escribe una descripcion máximo de 1000 caracteres."
                                        :end-date-label "Fecha de vencimiento"
                                        :end-date-title "Por favor escribe la fecha de vencimiento"
                                        :end-date-placeholder "yyyy-mm-dd"
                                        :submit "Crear"}
                     :objective-view {:doc-title "Objetivo | Objective[8]"
                                      :doc-description ""
                                      :description-label "Descripción"
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
