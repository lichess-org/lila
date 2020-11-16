package chess
package format.pgn

import scalaz.Validation.{ failure, success }

object Reader {

  sealed trait Result {
    def valid: Valid[Replay]
  }

  object Result {
    case class Complete(replay: Replay) extends Result {
      def valid = success(replay)
    }
    case class Incomplete(replay: Replay, failures: Failures) extends Result {
      def valid = failure(failures)
    }
  }

  def full(pgn: String, tags: Tags = Tags.empty): Valid[Result] =
    fullWithSans(pgn, identity, tags)

  def moves(moveStrs: Iterable[String], tags: Tags): Valid[Result] =
    movesWithSans(moveStrs, identity, tags)

  def fullWithSans(pgn: String, op: Sans => Sans, tags: Tags = Tags.empty): Valid[Result] =
    Parser.full(cleanUserInput(pgn)) map { parsed =>
      makeReplay(makeGame(parsed.tags ++ tags), op(parsed.sans))
    }

  def fullWithSans(parsed: ParsedPgn, op: Sans => Sans): Result =
    makeReplay(makeGame(parsed.tags), op(parsed.sans))

  def movesWithSans(moveStrs: Iterable[String], op: Sans => Sans, tags: Tags): Valid[Result] =
    Parser.moves(moveStrs, tags.variant | variant.Variant.default) map { moves =>
      makeReplay(makeGame(tags), op(moves))
    }

  // remove invisible byte order mark
  def cleanUserInput(str: String) = str.replace(s"\ufeff", "")

  private def makeReplay(game: Game, sans: Sans): Result =
    sans.value.foldLeft[Result](Result.Complete(Replay(game))) {
      case (Result.Complete(replay), san) =>
        san(replay.state.situation).fold(
          err => Result.Incomplete(replay, err),
          move => Result.Complete(replay addMove move)
        )
      case (r: Result.Incomplete, _) => r
    }

  private def makeGame(tags: Tags) = {
    val g = Game(
      variantOption = tags(_.Variant) flatMap chess.variant.Variant.byName,
      fen = tags(_.FEN)
    )
    g.copy(
      startedAtTurn = g.turns,
      clock = tags.clockConfig map Clock.apply
    )
  }
}
