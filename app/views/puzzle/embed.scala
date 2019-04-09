package views.html.puzzle

import play.api.mvc.RequestHeader

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.Lang
import views.html.base.layout.{ bits => layout }

import controllers.routes

object embed {

  private val dataStreamUrl = attr("data-stream-url")

  def apply(daily: lila.puzzle.DailyPuzzle, bg: String, board: String)(implicit req: RequestHeader, lang: Lang) = frag(
    layout.doctype,
    html(
      head(
        layout.charset,
        layout.metaCsp(basicCsp),
        st.headTitle("lichess.org chess puzzle"),
        layout.pieceSprite(lila.pref.PieceSet.default),
        responsiveCssTagWithTheme("tv.embed", bg)
      ),
      body(
        cls := s"base $board merida",
        dataStreamUrl := routes.Tv.feed
      )(
          div(id := "daily-puzzle", cls := "embedded", title := trans.clickToSolve.txt())(
            raw(daily.html),
            div(cls := "vstext", style := "text-align: center; justify-content: center")(
              trans.puzzleOfTheDay.frag(), br,
              daily.color.fold(trans.whitePlays, trans.blackPlays).frag()
            )
          ),
          jQueryTag,
          jsAt("javascripts/vendor/chessground.min.js", false),
          jsAt("compiled/puzzle.js", false)
        )
    )
  )
}
