package lila.importer

import chess.format.pgn.{ Parser, Reader, ParsedPgn, Tag, TagType }
import chess.format.{ FEN, Forsyth }
import chess.{ Game => ChessGame, Board, Replay, Color, Mode, MoveOrDrop, Status }
import play.api.data._
import play.api.data.Forms._
import scalaz.Validation.FlatMap._

import lila.game._

private[importer] final class DataForm {

  lazy val importForm = Form(mapping(
    "pgn" -> nonEmptyText.verifying("Invalid PGN", checkPgn _),
    "analyse" -> optional(nonEmptyText)
  )(ImportData.apply)(ImportData.unapply))

  private def checkPgn(pgn: String): Boolean =
    ImportData(pgn, none).preprocess(none).isSuccess
}

private[importer] case class Result(status: Status, winner: Option[Color])
case class Preprocessed(
  game: Game,
  replay: Replay,
  result: Option[Result],
  initialFen: Option[FEN],
  parsed: ParsedPgn)

case class ImportData(pgn: String, analyse: Option[String]) {

  private type TagPicker = Tag.type => TagType

  private val maxPlies = 600

  def preprocess(user: Option[String]): Valid[Preprocessed] = Parser.full(pgn) flatMap {
    case ParsedPgn(_, sans) if sans.size > maxPlies => !!("Replay is too long")
    case parsed@ParsedPgn(tags, sans) => Reader.full(pgn) map {
      case replay@Replay(setup, _, game) =>
        def tag(which: Tag.type => TagType): Option[String] =
          tags find (_.name == which(Tag)) map (_.value)

        val initBoard = tag(_.FEN) flatMap Forsyth.<< map (_.board)
        val fromPosition = initBoard.nonEmpty && tag(_.FEN) != Forsyth.initial.some
        val variant = {
          tag(_.Variant).map(Chess960.fixVariantName).flatMap(chess.variant.Variant.byName) | {
            if (fromPosition) chess.variant.FromPosition
            else chess.variant.Standard
          }
        } match {
          case chess.variant.Chess960 if !Chess960.isStartPosition(setup.board) => chess.variant.FromPosition
          case v => v
        }
        val initialFen = tag(_.FEN) flatMap {
          Forsyth.<<<@(variant, _)
        } map Forsyth.>> map FEN.apply

        val result = tag(_.Result) ifFalse game.situation.end collect {
          case "1-0"     => Result(Status.UnknownFinish, Color.White.some)
          case "0-1"     => Result(Status.UnknownFinish, Color.Black.some)
          case "1/2-1/2" => Result(Status.Draw, none)
        }

        val date = tag(_.Date)

        def name(whichName: TagPicker, whichRating: TagPicker): String = tag(whichName).fold("?") { n =>
          n + ~tag(whichRating).map(e => s" (${e take 8})")
        }

        val dbGame = Game.make(
          game = replay.state,
          whitePlayer = Player.white withName name(_.White, _.WhiteElo),
          blackPlayer = Player.black withName name(_.Black, _.BlackElo),
          mode = Mode.Casual,
          variant = variant,
          source = Source.Import,
          pgnImport = PgnImport.make(user = user, date = date, pgn = pgn).some
        ).copy(
            binaryPgn = BinaryFormat.pgn write replay.state.pgnMoves
          ).start

        Preprocessed(dbGame, replay, result, initialFen, parsed)
    }
  }
}
