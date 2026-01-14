package lila.study

import chess.ErrorStr
import chess.format.{ Fen, Uci, UciPath }
import chess.variant.Variant
import play.api.libs.json.JsObject

import lila.common.Json.given
import lila.tree.Branch

case class AnaDrop(
    role: chess.Role,
    pos: chess.Square,
    fen: Fen.Full,
    path: UciPath,
    chapterId: Option[StudyChapterId]
) extends AnaAny:

  def branch(variant: Variant): Either[ErrorStr, Branch] =
    chess
      .Game(variant.some, fen.some)
      .drop(role, pos)
      .map: (game, drop) =>
        Branch(
          ply = game.ply,
          move = Uci.WithSan(Uci(drop), drop.toSanStr),
          fen = chess.format.Fen.write(game),
          crazyData = game.position.crazyData
        )

object AnaDrop:

  def parse(o: JsObject) =
    for
      d <- o.obj("d")
      role <- d.str("role").flatMap(chess.Role.allByName.get)
      pos <- d.str("pos").flatMap(chess.Square.fromKey)
      fen <- d.get[Fen.Full]("fen")
      path <- d.get[UciPath]("path")
    yield AnaDrop(
      role = role,
      pos = pos,
      fen = fen,
      path = path,
      chapterId = d.get[StudyChapterId]("ch")
    )
