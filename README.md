# chat-nickname

This is a ClojureBridge Minneapolis sample application

## running in development

    export CHAT_NICKNAME=mynickname
    export CHAT_URL=http://localhost:3000
    lein run

## running in production

    lein with-profile production trampoline run

## running in production on heroku

NOTE: the above line is exactly what's in ```Procfile```

    heroku apps:create chat-mynickname
    heroku config:add CHAT_NICKNAME=mynickname CHAT_URL=http://chat-mynickname.herokuapp.com
    git push heroku master
    heroku open

## connecting to the remote REPL

It's possible to connect to your running Clojure program
with a REPL via drawbridge. You will need to setup a password
to use when connecting like this (locally):

    export REPL_PASSWORD=monkey
    lein repl :connect http://mynickname:monkey@localhost:3000/repl

Or set a Heroku config variable like this:

    heroku config:add REPL_PASSWORD=monkey

    $ lein repl :connect http://mynickname:monkey@chat-mynickname.herokuapp.com:80/repl
    Connecting to nREPL at http://mynickname:monkey@chat-mynickname.herokuapp.com:80/repl
    REPL-y 0.3.0
    Clojure 1.6.0
        Docs: (doc function-name-here)
              (find-doc "part-of-name-here")
      Source: (source function-name-here)
     Javadoc: (javadoc java-object-or-class-here)
        Exit: Control+D or (exit) or (quit)
     Results: Stored in vars *1, *2, *3, an exception in *e

    user=> (ns chat-nickname.web)
    nil
    chat-nickname.web=> @data
    {:servers {"http://ficelle.info9.net:1234" {:url "http://ficelle.info9.net:1234", :updated 1399074722830}}, :users {}}
    chat-nickname.web=> (exit)
    Bye for now!
    $

## test

The test suite is not really complete, but intended to demonstrate
how to setup running tests from leiningen:

    lein test

## License

Copyright © 2014 Informatique, Inc.

Distributed under and contributions made with the [MIT](http://opensource.org/licenses/MIT) license.
