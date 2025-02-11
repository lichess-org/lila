package views.html
package tournament.user

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.user.User

object bits {

  def best(u: User, pager: Paginator[lila.tournament.LeaderboardApi.TourEntry])(implicit
      ctx: Context,
  ) =
    layout(
      u,
      title = s"${u.username} best tournaments",
      path = "best",
      moreJs = infiniteScrollTag,
    ) {
      views.html.tournament.user.list(u, "best", pager, "BEST")
    }

  def recent(u: User, pager: Paginator[lila.tournament.LeaderboardApi.TourEntry])(implicit
      ctx: Context,
  ) =
    layout(
      u,
      title = s"${u.username} recent tournaments",
      path = "recent",
      moreJs = infiniteScrollTag,
    ) {
      views.html.tournament.user.list(u, "recent", pager, pager.nbResults.toString)
    }

  def layout(u: User, title: String, path: String, moreJs: Frag = emptyFrag)(
      body: Frag,
  )(implicit ctx: Context) = {
    val paths = List(
      ("created", trans.tourCreated()),
      ("recent", trans.tourRecent()),
      ("best", trans.bestResults()),
      ("chart", trans.stats()),
    )
    views.html.base.layout(
      title = title,
      moreCss = cssTag("tournament.user"),
      moreJs = frag(
        jsTag("tournament.user"),
        moreJs,
      ),
    ) {
      main(cls := "page-menu")(
        views.html.tournament.home.menu("user"),
        div(cls := "page-menu__content box")(
          div(cls := "tournamen-search")(
            st.input(
              name         := "name",
              value        := u.username,
              cls          := "form-control user-autocomplete",
              placeholder  := trans.clas.lishogiUsername.txt(),
              autocomplete := "off",
              dataTag      := "span",
            ),
            button(cls := "button")(trans.study.searchByUsername.txt()),
          ),
          div(cls := "angle-content")(
            div(cls := "number-menu number-menu--tabs menu-box-pop")(
              paths.map { ps =>
                a(
                  cls  := s"nm-item to-${ps._1}${(ps._1 == path) ?? " active"}",
                  href := routes.UserTournament.path(u.username, ps._1),
                )(ps._2)
              },
            ),
            body,
          ),
        ),
      )
    }
  }
}
