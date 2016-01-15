package lila.socket

import chess.format.Uci
import lila.common.PimpedJson._
import play.api.libs.json.JsObject

case class AnaMove(
    orig: chess.Pos,
    dest: chess.Pos,
    variant: chess.variant.Variant,
    fen: String,
    path: String,
    promotion: Option[chess.PromotableRole]) {

  def step: Valid[Step] =
    chess.Game(variant.some, fen.some)(orig, dest, promotion) map {
      case (game, move) => Step(
        ply = game.turns,
        move = game.pgnMoves.lastOption.map { san =>
          Step.Move(Uci(move), san)
        },
        fen = chess.format.Forsyth >> game,
        check = game.situation.check,
        dests = Some(!game.situation.end ?? game.situation.destinations))
    }
}

object AnaMove {

  def parse(o: JsObject) = for {
    d ← o obj "d"
    orig ← d str "orig" flatMap chess.Pos.posAt
    dest ← d str "dest" flatMap chess.Pos.posAt
    variant = chess.variant.Variant orDefault ~d.str("variant")
    fen ← d str "fen"
    path ← d str "path"
    prom = d str "promotion" flatMap chess.Role.promotable
  } yield AnaMove(
    orig = orig,
    dest = dest,
    variant = variant,
    fen = fen,
    path = path,
    promotion = prom)
}
