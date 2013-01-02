# carcajou
Clojure abstraction library for OpenObject/OpenERP I wrote to learn Clojure

## Getting started
Simply add carcajou as a dependency to your lein/cake project:

```clojure
[carcajou "0.2-beta"]
```

## Usage

```clojure
(use '[carcajou.core])

;; Instanciate a connexion to an instance and set it as default
(set_active_instance (definstance  "6_1_2439.runbot.openerp.com" "6_1_2439_all"))

;; The definstance function can take as optional argument a dict that overwrite non mandatory parameters
(definstance  "6_1_2439.runbot.openerp.com" "6_1_2439_all" :port 8080)
;; keys are :port, :protocol "http"/"https", :user, :password, :database

;; You can define entites that correspond to OpenObject Model
(defentity "res.country")
ResCountry Symbol created
#'user/ResCountry

;; You can read model
(browse ResCountry)

;; You can specifiy ids
(browse ResCountry (which 77 78))

;; You can add other option to read
(browse ResCountry (limit 1) (fields "code", "name") (domain "|" ["code" "=" "CH"] ["code" "=" "FR"]))
[{:code "FR", :name "France", :id 76}]
;; (domain xx) (order code) (offset 1) (limit 10) (context {}) are supported

;; You can search in the same way
(search ResCountry (limit 1) (fields "code", "name") (domain ["code" "=" "CH"]))
[44]
;; (domain xx) (order code) (offset 1) (limit 10) (context {}) are supported

;; To create an entry
(create ResCountry (values {"name" "The Shire Middle Hearth", "code" "TS"}))
265

;; To unlink
(unlink ResCountry (which 77 78))
true

;; To write
(write ResCountry (which 265) (values {"code" "TSHM"}))
true

;; Arbitrary execute
(execute ResCountry (args "read" 256))
{:id 265, :code "TS", :name "The Shire Middle Earth", :intrastat false}

;; All this macros support the instance function
(execute ResCountry (args "read" 265)
          (instance (definstance  "6_1_2439.runbot.openerp.com" "6_1_2439_all")))
```

Next step will be to add (where domain) in write and unlink function

## License

Copyright © 2012 Nicolas Bessi

Distributed under the MIT licence