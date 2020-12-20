package controllers

import lila.app._
import views._

final class UserTournament(env: Env) extends LilaController(env) {

  def path(username: String, path: String, page: Int) =
    Open { implicit ctx =>
      Reasonable(page) {
        OptionFuResult(env.user.repo named username) { user =>
          path match {
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
            case _ => notFound
          }
        }
      }
    }
}
