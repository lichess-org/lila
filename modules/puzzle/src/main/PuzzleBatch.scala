package lila.puzzle

import lila.db.dsl.*

// mobile app
final class PuzzleBatch(
    colls: PuzzleColls,
    anonApi: PuzzleAnon,
    pathApi: PuzzlePathApi,
    selector: PuzzleSelector
)(using Executor):

  import BsonHandlers.given

  def nextForMe(difficulty: PuzzleDifficulty, nb: Int)(using Option[Me], Perf): Fu[Vector[Puzzle]] =
    nextForMe(PuzzleAngle.mix, difficulty, nb)

  def nextForMe(
      angle: PuzzleAngle,
      difficulty: PuzzleDifficulty,
      nb: Int
  )(using me: Option[Me], perf: Perf): Fu[Vector[Puzzle]] =
    if nb < 1 then fuccess(Vector.empty)
    else if nb == 1 then selector.nextPuzzleFor(angle, none, difficulty.some).map(_.toVector)
    else
      me.foldUse(anonApi.getBatchFor(angle, difficulty, nb)): me ?=>
        val tier =
          if perf.nb > 5000 then PuzzleTier.good
          else if angle.opening.isDefined then PuzzleTier.good
          else if PuzzleDifficulty.isExtreme(difficulty) then PuzzleTier.good
          else PuzzleTier.top
        pathApi
          .nextFor("batch")(angle, tier, difficulty, Set.empty)
          .orFail(s"No puzzle path for batch ${me.username} $angle $tier")
          .flatMap: pathId =>
            colls.path:
              _.aggregateList(nb, _.sec): framework =>
                import framework.*
                Match($id(pathId)) -> List(
                  Project($doc("puzzleId" -> "$ids", "_id" -> false)),
                  Unwind("puzzleId"),
                  Sample(nb),
                  PipelineOperator:
                    $lookup.simple(
                      from = colls.puzzle.name,
                      local = "puzzleId",
                      foreign = "_id",
                      as = "puzzle",
                      pipe = Nil
                    )
                  ,
                  PipelineOperator:
                    $doc("$replaceWith" -> $doc("$arrayElemAt" -> $arr("$puzzle", 0)))
                )
              .map:
                _.view.flatMap(puzzleReader.readOpt).toVector
          .mon(_.puzzle.selector.user.batch(nb = nb))
