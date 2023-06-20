package lila.puzzle

import lila.db.dsl.*
import lila.user.Me

// mobile app
final class PuzzleBatch(colls: PuzzleColls, anonApi: PuzzleAnon, pathApi: PuzzlePathApi)(using
    Executor
):

  import BsonHandlers.given

  def nextForMe(difficulty: PuzzleDifficulty, nb: Int)(using Option[Me]): Fu[Vector[Puzzle]] =
    nextForMe(PuzzleAngle.mix, difficulty, nb)

  def nextForMe(
      angle: PuzzleAngle,
      difficulty: PuzzleDifficulty,
      nb: Int
  )(using me: Option[Me]): Fu[Vector[Puzzle]] = (nb > 0).so:
    me.fold(anonApi.getBatchFor(angle, difficulty, nb)): me =>
      val tier =
        if me.perfs.puzzle.nb > 5000 then PuzzleTier.good
        else if PuzzleDifficulty.isExtreme(difficulty) then PuzzleTier.good
        else PuzzleTier.top
      pathApi
        .nextFor(me, angle, tier, difficulty, Set.empty)
        .orFail(s"No puzzle path for ${me.username} $tier")
        .flatMap: pathId =>
          colls.path:
            _.aggregateList(nb): framework =>
              import framework.*
              Match($id(pathId)) -> List(
                Project($doc("puzzleId" -> "$ids", "_id" -> false)),
                Unwind("puzzleId"),
                Sample(nb),
                PipelineOperator:
                  $lookup.simple(
                    from = colls.puzzle,
                    local = "puzzleId",
                    foreign = "_id",
                    as = "puzzle"
                  )
                ,
                PipelineOperator:
                  $doc("$replaceWith" -> $doc("$arrayElemAt" -> $arr("$puzzle", 0)))
              )
            .map:
              _.view.flatMap(puzzleReader.readOpt).toVector
        .mon(_.puzzle.selector.user.batch(nb = nb))
