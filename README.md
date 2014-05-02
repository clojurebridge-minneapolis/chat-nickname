# chat-nickname

This is a ClojureBridge Minneapolis sample application

## running in development

    export NICKNAME=mynickname
    export CHAT_NICKNAME=mynickname
    export CHAT_URL=http://localhost:3000
    lein ring server

## running in production

    lein with-profile production trampoline ring server-headless

## running in production on heroku

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
    lein repl :connect http://mynickname:monkey@chat-mynickname.herokuapp.com:80/repl

NOTE: at the moment there is a bug after connecting to drawbridge :(.
Normally you should get a REPL prompt.

## test

The test suite is not really complete, but intended to demonstrate
how to setup running tests from leiningen:

    lein test

## License

Copyright © 2014 Informatique, Inc.

Distributed under and contributions made with the [MIT](http://opensource.org/licenses/MIT) license.
