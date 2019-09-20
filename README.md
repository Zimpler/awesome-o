# awesome-o
[![Build Status](https://travis-ci.org/PugglePay/awesome-o.svg)](https://travis-ci.org/PugglePay/awesome-o)

Running on lambdas and API gateway

## Deploy

```
make deploy
```


## Usage

This is an example-based quick tour for Awesome-o. The bot is set up to work on all Slack channels in the Zimpler organisation, which means one can start an interaction by typing `bot: `.

To see the commands available

    bot: help

This gives enough information to get started, but a few concrete examples follow for completeness' sake.

Registering a new teammate as part of the onboarding process (@Maria in this case)

    bot: Maria is a puggle
    bot: Maria is in Göteborg
    bot: Maria was born on 1980-01-01

If applicable, add the person to the development team

    bot: Maria is in the dev team

Checking the details of a fellow Zimpler (@Albert in this case)

    bot: who is Albert?

**Note:** If you want to play around with the bot, the best place to do so might in the #testing-bot channel. This will lower the likelihood of spamming other Slack users in regular channels.

## Development

To run unit tests, start `redis-server` and then run `lein test`.
You can also run `lein auto test` for ongoing unit testing every time
you change a file. For interactive development start `lein repl`,
which uses `org.clojure/tools.namespace` to assist reloading the code.

To start a local web server for development you can either eval the
commented out forms at the bottom of `web.clj` from your editor or
launch from the command line:

    $ lein run -m awesome-o.web

You'll need the [heroku cli tool](https://devcenter.heroku.com/articles/heroku-cli)
installed to deploy awesome-o, aswell as either a heroku account of your own, or
an SSH-key uploaded to the pugglepay account (under the address
accounts@pugglepay.net)

A guide to checking out the repository via the heroku cli, or adding the heroku
remote to an existing repo checked out from github can be found here:
https://devcenter.heroku.com/articles/git

The cookie-backed session store needs a session secret configured for encryption:

    $ heroku config:add SESSION_SECRET=$RANDOM_16_CHARS

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
