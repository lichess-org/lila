package lila.socket

import cats.data.Validated
import chess.format.{ Fen, Uci, UciCharPair }
import chess.opening.*
import chess.variant.Variant
import play.api.libs.json.JsObject

import lila.tree.Branch
import lila.common.Json.given

case class AnaDrop(
    role: chess.Role,
    pos: chess.Pos,
    variant: Variant,
    fen: Fen.Epd,
    path: String,
    chapterId: Option[String]
) extends AnaAny:

  def branch: Validated[String, Branch] =
    chess.Game(variant.some, fen.some).drop(role, pos) andThen { (game, drop) =>
      game.sans.lastOption toValid "Dropped but no last move!" map { san =>
        val uci     = Uci(drop)
        val movable = !game.situation.end
        val fen     = chess.format.Fen write game
        Branch(
          id = UciCharPair(uci),
          ply = game.ply,
          move = Uci.WithSan(uci, san),
          fen = fen,
          check = game.situation.check,
          dests = Some(movable ?? game.situation.destinations),
          opening = OpeningDb findByEpdFen fen,
          drops = if (movable) game.situation.drops else Some(Nil),
          crazyData = game.situation.board.crazyData
        )
      }
    }

object AnaDrop:

  def parse(o: JsObject) =
    import chess.variant.Variant
    for
      d    <- o obj "d"
      role <- d str "role" flatMap chess.Role.allByName.get
      pos  <- d str "pos" flatMap { chess.Pos.fromKey(_) }
      variant = Variant.orDefault(d.get[Variant.LilaKey]("variant"))
      fen  <- d.get[Fen.Epd]("fen")
      path <- d str "path"
    yield AnaDrop(
      role = role,
      pos = pos,
      variant = variant,
      fen = fen,
      path = path,
      chapterId = d str "ch"
    )
