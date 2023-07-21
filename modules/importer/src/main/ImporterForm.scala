package lila.importer

import chess.format.pgn.{ ParsedPgn, PgnStr, Parser, Reader }
import chess.format.Fen
import chess.{ Color, ByColor, Mode, Outcome, Replay, Status, ErrorStr }
import chess.format.pgn.Sans
import play.api.data.*
import play.api.data.Forms.*
import scala.util.chaining.*

import lila.game.*
import lila.common.Form.into

final class ImporterForm:

  lazy val importForm = Form:
    mapping(
      "pgn"     -> nonEmptyText.into[PgnStr].verifying("invalidPgn", p => checkPgn(p).isRight),
      "analyse" -> optional(nonEmptyText)
    )(ImportData.apply)(unapply)

  def checkPgn(pgn: PgnStr): Either[ErrorStr, Preprocessed] = ImporterForm.catchOverflow: () =>
    ImportData(pgn, none).preprocess(none)

object ImporterForm:

  def catchOverflow(f: () => Either[ErrorStr, Preprocessed]): Either[ErrorStr, Preprocessed] = try f()
  catch
    case e: RuntimeException if e.getMessage contains "StackOverflowError" =>
      ErrorStr("This PGN seems too long or too complex!").asLeft

private case class TagResult(status: Status, winner: Option[Color])
case class Preprocessed(
    game: NewGame,
    replay: Replay,
    initialFen: Option[Fen.Epd],
    parsed: ParsedPgn
)

case class ImportData(pgn: PgnStr, analyse: Option[String]):

  private val maxPlies = 600

  private def evenIncomplete(result: Reader.Result): Replay =
    result match
      case Reader.Result.Complete(replay)      => replay
      case Reader.Result.Incomplete(replay, _) => replay

  def preprocess(user: Option[UserId]): Either[ErrorStr, Preprocessed] =
    ImporterForm.catchOverflow: () =>
      Parser.full(pgn).map { parsed =>
        Reader.fullWithSans(
          parsed,
          _.map(_ take maxPlies)
        ) pipe evenIncomplete pipe { case replay @ Replay(setup, _, state) =>
          val initBoard    = parsed.tags.fen flatMap Fen.read map (_.board)
          val fromPosition = initBoard.nonEmpty && !parsed.tags.fen.exists(_.isInitial)
          val variant = {
            parsed.tags.variant | {
              if fromPosition then chess.variant.FromPosition
              else chess.variant.Standard
            }
          } match
            case chess.variant.Chess960 if !Chess960.isStartPosition(setup.situation) =>
              chess.variant.FromPosition
            case chess.variant.FromPosition if parsed.tags.fen.isEmpty => chess.variant.Standard
            case chess.variant.Standard if fromPosition                => chess.variant.FromPosition
            case v                                                     => v
          val game = state.copy(situation = state.situation withVariant variant)
          val initialFen = parsed.tags.fen flatMap {
            Fen.readWithMoveNumber(variant, _)
          } map Fen.write

          val status = parsed.tags(_.Termination).map(_.toLowerCase) match
            case Some("normal")                          => game.situation.status | Status.Resign
            case Some("abandoned")                       => Status.Aborted
            case Some("time forfeit")                    => Status.Outoftime
            case Some("rules infraction")                => Status.Cheat
            case Some(txt) if txt contains "won on time" => Status.Outoftime
            case _                                       => Status.UnknownFinish

          val date = parsed.tags.anyDate

          val dbGame = Game
            .make(
              chess = game,
              players = ByColor: c =>
                Player.makeImported(
                  c,
                  parsed.tags.players(c),
                  IntRating from parsed.tags.elos(c)
                ),
              mode = Mode.Casual,
              source = Source.Import,
              pgnImport = PgnImport.make(user = user, date = date, pgn = pgn).some
            )
            .sloppy
            .start pipe { dbGame =>
            // apply the result from the board or the tags

            val tagStatus: Option[TagResult] = parsed.tags.outcome
              .map {
                case Outcome(Some(winner))           => TagResult(status, winner.some)
                case _ if status == Status.Outoftime => TagResult(status, none)
                case _                               => TagResult(Status.Draw, none)
              }
              .filter(_.status > Status.Started)

            tagStatus
              .orElse { game.situation.status.map(TagResult(_, game.situation.winner)) }
              .fold(dbGame) { res =>
                dbGame.finish(res.status, res.winner)
              }

          }

          Preprocessed(NewGame(dbGame), replay.copy(state = game), initialFen, parsed)
        }
      }
