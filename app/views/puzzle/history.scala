package views
package html.puzzle

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.paginator.Paginator
import lila.puzzle.PuzzleHistory.{ PuzzleSession, SessionRound }
import lila.puzzle.PuzzleTheme
import lila.user.User

object history:

  def apply(user: User, pager: Paginator[PuzzleSession])(using ctx: PageContext) =
    val title =
      if ctx is user then trans.puzzle.history.txt()
      else s"${user.username} ${trans.puzzle.history.txt()}"
    views.html.base.layout(
      title = title,
      moreCss = cssTag("puzzle.dashboard"),
      moreJs = infiniteScrollTag
    )(
      main(cls := "page-menu")(
        bits.pageMenu("history", user.some),
        div(cls := "page-menu__content box box-pad")(
          h1(cls := "box__top")(title),
          div(cls := "puzzle-history")(
            div(cls := "infinite-scroll")(
              pager.currentPageResults map renderSession,
              pagerNext(pager, np => routes.Puzzle.history(np, user.username.some).url)
            )
          )
        )
      )
    )

  private def renderSession(session: PuzzleSession)(using ctx: PageContext) =
    div(cls := "puzzle-history__session")(
      h2(cls := "puzzle-history__session__title")(
        strong(PuzzleTheme(session.theme).name()),
        momentFromNow(session.puzzles.head.round.date)
      ),
      div(cls := "puzzle-history__session__rounds")(session.puzzles.toList.reverse map renderRound)
    )

  private def renderRound(r: SessionRound)(using PageContext) =
    a(cls := "puzzle-history__round", href := routes.Puzzle.show(r.puzzle.id))(
      views.html.board.bits.mini(r.puzzle.fenAfterInitialMove.board, r.puzzle.color, r.puzzle.line.head.some)(
        span(cls := "puzzle-history__round__puzzle")
      ),
      span(cls := "puzzle-history__round__meta")(
        span(cls := "puzzle-history__round__result")(
          if r.round.win.yes then goodTag(trans.puzzle.solved())
          else badTag(trans.puzzle.failed())
        ),
        span(cls := "puzzle-history__round__id")(s"#${r.puzzle.id}")
      )
    )
