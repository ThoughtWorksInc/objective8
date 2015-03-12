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
                                         :profile-title "Ir al perfil de usuario"}
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
                                 :doc-description "Lo sentimos, la página que solicitas no se encuentra"
                                 :page-title "Lo sentimos, la página que solicitas no se encuentra"
                                 :page-intro "Parece que la página a la que intentas acceder no existe. Esto puede deberse a un enlace incorrecto o desactualizado"
                                 :page-content! "<h2>What can I do now?</h2><ul class=\"list-large\"><li>Please return to the <a href=\"/\" title=\"Go to Home\">home page</a>.</li><li>To report an issue visit our <a href=\"https://github.com/ThoughtWorksInc/objective8/issues\">github issues page</a>.</li></ul>" }
                     :notifications {:drafting-has-started "la redacción ha comenzado"
                                     :drafting-started-helper-text "texto de ayuda para comenzar a redactar"
                                     :go-to-drafting "ir al borrador"}
                     :index {:doc-title "Objective[8]"
                             :doc-description ""
                             :index-welcome "Creación de politica colaborativa para organizaciones democráticas"
                             :index-intro "Recolectar la opinion de la comunidad, generar ideas, compartir, discutir, votar y colaborar con expertos para redactar nuevas politicas."
                             :index-get-started "Objetivos"
                             :index-get-started-title "Objetivos"
                             :index-learn-more "Más información"
                             :index-learn-more-title "Más información"}
                     :sign-in {:doc-title "Entrar o registrarte| Objective[8]"
                               :doc-description "Entra o regístrate a Objective[8]"
                               :page-title "Entrar o registrarte"
                               :twitter-sign-in-btn "Entra con Twitter"
                               :twitter-sign-in-title "Entra con Twitter"
                               :reassurance "No compartiremos nunca un contenido en Twitter sin tu permiso"}
                     :project-status {:doc-title "Estado del proyecto | Objective [8]"
                                      :doc-description ""
                                      :page-title "Fase alfa"
                                      :page-intro "Estamos diseñando y desarrollando Objetive[8] en abierto. Esto permite que la herramienta evolucione según la uses, nos des feedback, y los desarrolladores vayan actualizando y subiendo el nuevo contenido."                                     
                                      :page-content! "<p> Es importante que tengas en cuenta que durante este tiempo algunos contenidos pueden ser borrados como parte del proceso. Todas las vistas y los datos que se visualizan aquí son ejemplos prácticos y no represent de forma precisa la vista real del usuario final. </p><h2> Como involucrarse </h2><p>Este proyecto es Software Libre y estamos invitando a la gente a colaborar para mejorar la herramienta. Si eres desarrollador podrás encontrar el código en github en<a href=\"https://github.com/ThoughtWorksInc/objective8\" title=\"Objective[8] on github\">https://github.com/ThoughtWorksInc/objective8</a>. También estaremos añadiendo nuevos detalles provenientes de las pruebas de usabilidad próximamente. </p><h2> ¿Tienes una idea que aportar? </h2><p> Estamos muy interesados en recoger todo el feedback posible que los usuarios puedan darnos así que os invitamos a dejar comentarios aquí<a href=\"https://github.com/ThoughtWorksInc/objective8/issues\">página de comentarios de github </a>.</p>)" }
                     :learn-more {:doc-title "Saber más sobre Objetive[8]"
                                  :doc-description ""
                                  :page-title "Objective[8] 101"
                                  :sub-title "Información básica"
                                  :page-intro "Objective8 es una plataforma para facilitar la creación de política colectiva en organizaciones democráticas"
                                  :get-started-button-title "Comienza"
                                  :get-started-button-text "Comienza"
                                  :page-content! "
<p><b>Policy</b> is used by governments and organisations to make consistent and fair decisions in order to achieve desired outcomes.</p>
           
                                  <p>An <b>objective</b> is a change that could be achieved by introducing new policy.  For example: improving access to public housing, or increasing the safety of our roads.</p>
               
                                  <p><b>Writers</b> work with each other and the community to draft policy.  They use the crowdsourced questions, answers and comments to produce <b>policy drafts</b><span class=\"not-yet-implemented-footnote-marker\">*</span>.  These are further refined through cycles of feedback and redrafting.</p>
                                  <p>You can get involved and have your say by creating and sharing objectives, offering your opinion through <b>comments</b>, asking and answering <b>questions</b>, suggesting policy writers, or even providing feedback on the policy drafts themselves.  The whole process is open and transparent.</p>
                                  
                                  <p class=\"helper-text\"> <span class=\"not-yet-implemented-footnote-marker\">*</span>As the site is still under development, these features have not yet been implemented. Check back soon!</p>"}
                     :objective-nav {:title "Navegar por"
                                     :details "Detalles"
                                     :questions "Preguntas"
                                     :writers "Redactores"}   
                     :objective-list {:doc-title "Objetivos | Objective[8]"
                                      :doc-description ""
                                      :page-title "Objetivos"
                                      :create-button-title "Crear un objetivo"
                                      :create-button-text "Crear un objetivo"
                                      :please "Por favor"
                                      :sign-in "Entrar"
                                      :to "para crear un objetivo"
                                      :subtitle "Objetivos recientes"
                                      :drafting-begins "La redacción empieza en"
                                      :objectives-intro-text "Objective[8] centra su proceso de escritura de borradores políticos en los objetivos. Un objetivo es un cambio que puede ser conseguido a partir de una propuesta política"}
                     :objective-create {:doc-title "Crea un objetivo | Objective[8]"
                                        :doc-description "Crea un objetivo"
                                        :page-title "Crea un objetivo"
                                        :page-intro "Un objetivo es un cambio que puede ser conseguido a partir de una propuesta política. Por ejemplo: mejorar el acceso a la vivienda pública, o incrementar la seguridad de nuestras carreteras"
                                        :headline-label "Título"
                                        :headline-title "3 caracteres mínimo, 120 caracteres máximo"
                                        :headline-prompt "El título debe invitar a la contribución o a la discusión"
                                        :headline-placeholder "Spanish(...)"
                                        :goals-label "Meta"
                                        :goals-prompt "Una meta es lo que te gustaría que tu cambio consiguierea"
                                        :goals-title "50 caracteres mínimo, 200 caracteres máximo"
                                        :goals-placeholder "Spanish(...)"
                                        :background-label "Contexto"
                                        :background-prompt "Añade más información de por qué alcanzar este objetivo es necesario o importante"
                                        :background-title "Escribe una descripcion de máximo 1000 caracteres."
                                        :end-date-label "Fecha de comienzo de la escritura del borrador"
                                        :end-date-prompt "En esta fecha empieza la fase de creación del borrador. Debes dar a los participantes suficiente tiempo para generar información útil, y para proponer y votar potenciales escritores del texto"
                                        :drafting-not-yet-implemented "Aviso importante: dado que esta herramienta todavía está en desarrollo, la fase de creación del borrador todavía no ha sido implementada"
                                        :end-date-title "Por favor escribe la fecha de finalización del proceso"
                                        :end-date-placeholder "yyyy-mm-dd"
                                        :submit "Crear"}
                     :objective-view {:doc-title "Objetivo | Objective[8]"
                                      :doc-description "Objetivo ..."
                                      :background-label "Contexto"
                                      :goals-label "Metas"
                                      :end-date-label "Fecha de  finalización del proceso"
                                      :created-message "Tu objetivo ha sido creado"}
                     :comment-view {:created-message "Tu comentario ha sido creado"
                                    :comment-title "Comentarios"
                                    :no-comments "Todavía no hay comentarios"}
                     :comment-create {:comment-label "Tu comentario"
                                      :comment-title "Los comentarios tienen un máximo de 500 caracteres."
                                      :post-button "Añade un comentario"}
                     :comment-sign-in {:please "Por favor"
                                       :sign-in "entra"
                                       :to "para empezar a comentar."}
                     :question-list {:questions-about "Preguntas sobre"
                                     :page-intro "Obtén información de contexto como opiniones y experiencias preguntando algo. Tu pregunta debe ser concreta y específica, ya que las respuestas influirán en el proceso de creación del borrador"
                                     :question-list-heading "Preguntas recientemente añadidas"
                                     :no-questions "Todavía no hay preguntas" }
                     :question-create {:question-label "Pregunta"
                                       :question-title "Las preguntas tienen una longitud máxima de 500 caracteres"
                                       :post-button "Añadir"}
                     :question-view {:added-message "Tu pregunta ha sido añadida"
                                     :added-answer-message "Tu respuesta ha sido añadida!"}
                     :question-sign-in {:please "Por favor"
                                        :sign-in "Entrar"
                                        :to "añadir una pregunta" }
                     :answer-view   {:created-message "Tu respuesta ha sido enviada"
                                     :answer-title "Respuestas"
                                     :no-answers "Nadie ha respondido a esta pregunta aún"}
                     :answer-create {:answer-label "Tu respuesta"
                                     :answer-title "Las respuestas pueden tener un máximo de 500 caracteres"
                                     :post-button "Manda tu respuesta"}
                     :answer-sign-in {:please "Por favor"
                                      :sign-in "Entra"
                                      :to "para responder a la pregunta"}
                     
                     :invitation {:doc-title "Candidatos a redactores del borrador"
                                  :doc-description "Candidatos a redactores del borrador)"
                                  :page-title "Invita a un redactor"
                                  :page-intro "¿Conoces a alguien con el entusiasmo y la experiencia necesaria para ayudar a redactar este borrador"
                                  :writer-name-label "Nombre del redactor"
                                  :writer-name-title "El nombre del redactor puede tener una longitud máxima de 50 caracteres"
                                  :reason-label "Debería ayudar a redactar este borrador porque:"
                                  :reason-title "La razón por la que invitas puede tener un máximo de 1000 caracteres"
                                  :submit "Crear una invitación"}
                     
                     :invitation-response {:doc-title "Invitación para redactar I Objective[8]"
                                           :doc-description "Invitación para ayudar a redactar el borrador"
                                           :page-title "¿Puedes ayudarnos a redactar este borrador?"
                                           :page-intro "Has sido invitado a redactar un borrador de documento"
                                           :for-objective-text "Ayuda a redactar este borrador para conseguir este objetivo"
                                           :rsvp-text "Si estas interesados puedes entrar y aceptar la invitación siguiente"
                                           :sign-in-to-accept "Entrar para aceptar"
                                           :accept "Aceptar"
                                           :decline "Declinar el ofrecimiento)"}
                     :invitation-sign-in {:please "Por favor"
                                          :sign-in "entra"
                                          :to "para invitar a un nuevo redactor"}
                     :invitation-banner {:message "Responde a la invitación para ayudar a redactar el documento"}
                     
                     :candidate-list {:page-intro "Los redactores trabajan entre sí y con la comunidad para redactar el borrador"
                                      :candidate-list-heading "Redactores incorporados recientemente"
                                      :no-candidates "Ningún redactor ha sido invitado por ahora"}
                     :current-draft {:page-intro "Borrador actual"
                                     :no-drafts "No hay borradores para este objetivo"}
                     :edit-draft {:doc-title "Editar borrador I Objetive[8]"
                                  :doc-description "Borrador"
                                  :preview "Vista previa"
                                  :submit "Enviar"}

                     :sign-up {:doc-title "Perfil | Objective[8]"
                               :doc-description ""
                               :page-title "Casi hemos acabado"
                               :welcome  "Tu correo electrónico nos permitirá informarte cuando recibas respuestas a tus comentarios, objetivos y votos."
                               :username-label "Nombre de usuario"
                               :username-title "Crea tu nombre de usuario"
                               :not-unique "El nombre de usuario ya está en uso"
                               :not-well-formed "El nombre de usuario debe tener de 1 a 16 caracteres, conteniendo solo letras y númenos"
                               :email-label  "Correo electrónico"
                               :email-title  "Escribe tu cuenta de correo electrónico"                   
                               :button ""}}}
   :dev-mode? false
   :fallback-locale :en
   :log-missing-translations-function (fn [{:keys [locale ks scope]}]
                                        (log/warn (str "Missing translations! " locale ks scope)))})

(def t (tower/make-t translation-config))
