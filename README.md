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

## test

    lein test

## License

Copyright © 2014 Informatique, Inc.

Distributed under and contributions made with the [MIT](http://opensource.org/licenses/MIT) license.
