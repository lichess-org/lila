package views
package html.puzzle

import controllers.routes
import play.api.i18n.Lang

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.puzzle.PuzzleRound
import lila.user.User

object history {

  def apply(user: User, page: Int, pager: Paginator[PuzzleRound.WithPuzzle])(implicit ctx: Context) =
    views.html.base.layout(
      title = "Puzzle history",
      moreCss = cssTag("puzzle.dashboard")
    )(
      main(cls := "page-menu")(
        bits.pageMenu("history"),
        div(cls := "page-menu__content box box-pad")(
          h1(trans.puzzle.history()),
          div(cls := "puzzle-history")(
            div(cls := "infinite-scroll")(
              div(cls := "puzzle-history__session")(pager.currentPageResults map widget),
              pagerNext(pager, np => routes.Puzzle.history(np).url)
            )
          )
        )
      )
    )

  private def widget(pr: PuzzleRound.WithPuzzle)(implicit ctx: Context) = pr match {
    case PuzzleRound.WithPuzzle(round, puzzle) =>
      a(cls := "puzzle-history__round", href := routes.Puzzle.show(puzzle.id.value))(
        views.html.board.bits.mini(puzzle.fenAfterInitialMove, puzzle.color, puzzle.line.head.uci)(
          span(cls := "puzzle-history__round__puzzle")
        ),
        span(cls := "puzzle-history__round__meta")(
          span(cls := "puzzle-history__round__result")(
            if (round.win) goodTag(trans.puzzle.won())
            else badTag(trans.puzzle.failed())
          ),
          span(cls := "puzzle-history__round__id")(s"#${puzzle.id.value}")
        )
      )
  }
}
