package controllers

import lila.app.{ given, * }
import views.*

final class UserTournament(env: Env) extends LilaController(env):

  def path(username: UserStr, path: String, page: Int) = Open:
    Reasonable(page):
      val userOption =
        env.user.repo.byId(username).map { _.filter(_.enabled.yes || isGranted(_.SeeReport)) }
      OptionFuResult(userOption): user =>
        path match
          case "recent" =>
            env.tournament.leaderboardApi.recentByUser(user, page).map { entries =>
              Ok(html.userTournament.bits.recent(user, entries))
            }
          case "best" =>
            env.tournament.leaderboardApi.bestByUser(user, page).map { entries =>
              Ok(html.userTournament.bits.best(user, entries))
            }
          case "chart" =>
            env.tournament.leaderboardApi.chart(user).map { data =>
              Ok(html.userTournament.chart(user, data))
            }
          case "created" =>
            env.tournament.api.byOwnerPager(user, page).map { pager =>
              Ok(html.userTournament.created(user, pager))
            }
          case "upcoming" if ctx is user => // only mine because it's very expensive
            env.tournament.api.upcomingByPlayerPager(user, page).map { pager =>
              Ok(html.userTournament.upcoming(user, pager))
            }
          case "upcoming" =>
            ctx.me.fold(notFound) { me =>
              Redirect(routes.UserTournament.path(me.username, "upcoming")).toFuccess
            }
          case _ => notFound
