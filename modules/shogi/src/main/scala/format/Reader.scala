package shogi
package format

import cats.data.Validated

import shogi.format.usi.Usi

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
      usis: Seq[Usi],
      tags: Tags
  ): Result =
    makeReplayFromUsi(makeGame(tags), usis)

  private def makeReplayFromUsi(game: Game, usis: Seq[Usi]): Result =
    usis.foldLeft[Result](Result.Complete(Replay(game))) {
      case (Result.Complete(replay), usi) =>
        replay
          .state(usi)
          .fold(
            err => Result.Incomplete(replay, err),
            game => Result.Complete(replay(game))
          )
      case (r: Result.Incomplete, _) => r
    }

  private def makeReplayFromParsedMoves(game: Game, parsedMoves: ParsedMoves): Result =
    parsedMoves.value.foldLeft[Result](Result.Complete(Replay(game))) {
      case (Result.Complete(replay), parsedMove) =>
        replay
          .state(parsedMove)
          .fold(
            err => Result.Incomplete(replay, err),
            game => Result.Complete(replay(game))
          )
      case (r: Result.Incomplete, _) => r
    }

  private def makeGame(tags: Tags) =
    Game(
      variantOption = tags(_.Variant) flatMap shogi.variant.Variant.byName,
      sfen = tags.sfen
    ).copy(
      clock = tags.clockConfig map Clock.apply
    )
}
