package lila.socket

import chess.format.Uci
import chess.opening._
import chess.variant.Variant
import lila.common.PimpedJson._
import play.api.libs.json.JsObject

case class AnaDrop(
    role: chess.Role,
    pos: chess.Pos,
    variant: Variant,
    fen: String,
    path: String) {

  def step: Valid[Step] =
    chess.Game(variant.some, fen.some).drop(role, pos) map {
      case (game, drop) =>
        val movable = !game.situation.end
        val fen = chess.format.Forsyth >> game
        Step(
          ply = game.turns,
          move = game.pgnMoves.lastOption.map { san =>
            Step.Move(Uci(drop), san)
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

object AnaDrop {

  def parse(o: JsObject) = for {
    d ← o obj "d"
    role ← d str "role" flatMap chess.Role.allByName.get
    pos ← d str "pos" flatMap chess.Pos.posAt
    variant = chess.variant.Variant orDefault ~d.str("variant")
    fen ← d str "fen"
    path ← d str "path"
  } yield AnaDrop(
    role = role,
    pos = pos,
    variant = variant,
    fen = fen,
    path = path)
}
