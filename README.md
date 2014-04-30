# chat-nickname

This is a ClojureBridge Minneapolis sample application

## running in development

export NICKNAME=mynickname
lein ring server

NOTE: https://github.com/weavejester/lein-ring
If the LEIN_NO_DEV environment variable is not set, the server will monitor your source directory for file modifications, and any altered files will automatically be reloaded.


## running in production

lein with-profile production trampoline ring server-headless

## running in production on heroku

## test

lein test

## License

Copyright © 2014 Informatique, Inc.

Distributed under and contributions made with the [MIT](http://opensource.org/licenses/MIT) license.
