# carcajou
Clojure abstraction library for OpenObject/OpenERP, I wrote to learn Clojure

## Usage

```clojure
(use '[carcajou.core])

;; instanciate a connexion to an instance and set it as default
(set_active_instance (definstance  "6_1_2439.runbot.openerp.com" "6_1_2439_all"))

;; the definstance function can take as optional argument a dict that overwrite non mandatory parameters
(definstance  "6_1_2439.runbot.openerp.com" "6_1_2439_all" :port 8080)
;; keys are :port, :protocol "http"/"https", :user, :password, :database

;;You can define entites that correspond to OpenObject Model
(defentity "res.country")
ResCountry Symbol created
#'user/ResCountry

;;You can read model
(browse ResCountry)

;;You can add other option to read
(browse ResCountry (limit 1) (fields "code", "name") (domain "|" ["code" "=" "CH"] ["code" "=" "FR"]))
[{:code "FR", :name "France", :id 76}]
;;(offset 1) (context {}) are also supported

;;You can search in the same way
(search ResCountry (limit 1) (fields "code", "name") (domain ["code" "=" "CH"]))
[44]

;;To create an entry
(create ResCountry (values {"name" "The Shire Middle Hearth", "code" "TS"}))
265

;;To write
(write ResCountry (which 265) (values {"code" "TSHM"}))
true

;;Arbitrary execute
(execute ResCountry (args "read" 256))
{:address_format "%(street)s\n%(street2)s\n%(city)s,%(state_code)s %(zip)s\n%(country_name)s", :id 265, :code "TS", :name "The Shire Middle Hearth", :intrastat false}

;;unlink is not yet implemented

;;All this macros support the instance function
(execute ResCountry (args "read" 265) (instance (definstance  "6_1_2439.runbot.openerp.com" "6_1_2439_all")))
{:instance {:uid 1, :protocol http, :database 6_1_2439_all, :port 80, :url 6_1_2439.runbot.openerp.com, :user admin, :password admin}, :args (read 265)}

```

## License

Copyright © 2012 Nicolas Bessi

Distributed under the MIT licence