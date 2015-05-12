package lila.socket

import play.api.libs.json.JsObject
import lila.common.PimpedJson._

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
          Step.Move(move.orig, move.dest, san)
        },
        fen = chess.format.Forsyth >> game,
        check = game.situation.check,
        dests = !game.situation.end ?? game.situation.destinations)
    }
}

object AnaMove {

  def parse(o: JsObject) = for {
    d ← o obj "d"
    orig ← d str "orig" flatMap chess.Pos.posAt
    dest ← d str "dest" flatMap chess.Pos.posAt
    variant ← d str "variant" map chess.variant.Variant.orDefault
    fen ← d str "fen"
    path ← d str "path"
    prom = d str "promotion" chess.Role.promotable
  } yield AnaMove(
    orig = orig,
    dest = dest,
    variant = variant,
    fen = fen,
    path = path,
    promotion = prom)
}
