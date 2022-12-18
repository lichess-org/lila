package lila.importer

import cats.data.Validated
import chess.format.pgn.{ ParsedPgn, Parser, Reader, Tag, TagType, Tags }
import chess.format.Fen
import chess.{ Color, Mode, Outcome, Replay, Status }
import play.api.data.*
import play.api.data.Forms.*
import scala.util.chaining.*

import lila.game.*

final class ImporterForm:

  lazy val importForm = Form(
    mapping(
      "pgn"     -> nonEmptyText.verifying("invalidPgn", p => checkPgn(p).isValid),
      "analyse" -> optional(nonEmptyText)
    )(ImportData.apply)(unapply)
  )

  def checkPgn(pgn: String): Validated[String, Preprocessed] = ImporterForm.catchOverflow { () =>
    ImportData(pgn, none).preprocess(none)
  }

object ImporterForm:

  def catchOverflow(f: () => Validated[String, Preprocessed]): Validated[String, Preprocessed] = try f()
  catch
    case e: RuntimeException if e.getMessage contains "StackOverflowError" =>
      Validated.Invalid("This PGN seems too long or too complex!")

private case class TagResult(status: Status, winner: Option[Color])
case class Preprocessed(
    game: NewGame,
    replay: Replay,
    initialFen: Option[Fen.Epd],
    parsed: ParsedPgn
)

case class ImportData(pgn: String, analyse: Option[String]):

  private type TagPicker = Tag.type => TagType

  private val maxPlies = 600

  private def evenIncomplete(result: Reader.Result): Replay =
    result match
      case Reader.Result.Complete(replay)      => replay
      case Reader.Result.Incomplete(replay, _) => replay

  def preprocess(user: Option[UserId]): Validated[String, Preprocessed] = ImporterForm.catchOverflow(() =>
    Parser.full(pgn) map { parsed =>
      Reader.fullWithSans(
        parsed,
        sans => sans.copy(value = sans.value take maxPlies)
      ) pipe evenIncomplete pipe { case replay @ Replay(setup, _, state) =>
        val initBoard    = parsed.tags.fen flatMap Fen.read map (_.board)
        val fromPosition = initBoard.nonEmpty && !parsed.tags.fen.exists(_.isInitial)
        val variant = {
          parsed.tags.variant | {
            if (fromPosition) chess.variant.FromPosition
            else chess.variant.Standard
          }
        } match {
          case chess.variant.Chess960 if !Chess960.isStartPosition(setup.board) =>
            chess.variant.FromPosition
          case chess.variant.FromPosition if parsed.tags.fen.isEmpty => chess.variant.Standard
          case chess.variant.Standard if fromPosition                => chess.variant.FromPosition
          case v                                                     => v
        }
        val game = state.copy(situation = state.situation withVariant variant)
        val initialFen = parsed.tags.fen flatMap {
          Fen.readWithMoveNumber(variant, _)
        } map Fen.write

        val status = parsed.tags(_.Termination).map(_.toLowerCase) match {
          case Some("normal")                          => Status.Resign
          case Some("abandoned")                       => Status.Aborted
          case Some("time forfeit")                    => Status.Outoftime
          case Some("rules infraction")                => Status.Cheat
          case Some(txt) if txt contains "won on time" => Status.Outoftime
          case _                                       => Status.UnknownFinish
        }

        val date = parsed.tags.anyDate

        val dbGame = Game
          .make(
            chess = game,
            whitePlayer = Player.makeImported(
              chess.White,
              parsed.tags(_.White),
              IntRating from parsed.tags(_.WhiteElo).flatMap(_.toIntOption)
            ),
            blackPlayer = Player.makeImported(
              chess.Black,
              parsed.tags(_.Black),
              IntRating from parsed.tags(_.BlackElo).flatMap(_.toIntOption)
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
  )
