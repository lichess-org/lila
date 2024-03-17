package views
package html.puzzle

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.puzzle.Puzzle
import lila.user.User

object submitted {

  def apply(query: String, user: Option[User], puzzles: Option[Paginator[Puzzle]], count: Option[Int])(
      implicit ctx: Context
  ) =
    views.html.base.layout(
      title = user.fold("Submitted puzzles")(u => s"Submitted puzzles from ${u.username}"),
      moreCss = cssTag("puzzle.page"),
      moreJs = infiniteScrollTag
    )(
      main(cls := "page-menu")(
        bits.pageMenu("submitted"),
        div(cls := "page-menu__content puzzle-of-player box box-pad")(
          form(
            action := routes.Puzzle.submitted(),
            method := "get",
            cls    := "form3 puzzle-of-player__form complete-parent"
          )(
            st.input(
              name         := "name",
              value        := query,
              cls          := "form-control user-autocomplete",
              placeholder  := trans.clas.lishogiUsername.txt(),
              autocomplete := "off",
              dataTag      := "span",
              autofocus
            ),
            submitButton(cls := "button")(trans.puzzle.searchPuzzles.txt())
          ),
          a(
            cls      := "button button-green submit-puzzle",
            dataIcon := "O",
            href     := routes.Puzzle.newPuzzlesForm.url
          ),
          div(cls := "puzzle-of-player__results")(
            (user, puzzles) match {
              case (Some(u), Some(pager)) =>
                if (pager.nbResults == 0 && ctx.is(u))
                  p(
                    if (~count > 0)
                      s"Puzzles you have submitted are being verified (${~count} puzzles in queue). Thank you!"
                    else "You have not submitted any puzzles, but you can always change that."
                  )
                else
                  frag(
                    p(strong(s"Found ${pager.nbResults} puzzles", userLink(u))),
                    p(strong(s"Found ${~count} puzzles in queue")),
                    div(cls := "puzzle-of-player__pager infinite-scroll")(
                      pager.currentPageResults.map { puzzle =>
                        div(cls := "puzzle-of-player__puzzle")(
                          views.html.puzzle.bits.miniTag(
                            sfen = puzzle.sfenAfterInitialMove,
                            color = puzzle.color,
                            lastUsi = puzzle.lastUsi
                          )(
                            a(
                              cls  := s"puzzle-of-player__puzzle__board",
                              href := routes.Puzzle.show(puzzle.id.value)
                            )
                          ),
                          span(cls   := "puzzle-of-player__puzzle__meta")(
                            span(cls := "puzzle-of-player__puzzle__id", s"#${puzzle.id}"),
                            span(cls := "puzzle-of-player__puzzle__rating", puzzle.glicko.intRating)
                          )
                        )
                      },
                      pagerNext(pager, np => s"${routes.Puzzle.submitted(u.username.some, np).url}")
                    )
                  )
              case (_, _) => emptyFrag
            }
          )
        )
      )
    )
}
