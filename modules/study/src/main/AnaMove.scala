package lila.study

import chess.ErrorStr
import chess.format.{ Fen, Uci, UciCharPair, UciPath }
import chess.opening.*
import chess.variant.Variant
import play.api.libs.json.*

import lila.common.Json.given
import lila.tree.Branch

trait AnaAny:
  def branch: Either[ErrorStr, Branch]
  def chapterId: Option[StudyChapterId]
  def path: UciPath

// TODO StudyChapterId, UciPath
case class AnaMove(
    orig: chess.Square,
    dest: chess.Square,
    variant: Variant,
    fen: Fen.Full,
    path: UciPath,
    chapterId: Option[StudyChapterId],
    promotion: Option[chess.PromotableRole]
) extends AnaAny:

  def branch: Either[ErrorStr, Branch] =
    chess
      .Game(variant.some, fen.some)(orig, dest, promotion)
      .map: (game, move) =>
        val uci     = Uci(move)
        val movable = game.situation.playable(false)
        val fen     = chess.format.Fen.write(game)
        Branch(
          id = UciCharPair(uci),
          ply = game.ply,
          move = Uci.WithSan(uci, move.san),
          fen = fen,
          check = game.situation.check,
          dests = Some(movable.so(game.situation.destinations)),
          opening = (game.ply <= 30 && Variant.list.openingSensibleVariants(variant))
            .so(OpeningDb.findByFullFen(fen)),
          drops = if movable then game.situation.drops else Some(Nil),
          crazyData = game.situation.board.crazyData
        )

object AnaMove:

  def parse(o: JsObject) =
    import chess.variant.Variant
    for
      d    <- o.obj("d")
      orig <- d.str("orig").flatMap { chess.Square.fromKey(_) }
      dest <- d.str("dest").flatMap { chess.Square.fromKey(_) }
      fen  <- d.get[Fen.Full]("fen")
      path <- d.get[UciPath]("path")
      variant = Variant.orDefault(d.get[Variant.LilaKey]("variant"))
    yield AnaMove(
      orig = orig,
      dest = dest,
      variant = variant,
      fen = fen,
      path = path,
      chapterId = d.get[StudyChapterId]("ch"),
      promotion = d.str("promotion").flatMap(chess.Role.promotable)
    )
