package lila.socket

import cats.data.Validated
import shogi.format.FEN
import shogi.format.usi.{ Usi, UsiCharPair }
import shogi.opening._
import shogi.variant.Variant
import play.api.libs.json._

import lila.tree.Branch

trait AnaAny {

  def branch: Validated[String, Branch]
  def chapterId: Option[String]
  def path: String
}

case class AnaMove(
    orig: shogi.Pos,
    dest: shogi.Pos,
    variant: Variant,
    fen: String,
    path: String,
    chapterId: Option[String],
    promotion: Boolean
) extends AnaAny {

  def branch: Validated[String, Branch] = {
    shogi.Game(variant.some, fen.some)(orig, dest, promotion) flatMap { case (game, move) =>
      game.usiMoves.lastOption toValid "Moved but no last move!" map { usi =>
        val movable = game.situation playable false
        val fen     = shogi.format.Forsyth >> game
        Branch(
          id = UsiCharPair(usi),
          ply = game.turns,
          usi = usi,
          fen = fen,
          check = game.situation.check,
          dests = Some(movable ?? game.situation.destinations),
          opening = (game.turns <= 30 && Variant.openingSensibleVariants(variant)) ?? {
            FullOpeningDB findByFen FEN(fen)
          },
          drops = if (movable) game.situation.drops else Some(Nil)
        )
      }
    }
  }
  // def json(b: Branch): JsObject = Json.obj(
  //   "node" -> b,
  //   "path" -> path
  // ).add("ch" -> chapterId)
}

object AnaMove {

  def parse(o: JsObject) = {
    for {
      d    <- o obj "d"
      orig <- d str "orig" flatMap shogi.Pos.fromKey
      dest <- d str "dest" flatMap shogi.Pos.fromKey
      fen  <- d str "fen"
      path <- d str "path"
    } yield AnaMove(
      orig = orig,
      dest = dest,
      variant = shogi.variant.Variant orDefault ~d.str("variant"),
      fen = fen,
      path = path,
      chapterId = d str "ch",
      promotion = (d \ "promotion").as[Boolean]
    )
  }
}
