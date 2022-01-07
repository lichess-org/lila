package shogi
package format

import cats.data.Validated
import format.usi.Usi

object Reader {

  sealed trait Result {
    def valid: Validated[String, Replay]
  }

  object Result {
    case class Complete(replay: Replay) extends Result {
      def valid = Validated.valid(replay)
    }
    case class Incomplete(replay: Replay, failures: String) extends Result {
      def valid = Validated.invalid(failures)
    }
  }

  def fromParsedNotation(parsed: ParsedNotation, op: ParsedMoves => ParsedMoves): Result =
    makeReplayFromParsedMoves(makeGame(parsed.tags), op(parsed.parsedMoves))

  def fromUsi(
      usis: Iterable[Usi],
      tags: Tags
  ): Result =
    makeReplayFromUsi(makeGame(tags), usis)

  private def makeReplayFromUsi(game: Game, usis: Iterable[Usi]): Result =
    usis.foldLeft[Result](Result.Complete(Replay(game))) {
      case (Result.Complete(replay), usi) =>
        usi(replay.state.situation).fold(
          err => Result.Incomplete(replay, err),
          move => Result.Complete(replay addMove move)
        )
      case (r: Result.Incomplete, _) => r
    }

  private def makeReplayFromParsedMoves(game: Game, parsedMoves: ParsedMoves): Result =
    parsedMoves.value.foldLeft[Result](Result.Complete(Replay(game))) {
      case (Result.Complete(replay), parsedMove) =>
        parsedMove(replay.state.situation).fold(
          err => Result.Incomplete(replay, err),
          move => Result.Complete(replay addMove move)
        )
      case (r: Result.Incomplete, _) => r
    }

  private def makeGame(tags: Tags) =
    Game(
      variantOption = tags(_.Variant) flatMap shogi.variant.Variant.byName,
      fen = tags(_.FEN)
    ).copy(
      clock = tags.clockConfig map Clock.apply
    )
}
