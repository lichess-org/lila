package views
package html.puzzle

import controllers.routes
import play.api.i18n.Lang
import play.api.libs.json.Json

import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.puzzle.PuzzleTheme

object bits {

  def miniTag(sfen: shogi.format.forsyth.Sfen, color: shogi.Color = shogi.Sente, lastUsi: String = "")(
      tag: Tag
  ): Tag =
    tag(
      cls         := "mini-board parse-sfen",
      dataColor   := color.name,
      dataSfen    := sfen.value,
      dataLastUsi := lastUsi
    )(div(cls     := s"sg-wrap d-9x9 orientation-${color.name}"))

  def daily(p: lila.puzzle.Puzzle, sfen: shogi.format.forsyth.Sfen, lastUsi: String) =
    miniTag(sfen, p.color, lastUsi)(span)

  lazy val jsonThemes = PuzzleTheme.all
    .collect {
      case t if t != PuzzleTheme.mix => t.key
    }
    .partition(PuzzleTheme.staticThemes.contains) match {
    case (static, dynamic) =>
      Json.obj(
        "dynamic" -> dynamic.map(_.value).sorted.mkString(" "),
        "static"  -> static.map(_.value).mkString(" ")
      )
  }

  def pageMenu(active: String, days: Int = 30)(implicit lang: Lang) =
    st.nav(cls := "page-menu__menu subnav")(
      a(href := routes.Puzzle.home)(
        trans.puzzles()
      ),
      a(href := routes.Puzzle.show("tsume"))(
        trans.puzzleTheme.tsume()
      ),
      a(cls := active.active("themes"), href := routes.Puzzle.themes)(
        trans.puzzle.puzzleThemes()
      ),
      a(cls := active.active("dashboard"), href := routes.Puzzle.dashboard(days, "dashboard"))(
        trans.puzzle.puzzleDashboard()
      ),
      a(cls := active.active("improvementAreas"), href := routes.Puzzle.dashboard(days, "improvementAreas"))(
        trans.puzzle.improvementAreas()
      ),
      a(cls := active.active("strengths"), href := routes.Puzzle.dashboard(days, "strengths"))(
        trans.puzzle.strengths()
      ),
      a(cls := active.active("history"), href := routes.Puzzle.history(1))(
        trans.puzzle.history()
      ),
      a(cls := active.active("player"), href := routes.Puzzle.ofPlayer())(
        trans.puzzle.fromMyGames()
      ),
      a(cls := active.active("submitted"), href := routes.Puzzle.submitted())(
        trans.puzzle.submissions()
      )
    )
}
