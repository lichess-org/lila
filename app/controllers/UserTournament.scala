package controllers

import views._

import lila.app._

final class UserTournament(env: Env) extends LilaController(env) {

  def path(username: String, path: String, page: Int) =
    Open { implicit ctx =>
      Reasonable(page) {
        OptionFuResult(env.user.repo named username) { user =>
          path match {
            case "recent" =>
              env.tournament.leaderboardApi.recentByUser(user, page).map { entries =>
                Ok(html.tournament.user.bits.recent(user, entries))
              }
            case "best" =>
              env.tournament.leaderboardApi.bestByUser(user, page).map { entries =>
                Ok(html.tournament.user.bits.best(user, entries))
              }
            case "chart" =>
              env.tournament.leaderboardApi.chart(user).map { data =>
                Ok(html.tournament.user.chart(user, data))
              }
            case "created" =>
              env.tournament.api.byOwnerPager(user, page).map { pager =>
                Ok(html.tournament.user.created(user, pager))
              }
            case "upcoming" if ctx is user =>
              env.tournament.api.upcomingByPlayerPager(user, page).map { pager =>
                Ok(html.tournament.user.upcoming(user, pager))
              }
            case _ => notFound
          }
        }
      }
    }
}
