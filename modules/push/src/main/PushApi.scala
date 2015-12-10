package lila.push

import lila.common.LightUser
import lila.game.{ Game, GameRepo, Pov, Namer }
import lila.hub.actorApi.round.MoveEvent
import lila.user.User

import play.api.libs.json._

// https://aerogear.org/docs/specs/aerogear-unifiedpush-rest/index.html#397083935
private final class PushApi(
    aerogear: Aerogear,
    implicit val lightUser: String => Option[LightUser]) {

  def finish(game: Game): Funit =
    if (!game.isCorrespondence || game.hasAi) funit
    else game.userIds.map { userId =>
      Pov.ofUserId(game, userId) ?? { pov =>
        val result = pov.win match {
          case Some(true)  => "You won!"
          case Some(false) => "You lost."
          case _           => "It's a draw."
        }
        aerogear push Aerogear.Push(
          userId = userId,
          alert = s"Your game with ${opponentName(pov)} is over. $result",
          sound = "default",
          userData = Json.obj(
            "gameId" -> game.id,
            "color" -> pov.color.name,
            "win" -> pov.win)
        )
      }
    }.sequenceFu.void

  def move(move: MoveEvent): Funit = move.opponentUserId ?? { userId =>
    def filter(g: Game) = g.isCorrespondence && g.playable && g.nonAi
    GameRepo game move.gameId flatMap {
      case Some(game) if filter(game) =>
        game.pgnMoves.lastOption ?? { sanMove =>
          Pov.ofUserId(game, userId) ?? { pov =>
            aerogear push Aerogear.Push(
              userId = userId,
              alert = s"${opponentName(pov)} played $sanMove, it's your turn!",
              sound = "default",
              userData = Json.obj(
                "gameId" -> game.id,
                "color" -> pov.color.name)
            )
          }
        }
      case _ => funit
    }
  }

  private def opponentName(pov: Pov) = Namer playerString pov.opponent
}
