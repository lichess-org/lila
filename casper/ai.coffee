casper.test.begin 'ai', 5, (test) ->
  casper.start "http://en.l.org", ->
    link = document.querySelector("#start_buttons a.config_ai")
    casper.thenClick(link) ->
      console.log("done")

      casper.run -> test.done()
