package lila.socket

import chess.format.{ Fen, Uci, UciCharPair, UciPath }
import chess.opening.*
import chess.variant.Variant
import chess.ErrorStr
import play.api.libs.json.JsObject

import lila.tree.Branch
import lila.common.Json.given

case class AnaDrop(
    role: chess.Role,
    pos: chess.Square,
    variant: Variant,
    fen: Fen.Epd,
    path: UciPath,
    chapterId: Option[StudyChapterId]
) extends AnaAny:

  def branch: Either[ErrorStr, Branch] =
    chess
      .Game(variant.some, fen.some)
      .drop(role, pos)
      .map: (game, drop) =>
        val uci     = Uci(drop)
        val movable = !game.situation.end
        val fen     = chess.format.Fen write game
        Branch(
          id = UciCharPair(uci),
          ply = game.ply,
          move = Uci.WithSan(uci, drop.san),
          fen = fen,
          check = game.situation.check,
          dests = Some(movable so game.situation.destinations),
          opening = OpeningDb findByEpdFen fen,
          drops = if movable then game.situation.drops else Some(Nil),
          crazyData = game.situation.board.crazyData
        )

object AnaDrop:

  def parse(o: JsObject) =
    import chess.variant.Variant
    for
      d    <- o obj "d"
      role <- d str "role" flatMap chess.Role.allByName.get
      pos  <- d str "pos" flatMap { chess.Square.fromKey(_) }
      variant = Variant.orDefault(d.get[Variant.LilaKey]("variant"))
      fen  <- d.get[Fen.Epd]("fen")
      path <- d.get[UciPath]("path")
    yield AnaDrop(
      role = role,
      pos = pos,
      variant = variant,
      fen = fen,
      path = path,
      chapterId = d.get[StudyChapterId]("ch")
    )
