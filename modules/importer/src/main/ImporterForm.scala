package lila.importer

import cats.data.Validated
import chess.format.pgn.{ ParsedPgn, Parser, Reader, Tag, TagType, Tags }
import chess.format.{ FEN, Forsyth }
import chess.{ Color, Mode, Replay, Status }
import play.api.data._
import play.api.data.Forms._
import scala.util.chaining._

import lila.game._

final class ImporterForm {

  lazy val importForm = Form(
    mapping(
      "pgn"     -> nonEmptyText.verifying("invalidPgn", p => checkPgn(p).isValid),
      "analyse" -> optional(nonEmptyText)
    )(ImportData.apply)(ImportData.unapply)
  )

  def checkPgn(pgn: String): Validated[String, Preprocessed] = ImportData(pgn, none).preprocess(none)
}

private case class TagResult(status: Status, winner: Option[Color])
case class Preprocessed(
    game: NewGame,
    replay: Replay,
    initialFen: Option[FEN],
    parsed: ParsedPgn
)

case class ImportData(pgn: String, analyse: Option[String]) {

  private type TagPicker = Tag.type => TagType

  private val maxPlies = 600

  private def evenIncomplete(result: Reader.Result): Replay =
    result match {
      case Reader.Result.Complete(replay)      => replay
      case Reader.Result.Incomplete(replay, _) => replay
    }

  def preprocess(user: Option[String]): Validated[String, Preprocessed] =
    Parser.full(pgn) flatMap { parsed =>
      Reader.fullWithSans(
        pgn,
        sans => sans.copy(value = sans.value take maxPlies),
        Tags.empty
      ) map evenIncomplete map { case replay @ Replay(setup, _, state) =>
        val initBoard    = parsed.tags.fen flatMap Forsyth.<< map (_.board)
        val fromPosition = initBoard.nonEmpty && !parsed.tags.fen.exists(_.initial)
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
          Forsyth.<<<@(variant, _)
        } map Forsyth.>>

        val status = parsed.tags(_.Termination).map(_.toLowerCase) match {
          case Some("normal") | None    => Status.Resign
          case Some("abandoned")        => Status.Aborted
          case Some("time forfeit")     => Status.Outoftime
          case Some("rules infraction") => Status.Cheat
          case Some(_)                  => Status.UnknownFinish
        }

        val date = parsed.tags.anyDate

        def name(whichName: TagPicker, whichRating: TagPicker): String =
          parsed.tags(whichName).fold("?") { n =>
            n + ~parsed.tags(whichRating).map(e => s" (${e take 8})")
          }

        val dbGame = Game
          .make(
            chess = game,
            whitePlayer = Player.make(chess.White, None) withName name(_.White, _.WhiteElo),
            blackPlayer = Player.make(chess.Black, None) withName name(_.Black, _.BlackElo),
            mode = Mode.Casual,
            source = Source.Import,
            pgnImport = PgnImport.make(user = user, date = date, pgn = pgn).some
          )
          .sloppy
          .start pipe { dbGame =>
          // apply the result from the board or the tags
          game.situation.status match {
            case Some(situationStatus) => dbGame.finish(situationStatus, game.situation.winner).game
            case None =>
              parsed.tags.resultColor
                .map {
                  case Some(color)                        => TagResult(status, color.some)
                  case None if status == Status.Outoftime => TagResult(status, none)
                  case None                               => TagResult(Status.Draw, none)
                }
                .filter(_.status > Status.Started)
                .fold(dbGame) { res =>
                  dbGame.finish(res.status, res.winner).game
                }
          }
        }

        Preprocessed(NewGame(dbGame), replay.copy(state = game), initialFen, parsed)
      }
    }
}
