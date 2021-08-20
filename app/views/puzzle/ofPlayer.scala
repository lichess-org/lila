package views
package html.puzzle

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.puzzle.Puzzle
import lila.user.User
import play.api.data.Form

object ofPlayer {

  def apply(form: Form[_], user: Option[User], puzzles: Option[Paginator[Puzzle]])(implicit ctx: Context) =
    views.html.base.layout(
      title = user.fold(trans.puzzle.lookupOfPlayer.txt())(u => trans.puzzle.fromXGames.txt(u.username)),
      moreCss = cssTag("puzzle.page"),
      moreJs = infiniteScrollTag
    )(
      main(cls := "page-menu")(
        bits.pageMenu("player"),
        div(cls := "page-menu__content puzzle-of-player box box-pad")(
          st.form(
            action := routes.Puzzle.ofPlayer(),
            method := "get",
            cls := "form3 puzzle-of-player__form complete-parent"
          )(
            form3.input(form("name"), klass = "user-autocomplete")(
              autofocus,
              autocomplete := "off",
              dataTag := "span",
              placeholder := trans.clas.lichessUsername.txt()
            ),
            st.label(`for` := form3.id(form("rating.sort.order")), cls := "form-label")("Sort by rating"),
            form3.select(form("rating.sort.order"), Seq(("desc", "Descending"), ("asc", "Ascending"))),
            submitButton(cls := "button")(trans.puzzle.searchPuzzles.txt())
          ),
          div(cls := "puzzle-of-player__results")(
            (user, puzzles) match {
              case (Some(u), Some(pager)) =>
                if (pager.nbResults == 0 && ctx.is(u))
                  p(trans.puzzle.fromMyGamesNone())
                else
                  frag(
                    p(strong(trans.puzzle.fromXGamesFound((pager.nbResults), userLink(u)))),
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
                      pagerNext(
                        pager,
                        np =>
                          form.data.foldLeft(routes.Puzzle.ofPlayer(np).url)((url, params) =>
                            addQueryParameter(url, params._1, params._2)
                          )
                      )
                    )
                  )
              case (_, _) => emptyFrag
            }
          )
        )
      )
    )
}
