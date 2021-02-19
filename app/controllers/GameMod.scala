package controllers

import play.api.mvc._
import views._

import lila.api.Context
import lila.app._

final class GameMod(
    env: Env
) extends LilaController(env) {

  def index(username: String) =
    Secure(_.Hunter) { implicit ctx => me =>
      OptionFuResult(env.user.repo named username) { user =>
        env.game.gameRepo.recentPovsByUserFromSecondary(user, 100) flatMap { povs =>
          env.mod.assessApi.ofPovs(povs) map { games =>
            Ok(views.html.mod.games(user, games))
          }
        }
      }
    }
}
