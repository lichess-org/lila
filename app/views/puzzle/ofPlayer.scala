package views
package html.puzzle

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.puzzle.Puzzle
import lila.user.User

object ofPlayer {

  def apply(query: String, user: Option[User], puzzles: Option[Paginator[Puzzle]])(implicit ctx: Context) =
    views.html.base.layout(
      title = user.fold("Lookup puzzles from a player's games")(u => s"Puzzles from ${u.username}' games"),
      moreCss = cssTag("puzzle.page"),
      moreJs = infiniteScrollTag
    )(
      main(cls := "page-menu")(
        bits.pageMenu("player"),
        div(cls := "page-menu__content puzzle-of-player box box-pad")(
          form(
            action := routes.Puzzle.ofPlayer(),
            method := "get",
            cls := "form3 puzzle-of-player__form complete-parent"
          )(
            st.input(
              name := "name",
              value := query,
              cls := "form-control user-autocomplete",
              placeholder := "Lichess username",
              autocomplete := "off",
              dataTag := "span",
              autofocus
            ),
            submitButton(cls := "button")("Search puzzles")
          ),
          div(cls := "puzzle-of-player__results")(
            (user, puzzles) match {
              case (Some(u), Some(pager)) =>
                if (pager.nbResults == 0 && ctx.is(u))
                  p(
                    "You have no puzzles in the database, but Lichess still loves you very much.",
                    br,
                    "Play rapid and classical games to increase your chances of having a puzzle of yours added!"
                  )
                else
                  frag(
                    p(strong(pager.nbResults), " puzzles found in ", userLink(u), " games."),
                    div(cls := "puzzle-of-player__pager infinite-scroll")(
                      pager.currentPageResults.map { puzzle =>
                        div(cls := "puzzle-of-player__puzzle")(
                          views.html.board.bits.mini(
                            fen = puzzle.fenAfterInitialMove,
                            color = puzzle.color,
                            lastMove = puzzle.line.head.uci
                          )(
                            a(
                              cls := s"puzzle-of-player__puzzle__board",
                              href := routes.Puzzle.show(puzzle.id.value)
                            )
                          ),
                          span(cls := "puzzle-of-player__puzzle__meta")(
                            span(cls := "puzzle-of-player__puzzle__id", s"#${puzzle.id}"),
                            span(cls := "puzzle-of-player__puzzle__rating", puzzle.glicko.intRating)
                          )
                        )
                      },
                      pagerNext(pager, np => s"${routes.Puzzle.ofPlayer(u.username.some, np).url}")
                    )
                  )
              case (_, _) => emptyFrag
            }
          )
        )
      )
    )
}
