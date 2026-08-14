package lila.study

import chess.ErrorStr
import chess.format.{ Fen, Uci, UciPath }
import chess.variant.Variant
import play.api.libs.json.*

import lila.common.Json.given
import lila.tree.Branch

trait AnaAny:
  def branch(variant: Variant, fen: Fen.Full): Either[ErrorStr, Branch]
  def chapterId: Option[StudyChapterId]
  def path: UciPath

case class AnaMove(
    orig: chess.Square,
    dest: chess.Square,
    path: UciPath,
    chapterId: Option[StudyChapterId],
    promotion: Option[chess.PromotableRole]
) extends AnaAny:

  def branch(variant: Variant, fen: Fen.Full): Either[ErrorStr, Branch] =
    chess
      .Game(variant.some, fen.some)(orig, dest, promotion)
      .map: (game, move) =>
        Branch(
          ply = game.ply,
          move = Uci.WithSan(Uci(move), move.toSanStr),
          fen = chess.format.Fen.write(game),
          crazyData = game.position.crazyData
        )

object AnaMove:

  def parse(o: JsObject) =
    for
      d <- o.obj("d")
      orig <- d.str("orig").flatMap(chess.Square.fromKey)
      dest <- d.str("dest").flatMap(chess.Square.fromKey)
      path <- d.get[UciPath]("path")
    yield AnaMove(
      orig = orig,
      dest = dest,
      path = path,
      chapterId = d.get[StudyChapterId]("ch"),
      promotion = d.str("promotion").flatMap(chess.Role.promotable)
    )
