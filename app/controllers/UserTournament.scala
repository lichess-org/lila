package controllers

import lila.app._
import play.api.mvc._
import views._
import lila.user.UserRepo

object UserTournament extends LilaController {

  def all(username: String) = Open { implicit ctx =>
    OptionFuOk(UserRepo named username) { user =>
      Env.tournament.leaderboardApi.recentByUser(user, 50).map { entries =>
        html.userTournament.all(user, entries)
      }
    }
  }

  def best(username: String) = Open { implicit ctx =>
    OptionFuOk(UserRepo named username) { user =>
      Env.tournament.leaderboardApi.bestByUser(user, 50).map { entries =>
        html.userTournament.best(user, entries)
      }
    }
  }
  def chart(username: String) = Open { implicit ctx =>
    ???
  }
}
