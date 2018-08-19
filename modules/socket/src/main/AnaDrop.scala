package lila.socket

import chess.format.{ Uci, UciCharPair }
import chess.opening._
import chess.variant.Variant
import play.api.libs.json._
import play.api.libs.json.JsObject
import scalaz.Validation.FlatMap._

import lila.tree.Branch

case class AnaDrop(
    role: chess.Role,
    pos: chess.Pos,
    variant: Variant,
    fen: String,
    path: String,
    chapterId: Option[String]
) extends AnaAny {

  def branch: Valid[Branch] =
    chess.Game(variant.some, fen.some).drop(role, pos) flatMap {
      case (game, drop) => game.pgnMoves.lastOption toValid "Dropped but no last move!" map { san =>
        val uci = Uci(drop)
        val movable = !game.situation.end
        val fen = chess.format.Forsyth >> game
        Branch(
          id = UciCharPair(uci),
          ply = game.turns,
          move = Uci.WithSan(uci, san),
          fen = fen,
          check = game.situation.check,
          dests = Some(movable ?? game.situation.destinations),
          opening = Variant.openingSensibleVariants(variant) ?? {
            FullOpeningDB findByFen fen
          },
          drops = if (movable) game.situation.drops else Some(Nil),
          crazyData = game.situation.board.crazyData
        )
      }
    }

  def json(b: Branch): JsObject = Json.obj(
    "node" -> b,
    "path" -> path
  ).add("ch" -> chapterId)
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
    path = path,
    chapterId = d str "ch"
  )
}
