package lila.push

import akka.actor._
import akka.pattern.ask
import lila.common.LightUser
import lila.game.{ Game, GameRepo, Pov, Namer }
import lila.hub.actorApi.map.Ask
import lila.hub.actorApi.round.{ MoveEvent, IsOnGame }
import lila.user.User

import play.api.libs.json._

private final class PushApi(
    aerogear: Aerogear,
    implicit val lightUser: String => Option[LightUser],
    roundSocketHub: ActorSelection) {

  def finish(game: Game): Funit =
    if (!game.isCorrespondence || game.hasAi) funit
    else game.userIds.map { userId =>
      Pov.ofUserId(game, userId) ?? { pov =>
        IfAway(pov) {
          val result = pov.win match {
            case Some(true)  => "You won!"
            case Some(false) => "You lost."
            case _           => "It's a draw."
          }
          aerogear push Aerogear.Push(
            userId = userId,
            alert = s"Your game with ${opponentName(pov)} is over. $result",
            sound = "default",
            categories = List("move"),
            userData = Json.obj(
              "gameId" -> game.id,
              "color" -> pov.color.name,
              "win" -> pov.win)
          )
        }
      }
    }.sequenceFu.void

  def move(move: MoveEvent): Funit = move.opponentUserId ?? { userId =>
    def filter(g: Game) = g.isCorrespondence && g.playable && g.nonAi
    GameRepo game move.gameId flatMap {
      case Some(game) if filter(game) =>
        game.pgnMoves.lastOption ?? { sanMove =>
          Pov.ofUserId(game, userId) ?? { pov =>
            IfAway(pov) {
              aerogear push Aerogear.Push(
                userId = userId,
                alert = s"${opponentName(pov)} played $sanMove, it's your turn!",
                sound = "default",
                categories = List("gameEnd"),
                userData = Json.obj(
                  "gameId" -> game.id,
                  "color" -> pov.color.name)
              )
            }
          }
        }
      case _ => funit
    }
  }

  private def IfAway(pov: Pov)(f: => Funit): Funit = {
    import makeTimeout.short
    roundSocketHub ? Ask(pov.gameId, IsOnGame(pov.color)) mapTo manifest[Boolean] flatMap {
      case true  => funit
      case false => f
    }
  }

  private def opponentName(pov: Pov) = Namer playerString pov.opponent
}
