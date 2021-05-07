package lila.puzzle

import cats.data.NonEmptyList
import reactivemongo.api.ReadPreference
import scala.concurrent.ExecutionContext

import lila.common.config.MaxPerPage
import lila.common.paginator.AdapterLike
import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.memo.CacheApi
import lila.user.User

object PuzzleHistory {

  val maxPerPage = MaxPerPage(100)

  case class SessionRound(round: PuzzleRound, puzzle: Puzzle, theme: PuzzleTheme.Key)

  case class PuzzleSession(
      theme: PuzzleTheme.Key,
      puzzles: NonEmptyList[SessionRound] // chronological order, oldest first
  ) {
    // val nb              = puzzles.size
    // val firstWins       = puzzles.toList.count(_.round.firstWin)
    // val fails           = nb - firstWins
    // def puzzleRatingAvg = puzzles.toList.foldLeft(0)(_ + _.puzzle.glicko.intRating)
    // def performance     = puzzleRatingAvg - 500 + math.round(1000 * (firstWins.toFloat / nb))
  }

  final class HistoryAdapter(user: User, colls: PuzzleColls)(implicit ec: ExecutionContext)
      extends AdapterLike[PuzzleSession] {

    import BsonHandlers._

    def nbResults: Fu[Int] = fuccess(user.perfs.puzzle.nb)

    def slice(offset: Int, length: Int): Fu[Seq[PuzzleSession]] =
      colls
        .round {
          _.aggregateList(length, readPreference = ReadPreference.secondaryPreferred) { framework =>
            import framework._
            Match($doc("u" -> user.id)) -> List(
              Sort(Descending("d")),
              Skip(offset),
              Limit(length),
              PipelineOperator(PuzzleRound puzzleLookup colls),
              Unwind("puzzle")
            )
          }
        }
        .map { r =>
          for {
            doc   <- r
            round <- doc.asOpt[PuzzleRound]
            theme = doc.getAsOpt[PuzzleTheme.Key](PuzzleRound.BSONFields.theme) | PuzzleTheme.mix.key
            puzzle <- doc.getAsOpt[Puzzle]("puzzle")
          } yield SessionRound(round, puzzle, theme)
        }
        .map(groupBySessions)
  }

  private def groupBySessions(rounds: List[SessionRound]): List[PuzzleSession] =
    rounds
      .foldLeft(List.empty[PuzzleSession]) {
        case (Nil, round) => List(PuzzleSession(round.theme, NonEmptyList(round, Nil)))
        case (last :: sessions, r) =>
          if (
            last.puzzles.head.theme == r.theme &&
            r.round.date.isAfter(last.puzzles.head.round.date minusHours 1)
          )
            last.copy(puzzles = r :: last.puzzles) :: sessions
          else PuzzleSession(r.theme, NonEmptyList(r, Nil)) :: last :: sessions
      }
      .reverse
}

final class PuzzleHistoryApi(
    colls: PuzzleColls,
    cacheApi: CacheApi
)(implicit ec: ExecutionContext) {

  import PuzzleHistory._

  def apply(user: User, page: Int): Fu[Paginator[PuzzleSession]] =
    Paginator[PuzzleSession](
      new HistoryAdapter(user, colls),
      currentPage = page,
      maxPerPage = maxPerPage
    )

}
