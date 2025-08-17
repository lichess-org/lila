package lila.tree

import chess.format.Fen
import chess.format.pgn.{ ParsedPgn, Parser, PgnStr, Tags }
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
            val (game, result, initialFen, _, _) = extractData(replay, parsed.tags)
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
          extractData(result.replay, parsed.tags, clockHistory(parsed))

  type ImportGameResult = (
      game: ChessGame,
      result: Option[TagResult],
      initialFen: Option[Fen.Full],
      tags: Tags,
      clockHistory: Option[ClockHistory]
  )

  def extractData(replay: Replay, tags: Tags, clockHistory: Option[ClockHistory] = none): ImportGameResult =
    val variant = extractVariant(replay.setup, tags)
    val initialFen = tags.fen.flatMap(Fen.readWithMoveNumber(variant, _)).map(Fen.write)
    val game = replay.state.copy(position = replay.state.position.withVariant(variant))
    val result = extractResult(game, tags)
    (game, result, initialFen, tags, clockHistory)

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

  private def clockHistory(parsed: ParsedMainline[SanWithMetas]): Option[ClockHistory] =
    val clocks = parsed.moves.map: n =>
      n.metas.comments.flatMap(lila.tree.Node.Comment.clk).lastOption.orZero
    val whiteRemainder = if parsed.toPosition.color.white then 0 else 1
    val byColor = ByColor
      .fromPair(clocks.zipWithIndex.partition { (_, i) => i % 2 == whiteRemainder })
      .map(_.view.map(_._1).toVector)
    Option.when(byColor.exists(_.exists(_.value != 0)))(byColor)

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
