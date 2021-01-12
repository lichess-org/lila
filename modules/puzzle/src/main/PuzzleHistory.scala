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

private object PuzzleHistory {
  case class Session(theme: PuzzleTheme.Key, puzzles: List[PuzzleRound.WithPuzzle])

  final class HistoryAdapter(user: User, colls: PuzzleColls)(implicit ec: ExecutionContext)
      extends AdapterLike[PuzzleRound.WithPuzzle] {

    import BsonHandlers._

    def nbResults: Fu[Int] = fuccess(user.perfs.puzzle.nb)

    def slice(offset: Int, length: Int): Fu[Seq[PuzzleRound.WithPuzzle]] =
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
            doc    <- r
            round  <- doc.asOpt[PuzzleRound]
            puzzle <- doc.getAsOpt[Puzzle]("puzzle")
          } yield PuzzleRound.WithPuzzle(round, puzzle)
        }
  }
}

final class PuzzleHistoryApi(
    colls: PuzzleColls,
    cacheApi: CacheApi
)(implicit ec: ExecutionContext) {

  def apply(user: User, page: Int): Fu[Paginator[PuzzleRound.WithPuzzle]] =
    Paginator[PuzzleRound.WithPuzzle](
      new PuzzleHistory.HistoryAdapter(user, colls),
      currentPage = page,
      maxPerPage = MaxPerPage(50)
    )

}
