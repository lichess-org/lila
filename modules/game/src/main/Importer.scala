package lila.game
package importer

import chess.format.Fen
import chess.format.pgn.{ ParsedPgn, Parser, PgnStr, Reader, Sans }
import chess.{ ByColor, Color, ErrorStr, Mode, Outcome, Replay, Status }
import scala.util.chaining.*

import lila.game.GameExt.finish
import lila.core.game.{ Game, NewGame, Player, ImportData, ParseImport, ImportReady }

private val maxPlies = 600

object ImporterForm:
  import play.api.data.*
  import play.api.data.Forms.*
  import lila.common.Form.into

  val form = Form:
    mapping(
      "pgn"     -> nonEmptyText.into[PgnStr].verifying("invalidPgn", p => checkPgn(p).isRight),
      "analyse" -> optional(nonEmptyText)
    )(ImportData.apply)(unapply)

  private def checkPgn(pgn: PgnStr): Either[ErrorStr, ImportReady] = catchOverflow: () =>
    parseImport(ImportData(pgn, none), none)

final class Importer(gameRepo: lila.core.game.GameRepo)(using Executor) extends lila.core.game.Importer:

  export lila.game.importer.parseImport

  def importAsGame(data: ImportData, forceId: Option[GameId] = none)(using me: Option[MyId]): Fu[Game] =
    import lila.db.dsl.{ *, given }
    import lila.core.game.BSONFields as F
    import gameRepo.gameHandler
    gameRepo.coll
      .one[Game]($doc(s"${F.pgnImport}.h" -> lila.game.PgnImport.hash(data.pgn)))
      .flatMap:
        case Some(game) => fuccess(game)
        case None =>
          for
            g <- parseImport(data, me).toFuture
            game = forceId.fold(g.game.sloppy)(g.game.withId)
            _ <- gameRepo.insertDenormalized(game, initialFen = g.initialFen)
            _ <- game.pgnImport.flatMap(_.user).isDefined.so {
              // import date, used to make a compound sparse index with the user
              gameRepo.coll.updateField($id(game.id), s"${F.pgnImport}.ca", game.createdAt).void
            }
            _ <- gameRepo.finish(game.id, game.winnerColor, None, game.status)
          yield game

val parseImport: ParseImport = (data, user) =>
  catchOverflow: () =>
    Parser.full(data.pgn).map { parsed =>
      Reader
        .fullWithSans(parsed, _.map(_.take(maxPlies)))
        .pipe:
          case Reader.Result.Complete(replay)      => replay
          case Reader.Result.Incomplete(replay, _) => replay
        .pipe { case replay @ Replay(setup, _, state) =>
          val initBoard    = parsed.tags.fen.flatMap(Fen.read).map(_.board)
          val fromPosition = initBoard.nonEmpty && !parsed.tags.fen.exists(_.isInitial)
          val variant = {
            parsed.tags.variant | {
              if fromPosition then chess.variant.FromPosition
              else chess.variant.Standard
            }
          } match
            case chess.variant.Chess960 if !isChess960StartPosition(setup.situation) =>
              chess.variant.FromPosition
            case chess.variant.FromPosition if parsed.tags.fen.isEmpty => chess.variant.Standard
            case chess.variant.Standard if fromPosition                => chess.variant.FromPosition
            case v                                                     => v
          val game = state.copy(situation = state.situation.withVariant(variant))
          val initialFen = parsed.tags.fen
            .flatMap(Fen.readWithMoveNumber(variant, _))
            .map(Fen.write)

          val status = parsed.tags(_.Termination).map(_.toLowerCase) match
            case Some("normal")                           => game.situation.status | Status.Resign
            case Some("abandoned")                        => Status.Aborted
            case Some("time forfeit")                     => Status.Outoftime
            case Some("rules infraction")                 => Status.Cheat
            case Some(txt) if txt.contains("won on time") => Status.Outoftime
            case _                                        => Status.UnknownFinish

          val dbGame = lila.core.game
            .newGame(
              chess = game,
              players = ByColor: c =>
                lila.game.Player.makeImported(c, parsed.tags.names(c), parsed.tags.elos(c)),
              mode = Mode.Casual,
              source = lila.core.game.Source.Import,
              pgnImport = PgnImport.make(user = user, date = parsed.tags.anyDate, pgn = data.pgn).some
            )
            .sloppy
            .start
            .pipe: dbGame =>
              // apply the result from the board or the tags
              parsed.tags.outcome
                .map:
                  case Outcome(Some(winner))           => TagResult(status, winner.some)
                  case _ if status == Status.Outoftime => TagResult(status, none)
                  case _                               => TagResult(Status.Draw, none)
                .filter(_.status > Status.Started)
                .orElse { game.situation.status.map(TagResult(_, game.situation.winner)) }
                .fold(dbGame): res =>
                  dbGame.finish(res.status, res.winner)

          ImportReady(NewGame(dbGame), replay.copy(state = game), initialFen, parsed)
        }
    }

private case class TagResult(status: Status, winner: Option[Color])

private def isChess960StartPosition(sit: chess.Situation) =
  import chess.*
  val strict =
    def rankMatches(f: Option[Piece] => Boolean)(rank: Rank) =
      File.all.forall: file =>
        f(sit.board(file, rank))
    rankMatches {
      case Some(Piece(White, King | Queen | Rook | Knight | Bishop)) => true
      case _                                                         => false
    }(Rank.First) &&
    rankMatches {
      case Some(Piece(White, Pawn)) => true
      case _                        => false
    }(Rank.Second) &&
    List(Rank.Third, Rank.Fourth, Rank.Fifth, Rank.Sixth).forall(rankMatches(_.isEmpty)) &&
    rankMatches {
      case Some(Piece(Black, Pawn)) => true
      case _                        => false
    }(Rank.Seventh) &&
    rankMatches {
      case Some(Piece(Black, King | Queen | Rook | Knight | Bishop)) => true
      case _                                                         => false
    }(Rank.Eighth)

  variant.Chess960.valid(sit, strict)

private def catchOverflow(f: () => Either[ErrorStr, ImportReady]): Either[ErrorStr, ImportReady] =
  try f()
  catch
    case e: RuntimeException if e.getMessage.contains("StackOverflowError") =>
      ErrorStr("This PGN seems too long or too complex!").asLeft
