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
        val filter       = form.fold(_ => emptyFilter, identity)
        env.tournament.leaderboardApi.recentByUser(user, 1) zip
          env.activity.read.recentSwissRanks(user.id) zip
          env.game.gameRepo.recentPovsByUserFromSecondary(
            user,
            100,
            toDbSelect(filter) ++ lila.game.Query.finished
          ) flatMap { case ((arenas, swisses), povs) =>
            env.mod.assessApi.makeAndGetFullOrBasicsFor(povs) map { games =>
              Ok(views.html.mod.games(user, form, games, arenas.currentPageResults, swisses))
            }
          }
      }
    }

  private def guessSwisses(user: lila.user.User): Fu[Seq[lila.swiss.Swiss]] = fuccess(Nil)
}

object GameMod {

  case class Filter(arena: Option[String], swiss: Option[String], opponents: Option[String]) {
    def opponentIds: List[lila.user.User.ID] =
      (~opponents)
        .take(800)
        .replace(",", " ")
        .split(' ')
        .view
        .flatMap(_.trim.some.filter(_.nonEmpty))
        .filter(lila.user.User.couldBeUsername)
        .map(lila.user.User.normalize)
        .toList
        .distinct
  }

  val emptyFilter = Filter(none, none, none)

  def toDbSelect(filter: Filter): Bdoc = filter.arena.?? { id =>
    $doc(lila.game.Game.BSONFields.tournamentId -> id)
  } ++ filter.swiss.?? { id =>
    $doc(lila.game.Game.BSONFields.swissId -> id)
  } ++ (filter.opponentIds match {
    case Nil      => $empty
    case List(id) => $and(lila.game.Game.BSONFields.playerUids $eq id)
    case ids      => $and(lila.game.Game.BSONFields.playerUids $in ids)
  })

  val filterForm =
    Form(
      mapping(
        "arena"     -> optional(nonEmptyText),
        "swiss"     -> optional(nonEmptyText),
        "opponents" -> optional(nonEmptyText)
      )(Filter.apply)(Filter.unapply _)
    )
}
