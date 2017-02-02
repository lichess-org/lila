package lila.socket

import chess.format.{ Uci, UciCharPair }
import chess.opening._
import chess.variant.Variant
import play.api.libs.json._
import scalaz.Validation.FlatMap._

import lila.common.PimpedJson._
import lila.tree.Branch

case class AnaMove(
    orig: chess.Pos,
    dest: chess.Pos,
    variant: Variant,
    fen: String,
    path: String,
    promotion: Option[chess.PromotableRole]) {

  def branch: Valid[Branch] =
    chess.Game(variant.some, fen.some)(orig, dest, promotion) flatMap {
      case (game, move) => game.pgnMoves.lastOption toValid "Moved but no last move!" map { san =>
        val uci = Uci(move)
        val movable = game.situation playable false
        val fen = chess.format.Forsyth >> game
        Branch(
          id = UciCharPair(uci),
          ply = game.turns,
          move = Uci.WithSan(uci, san),
          fen = fen,
          check = game.situation.check,
          dests = Some(movable ?? game.situation.destinations),
          opening = (game.turns <= 30 && Variant.openingSensibleVariants(variant)) ?? {
            FullOpeningDB findByFen fen
          },
          drops = movable.fold(game.situation.drops, Some(Nil)),
          crazyData = game.situation.board.crazyData)
      }
    }

  def json(b: Branch): JsObject = Json.obj("node" -> b, "path" -> path)
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
