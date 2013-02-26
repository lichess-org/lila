package lila
package importer

import game._
import chess.format.pgn.{ Parser, Reader, ParsedPgn, Tag, TagType }
import chess.format.Forsyth
import chess.{ Game, Board, Replay, Color, Mode, Variant, Move, Status }

import play.api.data._
import play.api.data.Forms._
import scalaz.Success
import org.joda.time.DateTime

final class DataForm {

  val importForm = Form(mapping(
    "pgn" -> nonEmptyText.verifying("Invalid PGN", checkPgn _)
  )(ImportData.apply)(ImportData.unapply))

  private def checkPgn(pgn: String): Boolean = ImportData(pgn).preprocess(none).isSuccess
}

case class Result(status: Status, winner: Option[Color])
case class Preprocessed(game: DbGame, moves: List[Move], result: Option[Result])

case class ImportData(pgn: String) {

  private type TagPicker = Tag.type ⇒ TagType

  def preprocess(user: Option[String]): Valid[Preprocessed] = (Parser(pgn) |@| Reader(pgn)) apply {
    case (ParsedPgn(tags, _), replay @ Replay(game, _)) ⇒ {

      def tag(which: Tag.type ⇒ TagType): Option[String] =
        tags find (_.name == which(Tag)) map (_.value)

      val variant = tag(_.Variant).flatMap(v ⇒ Variant(v.value)) | Variant.Standard
      val initBoard = tag(_.FEN) flatMap Forsyth.<< map (_.board)

      val result = tag(_.Result) filterNot (_ ⇒ game.situation.end) collect {
        case "1-0"     ⇒ Result(Status.Resign, Color.White.some)
        case "0-1"     ⇒ Result(Status.Resign, Color.Black.some)
        case "1/2-1/2" ⇒ Result(Status.Draw, none)
      }

      val date = tag(_.Date)

      def name(whichName: TagPicker, whichElo: TagPicker): String =
        tag(whichName).fold(n ⇒ n.value + tag(whichElo).fold(e ⇒ " (%s)" format e, ""), "?")

      val dbGame = DbGame(
        game = Game(board = initBoard | (Board init variant)),
        ai = None,
        whitePlayer = DbPlayer.white withName name(_.White, _.WhiteElo),
        blackPlayer = DbPlayer.black withName name(_.Black, _.BlackElo),
        creatorColor = Color.White,
        mode = Mode.Casual,
        variant = variant,
        source = Source.Import,
        pgnImport = PgnImport(user = user, date = date, pgn = pgn).some
      )

      Preprocessed(dbGame, replay.chronoMoves, result)
    }
  }
}
