(ns carcajou.core
  (:require [necessary-evil.core :as xml-rpc]))

(def active_instance (ref nil))
(def COMMON "common")
(def OBJECT "object")

;;---------utils---------------
(defn string_to_erp_sym [model]
  (apply str (map clojure.string/capitalize (clojure.string/split model #"\." ))))

(defn compose_url [instance service]
  (str (:protocol instance) "://" (:url instance) ":" (:port instance) "/xmlrpc/" service))

(defn raw_rpc_exec [instance service function & args]
  (apply xml-rpc/call (compose_url instance service) function args))

(defn login_from_instance [instance]
  (raw_rpc_exec instance COMMON :login (:database instance) (:user instance) (:password instance)))

;;---instance management -------------
(defn definstance
  "Generate a instance connexion map info you can override any parameter in args
    keys are :port :protocol (http, https) :user :password :url :database
    "
  [url database & kwargs]
  (def default (merge {:port 80, :protocol "http", :user "admin", :password "admin",
                       :url "localhost", :database "demo"} {:url url, :database database}))
  (let [instance (merge default (apply hash-map kwargs))]
    (assoc instance :uid (login_from_instance instance))))

(defn get_active_instance
  "Return the active connexion"
  []
  @active_instance)

(defn set_active_instance
  "Set default connexion"
  [conn]
  (dosync(ref-set active_instance conn)))
;;----- entity management ----------

(defrecord Entity [name])

(defn create_entity
  "Create an entity representing a Model in ERP"
  [model]
  (Entity. model))

(defn model
  "Set the fields to be retrieved"
  [ent name]
  (assoc ent :name name))

;;----- query management

(defn fields
  "Set the fields to be retrieved"
  [query & fields]
  (update-in query [:fields] concat fields))

(defn domain
  "Set the search domain to be retrieved"
  [query & domain]
  (update-in query [:domain] concat domain))

(defn order
  "Set the order"
  [query order]
  (assoc query :order order))

(defn limit
  "Set the limit"
  [query limit]
  (assoc query :limit limit))

(defn offset
  "Set the context"
  [query offset]
  (assoc query :offset offset))

(defn context
  "Set the fields to be retrieved"
  [query context]
  (assoc query :context context))

(defn instance
  "Set the fields to be retrieved"
  [query instance]
  (assoc query :instance instance))

(defn create_query [instance]
  "Create an entity representing a Model in ERP"
  {:domain [], :order false, :limit 0, :offset false,
   :instance instance, :context {} :fields []})

(defn create_command [instance]
  {:instance instance :args []})

(defn args [command & args]
  (update-in command [:args] concat args))

(defn which [dataset & ids]
  (update-in dataset [:ids] concat (if (= (type (first ids)) java.lang.Long)
                                       ids
                                       (first ids))))
(defn create_dataset [instance]
  {:ids [], :domain [], :values {}, :context {}, :instance instance})

(defn values
  "Set the values"
  [dataset values]
  (assoc dataset :values values))

(defn execute* [ent command]
  (let [instance (:instance command)]
    (apply raw_rpc_exec instance OBJECT :execute (:database instance) (:uid instance)
           (:password instance) (:name ent) (:args command))))

(defn search* [ent query]
  "Do a raw search using a query map"
  (let [instance (:instance query)]
    (raw_rpc_exec instance OBJECT :execute (:database instance) (:uid instance) (:password instance)
                  (:name ent) "search" (:domain query) (:offset query) (:limit query) (:order query) (:context query))))

(defn ids-or-search-result [ent query]
  (if (empty? (:ids query))
    (search* ent query)
    (:ids query)))

;;--Todo dry xx* function by using execute--
(defn read* [ent query]
  "Do a read using a query map"
  (let [instance (:instance query)]
    (raw_rpc_exec instance OBJECT :execute  (:database instance) (:uid instance) (:password instance) (:name ent)
                  "read" (ids-or-search-result ent query) (:fields query) (:context query))))

(defn create* [ent dataset]
  "create an entity using a dataset"
  (let [instance (:instance dataset)]
    (apply raw_rpc_exec instance OBJECT :execute  (:database instance) (:uid instance) (:password instance) (:name ent)
                  "create" (:values dataset) (:context dataset))))

(defn unlink* [ent dataset]
  "remove entity using a dataset if dataset has ids"
  (if (empty? (:ids dataset)) (throw (IllegalArgumentException. "No ids in data set")))
  (let [instance (:instance dataset)]
    (raw_rpc_exec instance OBJECT :execute  (:database instance) (:uid instance) (:password instance) (:name ent)
                  "unlink" (:ids dataset) (:context dataset))))

(defn write* [ent dataset]
  "write a/many entity using a dataset if dataset has ids"
  (if (empty? (:ids dataset)) (throw (IllegalArgumentException. "No ids in data set")))
  (let [instance (:instance dataset)]
    (raw_rpc_exec instance OBJECT :execute  (:database instance) (:uid instance) (:password instance) (:name ent)
                  "write" (:ids dataset) (:values dataset) (:context dataset))))

(defmacro browse [ent & body]
  "Read an entity you can add filter function field domain offset order limit instance
   with respective functions"
  `(let [query# (-> (create_query (get_active_instance)) ~@body)]
     (read* ~ent query#)))

(defmacro search [ent & body]
  "Search an entity. It will return list of ids. You can add filter function field
   domain offset order limit instance with respective functions"
  `(let [query# (-> (create_query (get_active_instance)) ~@body)]
     (search* ~ent query#)))

(defmacro create
  "Create an entry for given model"
  [ent & body]
  ;;call default_get???
  `(let [dataset# (-> (create_dataset (get_active_instance)) ~@body)]
     (create* ~ent dataset#)))

(defmacro write
  "Write an entry for given model (which [ids]) is mandatory"
  [ent & body]
  `(let [dataset# (-> (create_dataset (get_active_instance)) ~@body)]
     (write* ~ent dataset#)))

(defmacro unlink
  "Remove an entry for given model (which [ids]) is mandatory"
  [ent & body]
  `(let [dataset# (-> (create_dataset (get_active_instance)) ~@body)]
     (unlink* ~ent dataset#)))

(defmacro execute
  "do an xml-rpc call for url object/execute erp api"
  [ent & body]
  `(let [exec# (-> (create_command (get_active_instance)) ~@body)]
     (execute* ~ent exec#)))

(defmacro defentity
  "Define an entity representing a model in erp, applying any modifications in
  the body. inspired by the great sqlkorma library"
  [ent & body]
  `(let [e# (-> (create_entity  ~(name ent)) ~@body)]
     (println (str (string_to_erp_sym (name ~ent)) " Symbol created"))
     (def ~(symbol (string_to_erp_sym (name ent))) e#)))

;----- tools --------------
(defn col [ent]
  "List the columns of entity"
  (execute ent (args "fields_get_keys")))
