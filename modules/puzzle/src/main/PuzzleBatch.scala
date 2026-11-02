package lila.puzzle

import lila.db.dsl.*
import lila.mon.extensions.*

// mobile app
final class PuzzleBatch(
    colls: PuzzleColls,
    anonApi: PuzzleAnon,
    pathApi: PuzzlePathApi,
    selector: PuzzleSelector
)(using Executor):

  import BsonHandlers.given

  def nextForMe(difficulty: PuzzleDifficulty, nb: Int)(using Option[Me], Perf): Fu[Vector[Puzzle]] =
    nextForMe(PuzzleAngle.mix, PuzzleSettings(difficulty, none), nb)

  def nextForMe(
      angle: PuzzleAngle,
      settings: PuzzleSettings,
      nb: Int
  )(using me: Option[Me], perf: Perf): Fu[Vector[Puzzle]] =
    import settings.*
    if nb < 1 then fuccess(Vector.empty)
    else if nb == 1 then selector.nextPuzzleFor(angle, color.map(some), difficulty.some).map(_.toVector)
    else
      me.foldUse(anonApi.getBatchFor(angle, difficulty, nb, color)): me ?=>
        val tier =
          if perf.nb > 5000 then PuzzleTier.good
          else if angle.opening.isDefined then PuzzleTier.good
          else if PuzzleDifficulty.isExtreme(difficulty) then PuzzleTier.good
          else PuzzleTier.top
        pathApi
          .nextFor("batch")(angle, tier, difficulty, Set.empty)
          .flatMapz: pathId =>
            samplePath(pathId, if color.isDefined then nb * 3 else nb)
              .map: puzzles =>
                color
                  .fold(puzzles): c =>
                    puzzles.filter(_.color == c)
                  .take(nb)
              .mon(lila.mon.puzzle.selector.user.batch(nb = nb))

  private def samplePath(pathId: PuzzlePath.Id, sampleSize: Int): Fu[Vector[Puzzle]] =
    colls
      .path:
        _.aggregateList(sampleSize): framework =>
          import framework.*
          Match(bid(pathId)) -> List(
            Project(bdoc("puzzleId" -> "$ids", "_id" -> false)),
            Unwind("puzzleId"),
            Sample(sampleSize),
            PipelineOperator:
              lookup.simple(
                from = colls.puzzle.name,
                local = "puzzleId",
                foreign = "_id",
                as = "puzzle",
                pipe = Nil
              )
            ,
            PipelineOperator:
              bdoc("$replaceWith" -> bdoc("$arrayElemAt" -> barr("$puzzle", 0)))
          )
      .map:
        _.view.flatMap(puzzleReader.readOpt).toVector
