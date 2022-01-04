package lila.socket

import cats.data.Validated
import shogi.format.FEN
import shogi.format.usi.{ Usi, UsiCharPair }
import shogi.opening._
import shogi.variant.Variant
import play.api.libs.json.JsObject

import lila.tree.Branch

case class AnaDrop(
    role: shogi.Role,
    pos: shogi.Pos,
    variant: Variant,
    fen: String,
    path: String,
    chapterId: Option[String]
) extends AnaAny {

  def branch: Validated[String, Branch] =
    shogi.Game(variant.some, fen.some).drop(role, pos) flatMap { case (game, drop) =>
      game.usiMoves.lastOption toValid "Dropped but no last move!" map { usi =>
        val movable = !game.situation.end
        val fen     = shogi.format.Forsyth >> game
        Branch(
          id = UsiCharPair(usi),
          ply = game.turns,
          usi = usi,
          fen = fen,
          check = game.situation.check,
          dests = Some(movable ?? game.situation.destinations),
          opening = Variant.openingSensibleVariants(variant) ?? {
            FullOpeningDB findByFen FEN(fen)
          },
          drops = if (movable) game.situation.drops else Some(Nil)
        )
      }
    }
}

object AnaDrop {

  def parse(o: JsObject) =
    for {
      d    <- o obj "d"
      role <- d str "role" flatMap shogi.Role.allByName.get
      pos  <- d str "pos" flatMap shogi.Pos.fromKey
      variant = shogi.variant.Variant orDefault ~d.str("variant")
      fen  <- d str "fen"
      path <- d str "path"
    } yield AnaDrop(
      role = role,
      pos = pos,
      variant = variant,
      fen = fen,
      path = path,
      chapterId = d str "ch"
    )
}
