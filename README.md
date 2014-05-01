# chat-nickname

This is a ClojureBridge Minneapolis sample application

## running in development

    export NICKNAME=mynickname
    lein ring server

## running in production

    lein with-profile production trampoline ring server-headless

## running in production on heroku

    heroku apps:create chat-mynickname
    heroku config:add NICKNAME=mynickname
    git push heroku master
    heroku open

## test

    lein test

## License

Copyright © 2014 Informatique, Inc.

Distributed under and contributions made with the [MIT](http://opensource.org/licenses/MIT) license.
