package lila.socket

import cats.data.Validated
import chess.format.{ Fen, Uci, UciCharPair }
import chess.opening.*
import chess.variant.Variant
import play.api.libs.json.*

import lila.tree.Branch
import lila.common.Json.given

trait AnaAny:

  def branch: Validated[String, Branch]
  def chapterId: Option[String]
  def path: String

case class AnaMove(
    orig: chess.Pos,
    dest: chess.Pos,
    variant: Variant,
    fen: Fen.Epd,
    path: String,
    chapterId: Option[String],
    promotion: Option[chess.PromotableRole]
) extends AnaAny:

  def branch: Validated[String, Branch] =
    chess.Game(variant.some, fen.some)(orig, dest, promotion) andThen { (game, move) =>
      game.sans.lastOption toValid "Moved but no last move!" map { san =>
        val uci     = Uci(move)
        val movable = game.situation playable false
        val fen     = chess.format.Fen write game
        Branch(
          id = UciCharPair(uci),
          ply = game.ply,
          move = Uci.WithSan(uci, san),
          fen = fen,
          check = game.situation.check,
          dests = Some(movable ?? game.situation.destinations),
          opening = (game.ply <= 30 && Variant.list.openingSensibleVariants(variant)) ??
            OpeningDb.findByEpdFen(fen),
          drops = if (movable) game.situation.drops else Some(Nil),
          crazyData = game.situation.board.crazyData
        )
      }
    }

object AnaMove:

  def parse(o: JsObject) =
    import chess.variant.Variant
    for
      d    <- o obj "d"
      orig <- d str "orig" flatMap { chess.Pos.fromKey(_) }
      dest <- d str "dest" flatMap { chess.Pos.fromKey(_) }
      fen  <- d.get[Fen.Epd]("fen")
      path <- d str "path"
      variant = Variant.orDefault(d.get[Variant.LilaKey]("variant"))
    yield AnaMove(
      orig = orig,
      dest = dest,
      variant = variant,
      fen = fen,
      path = path,
      chapterId = d str "ch",
      promotion = d str "promotion" flatMap chess.Role.promotable
    )
