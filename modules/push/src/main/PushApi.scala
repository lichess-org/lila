package lila.push

import lila.game.{ Game, GameRepo, Pov }
import lila.hub.actorApi.round.MoveEvent
import lila.user.User

import play.api.libs.json._

// https://aerogear.org/docs/specs/aerogear-unifiedpush-rest/index.html#397083935
private final class PushApi(aerogear: Aerogear) {

  def finish(game: Game): Funit = ???

  def move(move: MoveEvent): Funit = move.opponentUserId ?? { userId =>
    GameRepo game move.gameId flatMap {
      case Some(game) if filter(game) =>
        Pov.ofUserId(game, userId) ?? { pov =>
          aerogear push Aerogear.Push(
            userId = userId,
            alert = "It's your turn!",
            sound = "default",
            userData = Json.obj(
              "gameId" -> game.id,
              "color" -> pov.color.name,
              "fen" -> move.fen,
              "move" -> move.move)
          )
        }
      case _ => funit
    }
  }

  private def filter(game: Game) =
    game.isCorrespondence && game.playable && game.nonAi
}
