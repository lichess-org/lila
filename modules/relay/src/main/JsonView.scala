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
        "games" -> games.flatMap(gameJson))
    }

  private def playerJson(p: lila.game.Relay.Player) = Json.obj(
    "name" -> p.name,
    "title" -> p.title,
    "rating" -> p.rating)

  private def gameJson(g: Game) = g.relay map { r =>
    Json.obj(
      "id" -> g.id,
      "white" -> playerJson(r.white),
      "black" -> playerJson(r.black),
      "winner" -> g.winnerColor.map(_.name),
      "status" -> g.status.id,
      "fen" -> (chess.format.Forsyth exportBoard g.toChess.board),
      "lastMove" -> ~g.castleLastMoveTime.lastMoveString,
      "color" -> g.turnColor.name)
  }
}
