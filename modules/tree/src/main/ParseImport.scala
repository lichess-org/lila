package lila.tree

import chess.format.Fen
import chess.format.pgn.{ ParsedPgn, Parser, PgnStr, Tags, Comment }
import chess.variant.*
import chess.{ Game as ChessGame, Centis, * }
import chess.format.pgn.{ ParsedMainline, SanWithMetas }
import lila.core.game.ClockHistory

case class TagResult(status: Status, points: Outcome.GamePoints):
  // duplicated from Game.finish
  def finished = status >= Status.Mate
  def winner: Option[Color] = Outcome.fromPoints(points).flatMap(_.winner)

case class ImportResult(
    game: ChessGame,
    result: Option[TagResult],
    replay: Replay,
    initialFen: Option[Fen.Full],
    parsed: ParsedPgn,
    replayError: Option[ErrorStr]
)

object ParseImport:
  private val maxPlies = 600

  def full(pgn: PgnStr): Either[ErrorStr, ImportResult] =
    catchOverflow: () =>
      Parser.full(pgn).map { parsed =>
        Replay
          .makeReplay(parsed.toGame, parsed.mainline.take(maxPlies))
          .pipe { case Replay.Result(replay @ Replay(setup, _, state), replayError) =>
            val (game, result, initialFen, _) = extractData(replay, parsed.tags)
            ImportResult(
              game = game,
              result = result,
              replay = replay,
              initialFen = initialFen,
              parsed = parsed,
              replayError = replayError
            )
          }
      }

  def game(pgn: PgnStr): Either[ErrorStr, ImportGameResult] =
    catchOverflow: () =>
      Parser
        .mainlineWithMetas(pgn)
        .map: parsed =>
          val result = Replay.makeReplay(parsed.toGame, parsed.moves.map(_.san).take(maxPlies))
          val (game, res, initialFen, tags) = extractData(result.replay, parsed.tags)
          (game, res, initialFen, tags, extractClocks(parsed))

  type ImportGameResult = (
      setup: ChessGame,
      result: Option[TagResult],
      initialFen: Option[Fen.Full],
      tags: Tags,
      clockHistory: Option[ClockHistory]
  )

  def extractData(replay: Replay, tags: Tags): (ChessGame, Option[TagResult], Option[Fen.Full], Tags) =
    val variant = extractVariant(replay.setup, tags)
    val initialFen = tags.fen.flatMap(Fen.readWithMoveNumber(variant, _)).map(Fen.write)
    val game = replay.state.copy(position = replay.state.position.withVariant(variant))
    val result = extractResult(game, tags)
    (game, result, initialFen, tags)

  def extractVariant(setup: ChessGame, tags: Tags): Variant =
    inline def initBoard = tags.fen.flatMap(Fen.read).map(_.board)
    @scala.annotation.threadUnsafe
    lazy val fromPosition = initBoard.nonEmpty && !tags.fen.exists(_.isInitial)

    tags.variant | {
      if fromPosition then FromPosition
      else Standard
    } match
      case Chess960 if !isChess960StartPosition(setup.position) => FromPosition
      case FromPosition if tags.fen.isEmpty => Standard
      case Standard if fromPosition => FromPosition
      case v => v

  def extractResult(game: ChessGame, tags: Tags): Option[TagResult] =
    val status = tags(_.Termination).map(_.toLowerCase) match
      case Some("normal") =>
        game.position.status | (if tags.outcome.exists(_.winner.isEmpty) then Status.Draw else Status.Resign)
      case Some("abandoned") => Status.Aborted
      case Some("time forfeit") => Status.Outoftime
      case Some("rules infraction") => Status.Cheat
      case Some(txt) if txt.contains("won on time") => Status.Outoftime
      case _ => Status.UnknownFinish

    tags.points
      .map(points => TagResult(status, points))
      .filter(_.status > Status.Started)
      .orElse {
        game.position.status.flatMap: status =>
          Outcome
            .guessPointsFromStatusAndPosition(status, game.position.winner)
            .map(TagResult(status, _))
      }

  private val clockRegex = """(?s)\[%clk[ \r\n]+([\d:\.]+)\]""".r.unanchored

  private def extractClocks(parsed: ParsedMainline[SanWithMetas]): Option[ClockHistory] =
    val clocks = parsed.moves.map: n =>
      n.metas.comments.flatMap(parseClock).lastOption.getOrElse(Centis(0))
    val whiteRemainder = if parsed.toPosition.color == Color.White then 0 else 1
    val (w, b) = clocks.zipWithIndex.partition { case (_, i) => i % 2 == whiteRemainder }
    val (white, black) = (w.map(_._1).toVector, b.map(_._1).toVector)
    if white.exists(_.value != 0) || black.exists(_.value != 0) then
      ClockHistory(w.map(_._1).toVector, b.map(_._1).toVector).some
    else none

  private def parseClock(c: Comment): Option[Centis] =
    clockRegex
      .findFirstMatchIn(c.value)
      .map(_.group(1))
      .map: clk =>
        val ticks = clk.split(":")
        val (h, m) = ticks.length match
          case 3 => (ticks(0).toInt, ticks(1).toInt)
          case 2 => (0, ticks(0).toInt)
          case _ => (0, 0)
        Centis((((h * 3600 + m * 60) + ticks.last.replace(',', '.').toFloat) * 100).toInt)

  private def isChess960StartPosition(position: Position) =
    import chess.*
    val strict =
      def rankMatches(f: Option[Piece] => Boolean)(rank: Rank) =
        File.all.forall(file => f(position.board.pieceAt(file, rank)))
      rankMatches {
        case Some(Piece(White, King | Queen | Rook | Knight | Bishop)) => true
        case _ => false
      }(Rank.First) &&
      rankMatches {
        case Some(Piece(White, Pawn)) => true
        case _ => false
      }(Rank.Second) &&
      List(Rank.Third, Rank.Fourth, Rank.Fifth, Rank.Sixth).forall(rankMatches(_.isEmpty)) &&
      rankMatches {
        case Some(Piece(Black, Pawn)) => true
        case _ => false
      }(Rank.Seventh) &&
      rankMatches {
        case Some(Piece(Black, King | Queen | Rook | Knight | Bishop)) => true
        case _ => false
      }(Rank.Eighth)

    Chess960.valid(position, strict)

  private def catchOverflow[A](f: () => Either[ErrorStr, A]): Either[ErrorStr, A] =
    try f()
    catch
      case e: RuntimeException if e.getMessage.contains("StackOverflowError") =>
        ErrorStr("This PGN seems too long or too complex!").asLeft
