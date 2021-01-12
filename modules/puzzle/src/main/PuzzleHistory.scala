package lila.puzzle

import org.joda.time.DateTime
import reactivemongo.api.bson.BSONNull
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.chaining._

import lila.common.paginator.AdapterLike
import lila.db.dsl._
import lila.memo.CacheApi
import lila.user.User
import lila.common.paginator.Paginator
import lila.common.config.MaxPerPage
import cats.data.NonEmptyList

object PuzzleHistory {

  case class SessionRound(round: PuzzleRound, puzzle: Puzzle, theme: PuzzleTheme.Key)

  case class PuzzleSession(
      theme: PuzzleTheme.Key,
      puzzles: NonEmptyList[SessionRound] // chronological order, oldest first
  )

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
        case (last :: sessions, round) =>
          if (
            last.puzzles.head.theme == round.theme &&
            last.puzzles.head.round.date.isAfter(round.round.date minusMinutes 15)
          )
            last.copy(puzzles = round :: last.puzzles) :: sessions
          else PuzzleSession(round.theme, NonEmptyList(round, Nil)) :: last :: sessions
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
      maxPerPage = MaxPerPage(50)
    )

}
