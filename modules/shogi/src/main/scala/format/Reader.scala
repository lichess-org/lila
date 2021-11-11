package shogi
package format

import cats.data.Validated
import pgn.Parser

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

  def full(pgn: String, tags: Tags = Tags.empty): Validated[String, Result] =
    fullWithPgn(pgn, identity, tags)

  def moves(moveStrs: Iterable[String], tags: Tags): Validated[String, Result] =
    movesWithPgn(moveStrs, identity, tags)

  def fullWithPgn(
      pgn: String,
      op: ParsedMoves => ParsedMoves,
      tags: Tags = Tags.empty
  ): Validated[String, Result] =
    Parser.full(cleanUserInput(pgn)) map { parsed =>
      makeReplay(makeGame(parsed.tags ++ tags), op(parsed.parsedMoves))
    }

  def fullWithParsedMoves(parsed: ParsedNotation, op: ParsedMoves => ParsedMoves): Result =
    makeReplay(makeGame(parsed.tags), op(parsed.parsedMoves))

  def movesWithPgn(
      moveStrs: Iterable[String],
      op: ParsedMoves => ParsedMoves,
      tags: Tags
  ): Validated[String, Result] =
    Parser.moves(moveStrs, tags.variant | variant.Variant.default) map { moves =>
      makeReplay(makeGame(tags), op(moves))
    }

  // remove invisible byte order mark
  def cleanUserInput(str: String) = str.replace(s"\ufeff", "")

  private def makeReplay(game: Game, parsedMoves: ParsedMoves): Result =
    parsedMoves.value.foldLeft[Result](Result.Complete(Replay(game))) {
      case (Result.Complete(replay), san) =>
        san(replay.state.situation).fold(
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
