package lila.socket

import chess.format.Uci
import chess.opening._
import chess.variant.Variant
import lila.common.PimpedJson._
import play.api.libs.json.JsObject

case class AnaMove(
    orig: chess.Pos,
    dest: chess.Pos,
    variant: Variant,
    fen: String,
    path: String,
    promotion: Option[chess.PromotableRole]) {

  def step: Valid[Step] =
    chess.Game(variant.some, fen.some)(orig, dest, promotion) map {
      case (game, move) =>
        val movable = !game.situation.end
        val fen = chess.format.Forsyth >> game
        Step(
          ply = game.turns,
          move = game.pgnMoves.lastOption.map { san =>
            Step.Move(Uci(move), san)
          },
          fen = fen,
          check = game.situation.check,
          dests = Some(movable ?? game.situation.destinations),
          opening = Variant.openingSensibleVariants(variant) ?? {
            FullOpeningDB findByFen fen
          },
          drops = movable.fold(game.situation.drops, Some(Nil)),
          crazyData = game.situation.board.crazyData)
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
