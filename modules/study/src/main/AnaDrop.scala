package lila.study

import chess.ErrorStr
import chess.format.{ Fen, Uci, UciPath }
import chess.opening.*
import chess.variant.Variant
import play.api.libs.json.JsObject

import lila.common.Json.given
import lila.tree.Branch

case class AnaDrop(
    role: chess.Role,
    pos: chess.Square,
    variant: Variant,
    fen: Fen.Full,
    path: UciPath,
    chapterId: Option[StudyChapterId]
) extends AnaAny:

  def branch: Either[ErrorStr, Branch] =
    chess
      .Game(variant.some, fen.some)
      .drop(role, pos)
      .map: (game, drop) =>
        val uci = Uci(drop)
        val fen = chess.format.Fen.write(game)
        Branch(
          ply = game.ply,
          move = Uci.WithSan(uci, drop.toSanStr),
          fen = fen,
          opening = OpeningDb.findByFullFen(fen),
          crazyData = game.position.crazyData
        )

object AnaDrop:

  def parse(o: JsObject) =
    import chess.variant.Variant
    for
      d <- o.obj("d")
      role <- d.str("role").flatMap(chess.Role.allByName.get)
      pos <- d.str("pos").flatMap(chess.Square.fromKey)
      variant = Variant.orDefault(d.get[Variant.LilaKey]("variant"))
      fen <- d.get[Fen.Full]("fen")
      path <- d.get[UciPath]("path")
    yield AnaDrop(
      role = role,
      pos = pos,
      variant = variant,
      fen = fen,
      path = path,
      chapterId = d.get[StudyChapterId]("ch")
    )
