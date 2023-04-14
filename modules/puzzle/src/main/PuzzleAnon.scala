package lila.puzzle

import chess.Color
import ornicar.scalalib.ThreadLocalRandom

import lila.db.dsl.*
import lila.memo.CacheApi

final class PuzzleAnon(
    colls: PuzzleColls,
    cacheApi: CacheApi,
    pathApi: PuzzlePathApi,
    countApi: PuzzleCountApi
)(using Executor):

  import BsonHandlers.given

  def getOneFor(angle: PuzzleAngle, color: Option[Color]): Fu[Option[Puzzle]] =
    pool
      .get(angle)
      .map(color.fold[Vector[Puzzle] => Option[Puzzle]](ThreadLocalRandom.oneOf)(selectWithColor))
      .mon(_.puzzle.selector.anon.time(angle.key))
      .addEffect {
        _ foreach { puzzle =>
          lila.mon.puzzle.selector.anon.vote(angle.key).record(100 + math.round(puzzle.vote * 100))
        }
      }

  private def selectWithColor(color: Color)(puzzles: Vector[Puzzle]): Option[Puzzle] =
    def nextTry(attempts: Int): Option[Puzzle] =
      if (attempts < 10)
        ThreadLocalRandom oneOf puzzles filter (_.color == color) orElse nextTry(attempts + 1)
      else ThreadLocalRandom oneOf puzzles.filter(_.color == color)
    nextTry(1)

  def getBatchFor(angle: PuzzleAngle, nb: Int): Fu[Vector[Puzzle]] = {
    pool get angle map (_ take nb)
  }.mon(_.puzzle.selector.anon.batch(nb))

  private val poolSize = 150

  private val pool =
    cacheApi[PuzzleAngle, Vector[Puzzle]](initialCapacity = 64, name = "puzzle.byTheme.anon") {
      _.expireAfterWrite(1 minute)
        .buildAsyncFuture { angle =>
          countApi byAngle angle flatMap { count =>
            val tier =
              if (count > 5000) PuzzleTier.top
              else if (count > 2000) PuzzleTier.good
              else PuzzleTier.all
            val ratingRange: Range =
              if (count > 9000) 1300 to 1600
              else if (count > 5000) 1100 to 1800
              else 0 to 9999
            val pathSampleSize =
              if (count > 9000) 3
              else if (count > 5000) 5
              else if (count > 2000) 8
              else 15
            colls.path {
              _.aggregateList(poolSize) { framework =>
                import framework.*
                Match(pathApi.select(angle, tier, ratingRange)) -> List(
                  Sample(pathSampleSize),
                  Project($doc("puzzleId" -> "$ids", "_id" -> false)),
                  Unwind("puzzleId"),
                  Sample(poolSize),
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
                      "$replaceWith" -> $doc("$arrayElemAt" -> $arr("$puzzle", 0))
                    )
                  )
                )
              }.map {
                _.view.flatMap(puzzleReader.readOpt).toVector
              }
            }
          }
        }
    }
