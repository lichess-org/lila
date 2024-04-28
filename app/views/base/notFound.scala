package views.base

import lila.app.templating.Environment.{ *, given }

def notFound()(using PageContext) =
  layout(
    title = "Page not found",
    moreCss = cssTag("not-found")
  ):
    main(cls := "not-found page-small box box-pad")(
      header(
        h1("404"),
        div(
          strong("Page not found!"),
          p(
            "Return to ",
            a(href := routes.Lobby.home)("the homepage"),
            span(cls := "or-play")(" or play this mini-game")
          )
        )
      ),
      div(cls := "game")(
        iframe(
          src            := assetUrl(s"vendor/ChessPursuit/bin-release/index.html"),
          st.frameborder := 0,
          widthA         := 400,
          heightA        := 500,
          frame.credentialless
        ),
        p(cls := "credits")(
          a(href := "https://github.com/Saturnyn/ChessPursuit")("ChessPursuit"),
          " courtesy of ",
          a(href := "https://github.com/Saturnyn")("Saturnyn")
        )
      )
    )
