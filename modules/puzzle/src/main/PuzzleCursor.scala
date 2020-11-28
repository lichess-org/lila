package lila.puzzle

import reactivemongo.api.bson.BSONRegex
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.chaining._

import lila.db.dsl._
import lila.memo.CacheApi
import lila.rating.{ Perf, PerfType }
import lila.user.{ User, UserRepo }

private case class PuzzleCursor(
    theme: PuzzleTheme.Key,
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
    ec: ExecutionContext,
    system: akka.actor.ActorSystem,
    mode: play.api.Mode
) {

  import BsonHandlers._
  import Puzzle.PathId

  sealed private trait NextPuzzleResult
  private object NextPuzzleResult {
    case object PathMissing                        extends NextPuzzleResult
    case object PathEnded                          extends NextPuzzleResult
    case class PuzzleMissing(id: Puzzle.Id)        extends NextPuzzleResult
    case class PuzzleAlreadyPlayed(puzzle: Puzzle) extends NextPuzzleResult
    case class PuzzleFound(puzzle: Puzzle)         extends NextPuzzleResult
  }

  def nextPuzzleFor(user: User, theme: PuzzleTheme.Key, isRetry: Boolean = false): Fu[Puzzle] =
    continueOrCreateCursorFor(user, theme) flatMap { cursor =>
      import NextPuzzleResult._
      nextPuzzleResult(user, cursor.pp) flatMap {
        case PathMissing | PathEnded if !isRetry =>
          nextPathIdFor(user.id, theme, cursor.previousPaths) flatMap {
            case None => fufail(s"No remaining puzzle path for ${user.id}")
            case Some(pathId) =>
              val newCursor = cursor switchTo pathId
              cursors.put(user.id, fuccess(newCursor))
              nextPuzzleFor(user, theme, isRetry = true)
          }
        case PathMissing | PathEnded => fufail(s"Puzzle patth missing or ended for ${user.id}")
        case PuzzleMissing(id) =>
          logger.warn(s"Puzzle missing: $id")
          cursors.put(user.id, fuccess(cursor.next))
          nextPuzzleFor(user, theme, isRetry = isRetry)
        case PuzzleAlreadyPlayed(_) =>
          cursors.put(user.id, fuccess(cursor.next))
          nextPuzzleFor(user, theme, isRetry = isRetry)
        case PuzzleFound(puzzle) => fuccess(puzzle)
      }
    }

  private def nextPuzzleResult(user: User, cursor: PuzzleCursor): Fu[NextPuzzleResult] =
    colls.path {
      _.aggregateOne() { framework =>
        import framework._
        Match($id(cursor.path)) -> List(
          // get the puzzle ID from cursor position
          Project($doc("puzzleId" -> $doc("$arrayElemAt" -> $arr("$ids", cursor.positionInPath)))),
          Project(
            $doc(
              "puzzleId" -> true,
              "roundId"  -> $doc("$concat" -> $arr(s"${user.id}${PuzzleRound.idSep}", "$puzzleId"))
            )
          ),
          // fetch the puzzle
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
          // look for existing round
          PipelineOperator(
            $doc(
              "$lookup" -> $doc(
                "from"         -> colls.round.name.value,
                "localField"   -> "roundId",
                "foreignField" -> "_id",
                "as"           -> "round"
              )
            )
          )
        )
      }.map { docOpt =>
        import NextPuzzleResult._
        // println(docOpt map lila.db.BSON.debug)
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

  def onComplete(round: PuzzleRound, theme: PuzzleTheme.Key): Unit =
    cursors.getIfPresent(round.userId) foreach {
      _.filter(_.theme == theme) foreach { cursor =>
        // yes, even if the completed puzzle was not the current cursor puzzle
        // in that case we just skip a puzzle on the path, which doesn't matter
        cursors.put(round.userId, fuccess(cursor.next))
      }
    }

  private val cursors = cacheApi.notLoading[User.ID, PuzzleCursor](32768, "puzzle.cursor")(
    _.expireAfterWrite(1 hour).buildAsync()
  )

  private[puzzle] def currentCursorOf(user: User, theme: PuzzleTheme.Key): Fu[PuzzleCursor] =
    cursors.getFuture(user.id, _ => createCursorFor(user, theme))

  private[puzzle] def continueOrCreateCursorFor(
      user: User,
      theme: PuzzleTheme.Key
  ): Fu[PuzzleCursor] =
    currentCursorOf(user, theme) flatMap { current =>
      if (current.theme == theme) fuccess(current)
      else createCursorFor(user, theme) tap { cursors.put(user.id, _) }
    }

  private def createCursorFor(user: User, theme: PuzzleTheme.Key): Fu[PuzzleCursor] =
    nextPathIdFor(user.id, theme, Set.empty)
      .orFail(s"No puzzle path found for ${user.id}, theme: $theme")
      .dmap(pathId => PuzzleCursor(theme, pathId, Set.empty, 0))

  private def nextPathIdFor(
      userId: User.ID,
      theme: PuzzleTheme.Key,
      previousPaths: Set[PathId]
  ): Fu[Option[PathId]] =
    userRepo.perfOf(userId, PerfType.Puzzle).dmap(_ | Perf.default) flatMap { perf =>
      colls.path {
        _.aggregateOne() { framework =>
          import framework._
          val tier = "top"
          Match(
            $doc(
              "_id" ->
                $doc(
                  "$regex" -> BSONRegex(s"^${theme}_$tier", ""),
                  $nin(previousPaths)
                ),
              "min" $lte perf.glicko.rating,
              "max" $gt perf.glicko.rating
            )
          ) -> List(
            Project($id(true)),
            Sample(1)
          )
        }.dmap(_.flatMap(_.getAsOpt[PathId]("_id")))
      }
    }
}
