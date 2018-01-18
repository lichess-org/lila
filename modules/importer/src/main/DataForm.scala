package lila.importer

import chess.format.pgn.{ Parser, Reader, ParsedPgn, Tag, TagType, Tags }
import chess.format.{ FEN, Forsyth }
import chess.{ Replay, Color, Mode, Status }
import play.api.data._
import play.api.data.Forms._
import scalaz.Validation.FlatMap._

import lila.game._

private[importer] final class DataForm {

  lazy val importForm = Form(mapping(
    "pgn" -> nonEmptyText.verifying("invalidPgn", checkPgn _),
    "analyse" -> optional(nonEmptyText)
  )(ImportData.apply)(ImportData.unapply))

  private def checkPgn(pgn: String): Boolean =
    ImportData(pgn, none).preprocess(none).isSuccess
}

private[importer] case class Result(status: Status, winner: Option[Color])
case class Preprocessed(
    game: Game,
    replay: Replay,
    result: Result,
    initialFen: Option[FEN],
    parsed: ParsedPgn
)

case class ImportData(pgn: String, analyse: Option[String]) {

  private type TagPicker = Tag.type => TagType

  private val maxPlies = 600

  private def evenIncomplete(result: Reader.Result): Replay = result match {
    case Reader.Result.Complete(replay) => replay
    case Reader.Result.Incomplete(replay, _) => replay
  }

  def preprocess(user: Option[String]): Valid[Preprocessed] = Parser.full(pgn) flatMap {
    case parsed @ ParsedPgn(_, tags, sans) => Reader.fullWithSans(
      pgn,
      sans => sans.copy(value = sans.value take maxPlies),
      Tags.empty
    ) map evenIncomplete map {
        case replay @ Replay(setup, _, game) =>
          val initBoard = parsed.tags.fen.map(_.value) flatMap Forsyth.<< map (_.board)
          val fromPosition = initBoard.nonEmpty && !parsed.tags.fen.contains(FEN(Forsyth.initial))
          val variant = {
            parsed.tags.variant | {
              if (fromPosition) chess.variant.FromPosition
              else chess.variant.Standard
            }
          } match {
            case chess.variant.Chess960 if !Chess960.isStartPosition(setup.board) => chess.variant.FromPosition
            case chess.variant.FromPosition if parsed.tags.fen.isEmpty => chess.variant.Standard
            case v => v
          }
          val initialFen = parsed.tags.fen.map(_.value) flatMap {
            Forsyth.<<<@(variant, _)
          } map Forsyth.>> map FEN.apply

          val status = parsed.tags(_.Termination).map(_.toLowerCase) match {
            case Some("normal") | None => Status.Resign
            case Some("abandoned") => Status.Aborted
            case Some("time forfeit") => Status.Outoftime
            case Some("rules infraction") => Status.Cheat
            case Some(_) => Status.UnknownFinish
          }

          val result =
            parsed.tags.resultColor
              .ifFalse(game.situation.end)
              .fold(Result(Status.Started, none)) {
                case Some(color) => Result(status, color.some)
                case None if status == Status.Outoftime => Result(status, none)
                case None => Result(Status.Draw, none)
              }

          val date = parsed.tags.anyDate

          def name(whichName: TagPicker, whichRating: TagPicker): String = parsed.tags(whichName).fold("?") { n =>
            n + ~parsed.tags(whichRating).map(e => s" (${e take 8})")
          }

          val dbGame = Game.make(
            game = replay.state,
            whitePlayer = Player.white withName name(_.White, _.WhiteElo),
            blackPlayer = Player.black withName name(_.Black, _.BlackElo),
            mode = Mode.Casual,
            variant = variant,
            source = Source.Import,
            pgnImport = PgnImport.make(user = user, date = date, pgn = pgn).some
          ) |> { g =>
            g.copy(
              binaryPgn = g.binaryPgn update replay.state.pgnMoves
            ).start
          }

          Preprocessed(dbGame, replay, result, initialFen, parsed)
      }
  }
}
