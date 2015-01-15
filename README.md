# awesome-o

A [Heroku](http://www.heroku.com) web app using Compojure.

This generated project has a few basics set up beyond the bare Compojure defaults:

* Cookie-backed session store
* Stack traces when in development
* Environment-based config via [environ](https://github.com/weavejester/environ)
* [HTTP-based REPL debugging](https://devcenter.heroku.com/articles/debugging-clojure) via [drawbridge](https://github.com/cemerick/drawbridge)

## Usage

To start a local web server for development you can either eval the
commented out forms at the bottom of `web.clj` from your editor or
launch from the command line:

    $ lein run -m awesome-o.web

You'll need the [heroku toolbelt](https://toolbelt.herokuapp.com)
installed to deploy awesome-o, aswell as either a heroku account of your own, or
an SSH-key uploaded to the pugglepay account (under the address
accounts@pugglepay.net)

A guide to checking out the repository via the heroku cli, or adding the heroku
remote to an existing repo checked out from github can be found here:
https://devcenter.heroku.com/articles/git

The cookie-backed session store needs a session secret configured for encryption:

    $ heroku config:add SESSION_SECRET=$RANDOM_16_CHARS

## Remote REPL

The [devcenter article](https://devcenter.heroku.com/articles/debugging-clojure)
has a detailed explanation, but using the `repl` task from Leiningen
2.x lets you connect a REPL to a remote process over HTTP. The first
step is setting up credentials:

    $ heroku config:add REPL_USER=[...] REPL_PASSWORD=[...]

Then you can launch the REPL:

    $ lein repl :connect http://$REPL_USER:$REPL_PASSWORD@awesome-o.herokuapp.com/repl

Everything you enter will be evaluated remotely in the running dyno,
which can be very useful for debugging or inspecting live data.

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
