package lila.api

import play.api.http.ContentTypes.JSON
import play.api.libs.json.{ JsObject, Json => J }
import play.api.mvc.Results.Ok

import chess.format.Forsyth
import lila.game.{ Game, Pov }

object Json {

  def pov(p: Pov) = J.obj(
    "game" -> game(p.game),
    "player" -> player(p))

  private def game(g: Game) = J.obj(
    "id" -> g.id,
    "fen" -> (Forsyth >> g.toChess))

  private def player(p: Pov) = J.obj(
    "id" -> p.fullId,
    "color" -> p.color.name)
}
