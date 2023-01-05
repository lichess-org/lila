package lila.puzzle

import scala.concurrent.ExecutionContext

import lila.db.dsl.{ *, given }
import lila.user.User

// mobile app BC
final class PuzzleBatch(colls: PuzzleColls, anonApi: PuzzleAnon, pathApi: PuzzlePathApi)(using
    ec: ExecutionContext
):

  import BsonHandlers.given

  def nextFor(user: Option[User], nb: Int): Fu[Vector[Puzzle]] =
    nextFor(user, PuzzleAngle.mix, nb)

  def nextFor(user: Option[User], angle: PuzzleAngle, nb: Int): Fu[Vector[Puzzle]] = (nb > 0) ?? {
    user.fold(anonApi.getBatchFor(angle, nb)) { user =>
      val tier =
        if (user.perfs.puzzle.nb > 5000) PuzzleTier.Good
        else PuzzleTier.Top
      pathApi
        .nextFor(user, angle, tier, PuzzleDifficulty.Normal, Set.empty)
        .orFail(s"No puzzle path for ${user.id} $tier")
        .flatMap { pathId =>
          colls.path {
            _.aggregateList(nb) { framework =>
              import framework.*
              Match($id(pathId)) -> List(
                Project($doc("puzzleId" -> "$ids", "_id" -> false)),
                Unwind("puzzleId"),
                Sample(nb),
                PipelineOperator(
                  $lookup.simple(
                    from = colls.puzzle,
                    local = "puzzleId",
                    foreign = "_id",
                    as = "puzzle"
                  )
                ),
                PipelineOperator(
                  $doc("$replaceWith" -> $doc("$arrayElemAt" -> $arr("$puzzle", 0)))
                )
              )
            }.map {
              _.view.flatMap(puzzleReader.readOpt).toVector
            }
          }
        }
        .mon(_.puzzle.selector.user.batch(nb = nb))
    }
  }
