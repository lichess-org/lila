package lila.puzzle

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.memo.CacheApi
import lila.rating.{ Perf, PerfType }
import lila.user.{ User, UserRepo }

private case class PuzzleCursor(
    path: Puzzle.PathId,
    previousPaths: Set[Puzzle.PathId],
    positionInPath: Int
) {
  def switchTo(pathId: Puzzle.PathId) = copy(
    path = pathId,
    previousPaths = previousPaths + pathId,
    positionInPath = 0
  )
  def next = copy(positionInPath = positionInPath + 1)
}

final class PuzzleCursorApi(colls: PuzzleColls, cacheApi: CacheApi, userRepo: UserRepo)(implicit
    ec: ExecutionContext
) {

  import BsonHandlers._
  import Puzzle.PathId

  private[puzzle] def cursorOf(user: User): Fu[PuzzleCursor] =
    cursors.get(user.id)

  sealed private trait NextPuzzleResult
  private object NextPuzzleResult {
    case object PathMissing                        extends NextPuzzleResult
    case object PathEnded                          extends NextPuzzleResult
    case class PuzzleMissing(id: Puzzle.Id)        extends NextPuzzleResult
    case class PuzzleAlreadyPlayed(puzzle: Puzzle) extends NextPuzzleResult
    case class PuzzleFound(puzzle: Puzzle)         extends NextPuzzleResult
  }

  def nextPuzzleFor(user: User, isRetry: Boolean = false): Fu[Puzzle] =
    cursorOf(user) flatMap { cursor =>
      import NextPuzzleResult._
      nextPuzzleResult(user, cursor).thenPp flatMap {
        case PathMissing | PathEnded if !isRetry =>
          nextPathIdFor(user.id, cursor.previousPaths) flatMap {
            case None => fufail(s"No remaining puzzle path for ${user.id}")
            case Some(pathId) =>
              val newCursor = cursor switchTo pathId
              cursors.put(user.id, fuccess(newCursor))
              nextPuzzleFor(user, isRetry = true)
          }
        case PathMissing | PathEnded => fufail(s"Puzzle patth missing or ended for ${user.id}")
        case PuzzleMissing(id) =>
          logger.warn(s"Puzzle missing: $id")
          cursors.put(user.id, fuccess(cursor.next))
          nextPuzzleFor(user, isRetry = isRetry)
        case PuzzleAlreadyPlayed(_) =>
          cursors.put(user.id, fuccess(cursor.next))
          nextPuzzleFor(user, isRetry = isRetry)
        case PuzzleFound(puzzle) => fuccess(puzzle)
      }
    }

  private def nextPuzzleResult(user: User, cursor: PuzzleCursor): Fu[NextPuzzleResult] =
    colls.path {
      _.aggregateOne() { framework =>
        import framework._
        Match($id(cursor.path)) -> List(
          Project($doc("puzzleId" -> $doc("$arrayElemAt" -> $arr("$ids", 0)))),
          PipelineOperator(
            $doc(
              "$lookup" -> $doc(
                "from"         -> colls.puzzle.name.value,
                "localField"   -> "puzzleId",
                "foreignField" -> "_id",
                "as"           -> "puzzle"
              )
            )
          ),
          PipelineOperator(
            $doc(
              "$lookup" -> $doc(
                "from" -> colls.round.name.value,
                "let" -> $doc(
                  "roundId" -> $doc("$concat" -> $arr(s"${user.id}${PuzzleRound.idSep}", "$puzzleId"))
                ),
                "pipeline" -> $arr(
                  $doc("$match" -> $id("$$roundId")),
                  // $doc("$match"   -> $id("thibault:313og")),
                  $doc("$project" -> $doc("i" -> "$$roundId"))
                ),
                "as" -> "round"
              )
            )
          )
        )
      }.map { docOpt =>
        import NextPuzzleResult._
        println(docOpt map lila.db.BSON.debug)
        docOpt.fold[NextPuzzleResult](PathMissing) { doc =>
          doc.getAsOpt[Puzzle.Id]("puzzleId").fold[NextPuzzleResult](PathEnded) { puzzleId =>
            doc
              .getAsOpt[List[Puzzle]]("puzzle")
              .flatMap(_.headOption)
              .fold[NextPuzzleResult](PuzzleMissing(puzzleId)) { puzzle =>
                if (doc.getAsOpt[List[Bdoc]]("round").exists(_.nonEmpty)) PuzzleAlreadyPlayed(puzzle)
                else PuzzleFound(puzzle)
              }
          }
        }
      }
    }

  private val cursors = cacheApi[User.ID, PuzzleCursor](32768, "puzzle.cursor")(
    _.expireAfterWrite(1 hour)
      .buildAsyncFuture { userId =>
        nextPathIdFor(userId, Set.empty)
          .orFail(s"No puzzle path found for $userId")
          .dmap(pathId => PuzzleCursor(pathId, Set.empty, 0))
      }
  )

  private def nextPathIdFor(userId: User.ID, previousPaths: Set[PathId]): Fu[Option[PathId]] =
    userRepo.perfOf(userId, PerfType.Puzzle).dmap(_ | Perf.default) flatMap { perf =>
      colls.path {
        _.aggregateOne() { framework =>
          import framework._
          Match(
            $doc(
              "tier" -> "top",
              "min" $lte perf.glicko.rating,
              "max" $gt perf.glicko.rating,
              "_id" $nin previousPaths
            )
          ) -> List(
            Project($id(true)),
            Sample(1)
          )
        }.dmap(_.flatMap(_.getAsOpt[PathId]("_id")))
      }
    }
}
