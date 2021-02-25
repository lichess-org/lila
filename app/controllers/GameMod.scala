package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import views._

import lila.db.dsl._
import lila.api.Context
import lila.app._

final class GameMod(env: Env) extends LilaController(env) {

  import GameMod._

  def index(username: String) =
    SecureBody(_.Hunter) { implicit ctx => me =>
      OptionFuResult(env.user.repo named username) { user =>
        implicit def req = ctx.body
        val form         = filterForm.bindFromRequest()
        val filter       = form.fold(_ => Filter(none), identity)
        env.tournament.leaderboardApi.recentByUser(user, 1) flatMap { tours =>
          env.game.gameRepo.recentPovsByUserFromSecondary(user, 100, toDbSelect(filter)) flatMap { povs =>
            env.mod.assessApi.ofPovs(povs) map { games =>
              Ok(views.html.mod.games(user, form, games, tours.currentPageResults))
            }
          }
        }
      }
    }
}

object GameMod {

  case class Filter(tournament: Option[String])

  def toDbSelect(filter: Filter): Bdoc = filter.tournament ?? { tid =>
    $doc(lila.game.Game.BSONFields.tournamentId -> tid)
  }

  val filterForm =
    Form(
      mapping(
        "tournament" -> optional(nonEmptyText)
      )(Filter.apply)(Filter.unapply _)
    )
}
