package lila.relay

import play.api.libs.json._

import lila.game.{ Game, GameRepo }

final class JsonView {

  def apply(relay: Relay): Fu[JsObject] =
    GameRepo.games(relay.gameIds) map { games =>
      Json.obj(
        "id" -> relay.id,
        "name" -> relay.name,
        "status" -> relay.status.id,
        "games" -> games.map(gameJson)
      )
    }

  private def gameJson(g: Game) = Json.obj(
    "id" -> g.id,
    "status" -> g.status.id,
    "fen" -> (chess.format.Forsyth exportBoard g.toChess.board),
    "lastMove" -> ~g.castleLastMoveTime.lastMoveString,
    "orient" -> g.firstPlayer.color.name)
}
