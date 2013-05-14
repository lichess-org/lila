casper.test.begin 'homepage', 5, (test) ->
    casper.start "http://en.l.org", ->
        casper.test.assertTitle "Lichess", "page title"

    casper.run -> test.done()
