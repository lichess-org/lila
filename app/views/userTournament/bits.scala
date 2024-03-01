package views.html
package userTournament

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.paginator.Paginator
import lila.user.User

import controllers.routes

object bits:

  def best(u: User, pager: Paginator[lila.tournament.LeaderboardApi.TourEntry])(using PageContext) =
    layout(
      u,
      title = s"${u.username} best tournaments",
      path = "best",
      moreJs = infiniteScrollTag
    ):
      views.html.userTournament.list(u, "best", pager, "BEST")

  def recent(u: User, pager: Paginator[lila.tournament.LeaderboardApi.TourEntry])(using PageContext) =
    layout(
      u,
      title = s"${u.username} recent tournaments",
      path = "recent",
      moreJs = infiniteScrollTag
    ):
      views.html.userTournament.list(u, "recent", pager, pager.nbResults.toString)

  def layout(u: User, title: String, path: String, moreJs: Frag = emptyFrag)(
      body: Frag
  )(using ctx: PageContext) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("user-tournament"),
      moreJs = moreJs
    ):
      main(cls := "page-menu")(
        views.html.site.bits.pageMenuSubnav(
          a(cls := path.active("created"), href := routes.UserTournament.path(u.username, "created"))(
            trans.arena.created()
          ),
          ctx.is(u) option
            a(cls := path.active("upcoming"), href := routes.UserTournament.path(u.username, "upcoming"))(
              trans.broadcast.upcoming()
            ),
          a(cls := path.active("recent"), href := routes.UserTournament.path(u.username, "recent"))(
            trans.arena.recentlyPlayed()
          ),
          a(cls := path.active("best"), href := routes.UserTournament.path(u.username, "best"))(
            trans.arena.bestResults()
          ),
          a(cls := path.active("chart"), href := routes.UserTournament.path(u.username, "chart"))(
            trans.arena.stats()
          )
        ),
        div(cls := "page-menu__content box")(body)
      )
