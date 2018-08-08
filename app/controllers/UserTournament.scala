package controllers

import lidraughts.app._
import lidraughts.user.UserRepo
import views._

object UserTournament extends LidraughtsController {

  def path(username: String, path: String, page: Int) = Open { implicit ctx =>
    Reasonable(page) {
      OptionFuResult(UserRepo named username) { user =>
        path match {
          case "recent" =>
            Env.tournament.leaderboardApi.recentByUser(user, page).map { entries =>
              Ok(html.userTournament.recent(user, entries))
            }
          case "best" =>
            Env.tournament.leaderboardApi.bestByUser(user, page).map { entries =>
              Ok(html.userTournament.best(user, entries))
            }
          case "chart" => Env.tournament.leaderboardApi.chart(user).map { data =>
            Ok(html.userTournament.chart(user, data))
          }
          case _ => notFound
        }
      }
    }
  }
}
