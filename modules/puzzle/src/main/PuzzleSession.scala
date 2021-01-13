package lila.puzzle

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.chaining._

import lila.db.dsl._
import lila.memo.CacheApi
import lila.user.{ User, UserRepo }

private case class PuzzleSession(
    difficulty: PuzzleDifficulty,
    path: PuzzlePath.Id,
    positionInPath: Int,
    previousPaths: Set[PuzzlePath.Id] = Set.empty
) {
  def switchTo(pathId: PuzzlePath.Id) = copy(
    path = pathId,
    previousPaths = previousPaths + pathId,
    positionInPath = 0
  )
  def next = copy(positionInPath = positionInPath + 1)

  override def toString = s"$path:$positionInPath"
}

final class PuzzleSessionApi(
    colls: PuzzleColls,
    pathApi: PuzzlePathApi,
    cacheApi: CacheApi,
    userRepo: UserRepo
)(implicit ec: ExecutionContext) {

  import BsonHandlers._

  sealed abstract private class NextPuzzleResult(val name: String)
  private object NextPuzzleResult {
    case object PathMissing                        extends NextPuzzleResult("pathMissing")
    case object PathEnded                          extends NextPuzzleResult("pathEnded")
    case class PuzzleMissing(id: Puzzle.Id)        extends NextPuzzleResult("puzzleMissing")
    case class PuzzleAlreadyPlayed(puzzle: Puzzle) extends NextPuzzleResult("puzzlePlayed")
    case class PuzzleFound(puzzle: Puzzle)         extends NextPuzzleResult("puzzleFound")
  }

  def nextPuzzleFor(user: User, theme: PuzzleTheme.Key, retries: Int = 0): Fu[Puzzle] =
    continueOrCreateSessionFor(user, theme)
      .flatMap { session =>
        import NextPuzzleResult._

        def switchPath(tier: PuzzleTier) =
          pathApi.nextFor(user, theme, tier, session.difficulty, session.previousPaths) orFail
            s"No puzzle path for ${user.id} $theme $tier" flatMap { pathId =>
              val newSession = session.switchTo(pathId)
              sessions.put(user.id, fuccess(newSession))
              nextPuzzleFor(user, theme, retries = retries + 1)
            }

        def serveAndMonitor(puzzle: Puzzle) = {
          val mon = lila.mon.puzzle.selector.user
          mon.retries(theme.value).record(retries)
          mon.vote(theme.value).record(100 + math.round(puzzle.vote * 100))
          mon
            .ratingDiff(theme.value, session.difficulty.key)
            .record(math.abs(puzzle.glicko.intRating - user.perfs.puzzle.intRating))
          mon.ratingDev(theme.value).record(puzzle.glicko.intDeviation)
          mon.tier(session.path.tier.key, theme.value, session.difficulty.key).increment().unit
          puzzle
        }

        nextPuzzleResult(user, session)
          .flatMap {
            case PathMissing | PathEnded if retries < 10 => switchPath(session.path.tier)
            case PathMissing | PathEnded                 => fufail(s"Puzzle path missing or ended for ${user.id}")
            case PuzzleMissing(id) =>
              logger.warn(s"Puzzle missing: $id")
              sessions.put(user.id, fuccess(session.next))
              nextPuzzleFor(user, theme, retries)
            case PuzzleAlreadyPlayed(_) if retries < 3 =>
              sessions.put(user.id, fuccess(session.next))
              nextPuzzleFor(user, theme, retries = retries + 1)
            case PuzzleAlreadyPlayed(puzzle) =>
              session.path.tier.stepDown.fold(fuccess(serveAndMonitor(puzzle)))(switchPath)
            case PuzzleFound(puzzle) => fuccess(serveAndMonitor(puzzle))
          }
      }
      .mon(_.puzzle.selector.user.time(theme.value))

  private def nextPuzzleResult(user: User, session: PuzzleSession): Fu[NextPuzzleResult] =
    colls
      .path {
        _.aggregateOne() { framework =>
          import framework._
          Match($id(session.path)) -> List(
            // get the puzzle ID from session position
            Project($doc("puzzleId" -> $doc("$arrayElemAt" -> $arr("$ids", session.positionInPath)))),
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
        }
      }
      .map { docOpt =>
        import NextPuzzleResult._
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
      .monValue { result =>
        _.puzzle.selector.nextPuzzleResult(
          theme = session.path.theme.value,
          difficulty = session.difficulty.key,
          result = result.name
        )
      }

  def onComplete(round: PuzzleRound, theme: PuzzleTheme.Key): Funit =
    sessions.getIfPresent(round.userId) ?? {
      _ map { session =>
        // yes, even if the completed puzzle was not the current session puzzle
        // in that case we just skip a puzzle on the path, which doesn't matter
        if (session.path.theme == theme)
          sessions.put(round.userId, fuccess(session.next))
      }
    }

  def getDifficulty(user: User): Fu[PuzzleDifficulty] =
    sessions
      .getIfPresent(user.id)
      .fold[Fu[PuzzleDifficulty]](fuccess(PuzzleDifficulty.default))(_.dmap(_.difficulty))

  def setDifficulty(user: User, difficulty: PuzzleDifficulty): Funit =
    sessions.getIfPresent(user.id).fold(fuccess(PuzzleTheme.mix.key))(_.dmap(_.path.theme)) flatMap { theme =>
      createSessionFor(user, theme, difficulty).tap { sessions.put(user.id, _) }.void
    }

  private val sessions = cacheApi.notLoading[User.ID, PuzzleSession](16384, "puzzle.session")(
    _.expireAfterWrite(1 hour).buildAsync()
  )

  private[puzzle] def continueOrCreateSessionFor(
      user: User,
      theme: PuzzleTheme.Key
  ): Fu[PuzzleSession] =
    sessions.getFuture(user.id, _ => createSessionFor(user, theme)) flatMap { current =>
      if (current.path.theme == theme) fuccess(current)
      else createSessionFor(user, theme, current.difficulty) tap { sessions.put(user.id, _) }
    }

  private def createSessionFor(
      user: User,
      theme: PuzzleTheme.Key,
      difficulty: PuzzleDifficulty = PuzzleDifficulty.default
  ): Fu[PuzzleSession] =
    pathApi
      .nextFor(user, theme, PuzzleTier.Top, difficulty, Set.empty)
      .orFail(s"No puzzle path found for ${user.id}, theme: $theme")
      .dmap(pathId => PuzzleSession(difficulty, pathId, 0))
}
