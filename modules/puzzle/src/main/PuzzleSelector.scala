package lila.puzzle

import lila.db.dsl.{ *, given }
import lila.user.User

final class PuzzleSelector(
    colls: PuzzleColls,
    pathApi: PuzzlePathApi,
    sessionApi: PuzzleSessionApi
)(using Executor):

  import BsonHandlers.given

  private enum NextPuzzleResult(val name: String):
    case PathMissing                         extends NextPuzzleResult("pathMissing")
    case PathEnded                           extends NextPuzzleResult("pathEnded")
    case WrongColor(puzzle: Puzzle)          extends NextPuzzleResult("wrongColor")
    case PuzzleMissing(id: PuzzleId)         extends NextPuzzleResult("puzzleMissing")
    case PuzzleAlreadyPlayed(puzzle: Puzzle) extends NextPuzzleResult("puzzlePlayed")
    case PuzzleFound(puzzle: Puzzle)         extends NextPuzzleResult("puzzleFound")

  def nextPuzzleFor(user: User, angle: PuzzleAngle): Fu[Option[Puzzle]] =
    findNextPuzzleFor(user, angle, 0)
      .fold(
        err =>
          logger.warn(s"PuzzleSelector.nextPuzzleFor ${err.getMessage}")
          none
        ,
        some
      )
      .mon(_.puzzle.selector.user.time(angle.key))

  private def findNextPuzzleFor(user: User, angle: PuzzleAngle, retries: Int): Fu[Puzzle] =
    sessionApi
      .continueOrCreateSessionFor(user, angle, canFlush = retries == 0)
      .flatMap { session =>
        import NextPuzzleResult.*

        def switchPath(withRetries: Int)(tier: PuzzleTier) =
          pathApi
            .nextFor(user, angle, tier, session.settings.difficulty, session.previousPaths) orFail
            s"No puzzle path for ${user.id} $angle $tier" flatMap { pathId =>
              val newSession = session.switchTo(pathId)
              sessionApi.set(user, newSession)
              findNextPuzzleFor(user, angle, retries = withRetries + 1)
            }

        def serveAndMonitor(puzzle: Puzzle) =
          val mon = lila.mon.puzzle.selector.user
          mon.retries(angle.key).record(retries)
          mon.vote(angle.key).record(100 + math.round(puzzle.vote * 100))
          mon
            .ratingDiff(angle.key, session.settings.difficulty.key)
            .record(math.abs(puzzle.glicko.intRating.value - user.perfs.puzzle.intRating.value))
          mon.ratingDev(angle.key).record(puzzle.glicko.intDeviation)
          mon.tier(session.path.tier.key, angle.key, session.settings.difficulty.key).increment().unit
          puzzle

        nextPuzzleResult(user, session)
          .flatMap {
            case PathMissing | PathEnded if retries < 10 => switchPath(retries)(session.path.tier)
            case PathMissing | PathEnded => fufail(s"Puzzle path missing or ended for ${user.id}")
            case PuzzleMissing(id) =>
              logger.warn(s"Puzzle missing: $id")
              sessionApi.set(user, session.next)
              findNextPuzzleFor(user, angle, retries + 1)
            case PuzzleAlreadyPlayed(_) if retries < 5 =>
              sessionApi.set(user, session.next)
              findNextPuzzleFor(user, angle, retries = retries + 1)
            case PuzzleAlreadyPlayed(puzzle) =>
              session.path.tier.stepDown.fold(fuccess(serveAndMonitor(puzzle)))(switchPath(retries))
            case WrongColor(_) if retries < 10 =>
              sessionApi.set(user, session.next)
              findNextPuzzleFor(user, angle, retries = retries + 1)
            case WrongColor(puzzle) =>
              session.path.tier.stepDown.fold(fuccess(serveAndMonitor(puzzle)))(switchPath(retries - 5))
            case PuzzleFound(puzzle) => fuccess(serveAndMonitor(puzzle))
          }
      }

  private def nextPuzzleResult(user: User, session: PuzzleSession): Fu[NextPuzzleResult] =
    colls
      .path {
        _.aggregateOne() { framework =>
          import framework.*
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
        import NextPuzzleResult.*
        docOpt.fold[NextPuzzleResult](PathMissing) { doc =>
          doc.getAsOpt[PuzzleId]("puzzleId").fold[NextPuzzleResult](PathEnded) { puzzleId =>
            doc
              .getAsOpt[List[Puzzle]]("puzzle")
              .flatMap(_.headOption)
              .fold[NextPuzzleResult](PuzzleMissing(puzzleId)) { puzzle =>
                if (session.settings.color.exists(puzzle.color !=)) WrongColor(puzzle)
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
