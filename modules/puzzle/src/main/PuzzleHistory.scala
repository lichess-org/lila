package lila.puzzle

import scalalib.paginator.{ AdapterLike, Paginator }

import lila.core.user.WithPerf
import lila.db.dsl.{ *, given }

object PuzzleHistory:

  val maxPerPage = MaxPerPage(100)

  case class SessionRound(round: PuzzleRound, puzzle: Puzzle, theme: PuzzleTheme.Key)

  case class PuzzleSession(
      theme: PuzzleTheme.Key,
      puzzles: NonEmptyList[SessionRound] // chronological order, oldest first
  )

  final class HistoryAdapter(user: WithPerf, colls: PuzzleColls)(using Executor)
      extends AdapterLike[PuzzleSession]:

    import BsonHandlers.given

    def nbResults: Fu[Int] = fuccess(user.perf.nb)

    def slice(offset: Int, length: Int): Fu[Seq[PuzzleSession]] =
      colls
        .round:
          _.aggregateList(length, _.sec): framework =>
            import framework.*
            Match($doc("u" -> user.id)) -> List(
              Sort(Descending("d")),
              Skip(offset),
              Limit(length),
              PipelineOperator(PuzzleRound.puzzleLookup(colls)),
              Unwind("puzzle")
            )
        .map: r =>
          for
            doc   <- r
            round <- doc.asOpt[PuzzleRound]
            theme = doc.getAsOpt[PuzzleTheme.Key](PuzzleRound.BSONFields.theme) | PuzzleTheme.mix.key
            puzzle <- doc.getAsOpt[Puzzle]("puzzle")
          yield SessionRound(round, puzzle, theme)
        .map(groupBySessions)

  private def groupBySessions(rounds: List[SessionRound]): List[PuzzleSession] =
    rounds
      .foldLeft(List.empty[PuzzleSession]):
        case (Nil, round) => List(PuzzleSession(round.theme, NonEmptyList(round, Nil)))
        case (last :: sessions, r) =>
          if last.puzzles.head.theme == r.theme &&
            r.round.date.isAfter(last.puzzles.head.round.date.minusHours(1))
          then last.copy(puzzles = r :: last.puzzles) :: sessions
          else PuzzleSession(r.theme, NonEmptyList(r, Nil)) :: last :: sessions
      .reverse

final class PuzzleHistoryApi(colls: PuzzleColls)(using Executor):

  import PuzzleHistory.*

  def apply(user: WithPerf, page: Int): Fu[Paginator[PuzzleSession]] =
    Paginator[PuzzleSession](
      HistoryAdapter(user, colls),
      currentPage = page,
      maxPerPage = maxPerPage
    )
