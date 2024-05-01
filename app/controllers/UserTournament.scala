package controllers

import lila.app.{ *, given }

final class UserTournament(env: Env) extends LilaController(env):

  def path(username: UserStr, path: String, page: Int) = Open:
    Reasonable(page):
      Found(meOrFetch(username).map(_.filter(_.enabled.yes || isGrantedOpt(_.SeeReport)))): user =>
        path match
          case "recent" =>
            env.tournament.leaderboardApi.recentByUser(user, page).flatMap { entries =>
              Ok.page(views.userTournament.recent(user, entries))
            }
          case "best" =>
            env.tournament.leaderboardApi.bestByUser(user, page).flatMap { entries =>
              Ok.page(views.userTournament.best(user, entries))
            }
          case "chart" =>
            env.tournament.leaderboardApi.chart(user).flatMap { data =>
              Ok.page(views.userTournament.chart(user, data))
            }
          case "created" =>
            env.tournament.api.byOwnerPager(user, page).flatMap { pager =>
              Ok.page(views.userTournament.created(user, pager))
            }
          case "upcoming" if ctx.is(user) => // only mine because it's very expensive
            env.tournament.api.upcomingByPlayerPager(user, page).flatMap { pager =>
              Ok.page(views.userTournament.upcoming(user, pager))
            }
          case "upcoming" =>
            Found(ctx.me): me =>
              Redirect(routes.UserTournament.path(me.username, "upcoming"))
          case _ => notFound
