package lila.puzzle

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.chaining._
import chess.Color

import lila.db.dsl._
import lila.user.User

final class PuzzleSelector(
    colls: PuzzleColls,
    pathApi: PuzzlePathApi,
    sessionApi: PuzzleSessionApi
)(implicit ec: ExecutionContext) {

  import BsonHandlers._

  sealed abstract private class NextPuzzleResult(val name: String)
  private object NextPuzzleResult {
    case object PathMissing                        extends NextPuzzleResult("pathMissing")
    case object PathEnded                          extends NextPuzzleResult("pathEnded")
    case object WrongColor                         extends NextPuzzleResult("wrongColor")
    case class PuzzleMissing(id: Puzzle.Id)        extends NextPuzzleResult("puzzleMissing")
    case class PuzzleAlreadyPlayed(puzzle: Puzzle) extends NextPuzzleResult("puzzlePlayed")
    case class PuzzleFound(puzzle: Puzzle)         extends NextPuzzleResult("puzzleFound")
  }

  def nextPuzzleFor(user: User, angle: PuzzleAngle, retries: Int = 0): Fu[Puzzle] =
    sessionApi
      .continueOrCreateSessionFor(user, angle)
      .flatMap { session =>
        import NextPuzzleResult._

        def switchPath(tier: PuzzleTier) =
          pathApi.nextFor(user, angle, tier, session.settings.difficulty, session.previousPaths) orFail
            s"No puzzle path for ${user.id} $angle $tier" flatMap { pathId =>
              val newSession = session.switchTo(pathId)
              sessionApi.set(user, newSession)
              nextPuzzleFor(user, angle, retries = retries + 1)
            }

        def serveAndMonitor(puzzle: Puzzle) = {
          val mon = lila.mon.puzzle.selector.user
          mon.retries(angle.key).record(retries)
          mon.vote(angle.key).record(100 + math.round(puzzle.vote * 100))
          mon
            .ratingDiff(angle.key, session.settings.difficulty.key)
            .record(math.abs(puzzle.glicko.intRating - user.perfs.puzzle.intRating))
          mon.ratingDev(angle.key).record(puzzle.glicko.intDeviation)
          mon.tier(session.path.tier.key, angle.key, session.settings.difficulty.key).increment().unit
          puzzle
        }

        nextPuzzleResult(user, session)
          .flatMap {
            case PathMissing | PathEnded | WrongColor =>
              if (retries < 10) switchPath(session.path.tier)
              else fufail(s"Puzzle path missing or ended for ${user.id}")
            case PuzzleMissing(id) =>
              logger.warn(s"Puzzle missing: $id")
              sessionApi.set(user, session.next)
              nextPuzzleFor(user, angle, retries)
            case PuzzleAlreadyPlayed(_) if retries < 3 =>
              sessionApi.set(user, session.next)
              nextPuzzleFor(user, angle, retries = retries + 1)
            case PuzzleAlreadyPlayed(puzzle) =>
              session.path.tier.stepDown.fold(fuccess(serveAndMonitor(puzzle)))(switchPath)
            case PuzzleFound(puzzle) => fuccess(serveAndMonitor(puzzle))
          }
      }
      .mon(_.puzzle.selector.user.time(angle.key))

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
                if (session.settings.color.exists(puzzle.color !=)) WrongColor
                else if (doc.getAsOpt[List[Bdoc]]("round").exists(_.nonEmpty)) PuzzleAlreadyPlayed(puzzle)
                else PuzzleFound(puzzle)
              }
          }
        }
      }
      .monValue { result =>
        _.puzzle.selector.nextPuzzleResult(
          theme = session.path.angle.key,
          difficulty = session.settings.difficulty.key,
          color = session.settings.color.fold("random")(_.name),
          result = result.name
        )
      }
}
