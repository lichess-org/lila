package lila.study

import chess.ErrorStr
import chess.format.{ Fen, Uci, UciPath }
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
        val uci = Uci(move)
        val fen = chess.format.Fen.write(game)
        Branch(
          ply = game.ply,
          move = Uci.WithSan(uci, move.toSanStr),
          fen = fen,
          opening = (game.ply <= 30 && Variant.list.openingSensibleVariants(variant))
            .so(OpeningDb.findByFullFen(fen)),
          crazyData = game.position.crazyData
        )

object AnaMove:

  def parse(o: JsObject) =
    import chess.variant.Variant
    for
      d <- o.obj("d")
      orig <- d.str("orig").flatMap(chess.Square.fromKey)
      dest <- d.str("dest").flatMap(chess.Square.fromKey)
      fen <- d.get[Fen.Full]("fen")
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
