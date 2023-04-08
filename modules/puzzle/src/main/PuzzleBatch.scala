package lila.puzzle

import lila.db.dsl.*
import lila.user.User

// mobile app BC
final class PuzzleBatch(colls: PuzzleColls, anonApi: PuzzleAnon, pathApi: PuzzlePathApi)(using
    Executor
):

  import BsonHandlers.given

  def nextFor(user: Option[User], difficulty: PuzzleDifficulty, nb: Int): Fu[Vector[Puzzle]] =
    nextFor(user, PuzzleAngle.mix, difficulty, nb)

  def nextFor(
      user: Option[User],
      angle: PuzzleAngle,
      difficulty: PuzzleDifficulty,
      nb: Int
  ): Fu[Vector[Puzzle]] = (nb > 0) ?? {
    user.fold(anonApi.getBatchFor(angle, nb)) { user =>
      val tier =
        if user.perfs.puzzle.nb > 5000 then PuzzleTier.good
        else if PuzzleDifficulty.isExtreme(difficulty) then PuzzleTier.good
        else PuzzleTier.top
      pathApi
        .nextFor(user, angle, tier, difficulty, Set.empty)
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
