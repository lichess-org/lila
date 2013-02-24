package lila
package importer

import game._
import chess.format.pgn.{ Parser, Reader, ParsedPgn, Tag, TagType }
import chess.format.Forsyth
import chess.{ Game, Board, Replay, Color, Mode, Variant, Move }

import play.api.data._
import play.api.data.Forms._
import scalaz.Success

final class DataForm {

  val importForm = Form(mapping(
    "pgn" -> nonEmptyText.verifying("Invalid PGN", checkPgn _)
  )(ImportData.apply)(ImportData.unapply))

  private def checkPgn(pgn: String): Boolean = ImportData(pgn).game.isSuccess
}

case class ImportData(pgn: String) {

  def game: Valid[(DbGame, List[Move])] = (Parser(pgn) |@| Reader(pgn)) apply {
    case (ParsedPgn(tags, _), replay @ Replay(game, _)) ⇒ {

      def tag(which: Tag.type ⇒ TagType): Option[String] =
        tags find (_.name == which(Tag)) map (_.value)

      val variant = tag(_.Variant).flatMap(v ⇒ Variant(v.value)) | Variant.Standard
      val board = tag(_.FEN) flatMap Forsyth.<< map (_.board)

      DbGame(
        game = Game(board = board | (Board init variant)),
        ai = None,
        whitePlayer = DbPlayer.white,
        blackPlayer = DbPlayer.black,
        creatorColor = Color.White,
        mode = Mode.Casual,
        variant = variant,
        source = Source.Import) -> replay.chronoMoves
    }
  }
}
