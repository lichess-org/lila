package lila.game

import akka.actor._
import akka.routing.RoundRobinRouter
import chess.Speed
import lila.db.api._
import lila.db.Implicits._
import lila.user.tube.userTube
import lila.user.{ User, UserRepo, SpeedElos, VariantElos, SubElo }
import play.api.libs.json.Json
import tube.gameTube

private[game] final class ComputeElos(system: ActorSystem) {

  private lazy val eloCalculator = new chess.EloCalculator(false)

  private val router = system.actorOf(Props(new Actor {
    def receive = {
      case user: User ⇒ {
        loginfo("Computing elo of " + user.id)
        apply(user).await
      }
    }
  }).withRouter(RoundRobinRouter(4)), "compute-elos-router")

  def all: Funit = $enumerate[Option[User]](usersQuery) { userOption ⇒
    userOption foreach router.!
    funit
  }

  def apply(user: User): Funit = $enumerate.fold[Option[Game], User](gamesQuery(user))(
    user.copy(speedElos = SpeedElos.default, variantElos = VariantElos.default)
  ) {
    case (user, gameOption) ⇒ (for {
      game ← gameOption
      player ← game player user
      opponentElo ← game.opponent(player).elo
    } yield user.copy(
      speedElos = {
        val speed = Speed(game.clock)
        val speedElo = user.speedElos(speed)
        val opponentSpeedElo = SubElo(0, opponentElo)
        val (white, black) = player.color.fold[(eloCalculator.User, eloCalculator.User)](
          speedElo -> opponentSpeedElo,
          opponentSpeedElo -> speedElo)
        val newElos = eloCalculator.calculate(white, black, game.winnerColor)
        val newElo = player.color.fold(newElos._1, newElos._2)
        user.speedElos.addGame(speed, newElo)
      },
      variantElos = {
        val variantElo = user.variantElos(game.variant)
        val opponentVariantElo = SubElo(0, opponentElo)
        val (white, black) = player.color.fold[(eloCalculator.User, eloCalculator.User)](
          variantElo -> opponentVariantElo,
          opponentVariantElo -> variantElo)
        val newElos = eloCalculator.calculate(white, black, game.winnerColor)
        val newElo = player.color.fold(newElos._1, newElos._2)
        user.variantElos.addGame(game.variant, newElo)
      }
    )
    ) | user
  } flatMap { user ⇒
    UserRepo.setSpeedElos(user.id, user.speedElos) >>
      UserRepo.setVariantElos(user.id, user.variantElos)
  }

  private def usersQuery = $query.apply[User](
    Json.obj(
      "count.rated" -> $gt(0)
    )) sort ($sort desc "seenAt")

  private def gamesQuery(user: User) = $query.apply[Game](
    Query.finished ++ Query.rated ++ Query.user(user.id)
  ) sort ($sort asc Game.BSONFields.createdAt)

}
