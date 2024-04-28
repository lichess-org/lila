package views.userTournament

import lila.app.templating.Environment.{ *, given }

import scalalib.paginator.Paginator

object bits:

  def best(u: User, pager: Paginator[lila.tournament.LeaderboardApi.TourEntry])(using PageContext) =
    layout(
      u,
      title = s"${u.username} best tournaments",
      path = "best",
      modules = infiniteScrollEsmInit
    ):
      views.userTournament.list(u, "best", pager, "BEST")

  def recent(u: User, pager: Paginator[lila.tournament.LeaderboardApi.TourEntry])(using PageContext) =
    layout(
      u,
      title = s"${u.username} recent tournaments",
      path = "recent",
      modules = infiniteScrollEsmInit
    ):
      views.userTournament.list(u, "recent", pager, pager.nbResults.toString)

  def layout(u: User, title: String, path: String, modules: EsmList = Nil)(
      body: Frag
  )(using ctx: PageContext) =
    views.base.layout(
      title = title,
      moreCss = cssTag("user-tournament"),
      modules = modules
    ):
      main(cls := "page-menu")(
        lila.ui.bits.pageMenuSubnav(
          a(cls := path.active("created"), href := routes.UserTournament.path(u.username, "created"))(
            trans.arena.created()
          ),
          ctx
            .is(u)
            .option(
              a(cls := path.active("upcoming"), href := routes.UserTournament.path(u.username, "upcoming"))(
                trans.broadcast.upcoming()
              )
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
