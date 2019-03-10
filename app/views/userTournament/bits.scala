package views.html
package userTournament

import play.twirl.api.Html

import lila.api.Context
import lila.common.paginator.Paginator
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

import controllers.routes

object bits {

  def best(u: User, pager: Paginator[lila.tournament.LeaderboardApi.TourEntry])(implicit ctx: Context) =
    layout(
      u,
      title = s"${u.username} tournaments",
      path = "best",
      moreJs = infiniteScrollTag
    ) {
      list(u, "best", pager, "Best results", "BEST")
    }

  def recent(u: User, pager: Paginator[lila.tournament.LeaderboardApi.TourEntry])(implicit ctx: Context) =
    layout(
      u,
      title = s"${u.username} tournaments",
      path = "recent",
      moreJs = infiniteScrollTag
    ) {
      list(u, "recent", pager, "Recently played", pager.nbResults.toString)
    }

  def layout(u: User, title: String, path: String, moreJs: Html = emptyHtml)(body: Html)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      responsive = true,
      moreCss = responsiveCssTag("user-tournament"),
      moreJs = moreJs
    ) {
        main(cls := "page-menu")(
          st.nav(cls := "page-menu__menu subnav")(
            a(cls := path.active("recent"), href := routes.UserTournament.path(u.username, "recent"))(
              "Recently played"
            ),
            a(cls := path.active("best"), href := routes.UserTournament.path(u.username, "best"))(
              "Best results"
            ),
            a(cls := path.active("chart"), href := routes.UserTournament.path(u.username, "chart"))(
              "Stats"
            )
          ),
          div(cls := "page-menu__content box")(body)
        )
      }
}
