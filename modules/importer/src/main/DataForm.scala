package lila.importer

import chess.format.Forsyth
import chess.format.pgn.{ Parser, Reader, ParsedPgn, Tag, TagType }
import chess.{ Game => ChessGame, Board, Replay, Color, Mode, Move, Status }
import play.api.data._
import play.api.data.Forms._

import lila.game._

private[importer] final class DataForm {

  lazy val importForm = Form(mapping(
    "pgn" -> nonEmptyText.verifying("Invalid PGN", checkPgn _)
  )(ImportData.apply)(ImportData.unapply))

  private def checkPgn(pgn: String): Boolean =
    ImportData(pgn).preprocess(none).isSuccess
}

private[importer] case class Result(status: Status, winner: Option[Color])
private[importer] case class Preprocessed(game: Game, moves: List[Move], result: Option[Result])

private[importer] case class ImportData(pgn: String) {

  private type TagPicker = Tag.type => TagType

  private val maxPlies = 600

  def preprocess(user: Option[String]): Valid[Preprocessed] = Parser.full(pgn) flatMap {
    case ParsedPgn(_, sans) if sans.size > maxPlies => !!("Replay is too long")
    case ParsedPgn(tags, sans) => Reader.full(pgn) map {
      case replay@Replay(setup, _, game) =>
        def tag(which: Tag.type => TagType): Option[String] =
          tags find (_.name == which(Tag)) map (_.value)

        val initBoard = tag(_.FEN) flatMap Forsyth.<< map (_.board)
        val fromPosition = initBoard.nonEmpty && tag(_.FEN) != Forsyth.initial.some
        val variant = {
          tag(_.Variant).map(Chess960.fixVariantName).flatMap(chess.variant.Variant.byName) | {
            fromPosition.fold(chess.variant.FromPosition, chess.variant.Standard)
          }
        } match {
          case chess.variant.Chess960 if !Chess960.isStartPosition(setup.board) => chess.variant.FromPosition
          case v => v
        }

        val result = tag(_.Result) filterNot (_ => game.situation.end) collect {
          case "1-0"     => Result(Status.Resign, Color.White.some)
          case "0-1"     => Result(Status.Resign, Color.Black.some)
          case "1/2-1/2" => Result(Status.Draw, none)
        }

        val date = tag(_.Date)

        def name(whichName: TagPicker, whichRating: TagPicker): String = tag(whichName).fold("?") { n =>
          n + ~tag(whichRating).map(e => " (%s)" format e)
        }

        val dbGame = Game.make(
          game = ChessGame(board = initBoard | (Board init variant)),
          whitePlayer = Player.white withName name(_.White, _.WhiteElo),
          blackPlayer = Player.black withName name(_.Black, _.BlackElo),
          mode = Mode.Casual,
          variant = variant,
          source = Source.Import,
          pgnImport = PgnImport.make(user = user, date = date, pgn = pgn).some).start

        Preprocessed(dbGame, replay.chronoMoves, result)
    }
  }
}
