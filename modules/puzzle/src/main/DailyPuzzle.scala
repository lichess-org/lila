package lila.puzzle

import akka.pattern.ask
import org.joda.time.DateTime
import Puzzle.{ BSONFields => F }
import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.CacheApi._

final private[puzzle] class DailyPuzzle(
    colls: PuzzleColls,
    pathApi: PuzzlePathApi,
    renderer: lila.hub.actors.Renderer,
    cacheApi: lila.memo.CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  private val cache =
    cacheApi.unit[Option[DailyPuzzle.WithHtml]] {
      _.refreshAfterWrite(1 minutes)
        .buildAsyncFuture(_ => find)
    }

  def get: Fu[Option[DailyPuzzle.WithHtml]] = cache.getUnit

  private def find: Fu[Option[DailyPuzzle.WithHtml]] =
    (findCurrent orElse findNew) recover { case e: Exception =>
      logger.error("find daily", e)
      none
    } flatMap { _ ?? makeDaily }

  private def makeDaily(puzzle: Puzzle): Fu[Option[DailyPuzzle.WithHtml]] = {
    import makeTimeout.short
    renderer.actor ? DailyPuzzle.Render(puzzle, puzzle.fenAfterInitialMove, puzzle.line.head.uci) map {
      case html: String => DailyPuzzle.WithHtml(puzzle, html).some
    }
  } recover { case e: Exception =>
    logger.warn("make daily", e)
    none
  }

  private def findCurrent =
    colls.puzzle {
      _.find($doc(F.day $gt DateTime.now.minusDays(1)))
        .one[Puzzle]
    }

  private def findNew: Fu[Option[Puzzle]] =
    colls
      .path {
        _.aggregateOne() { framework =>
          import framework._
          Match(pathApi.select(PuzzleTheme.mix.key, PuzzleTier.Top, 2000 to 2100)) -> List(
            Sample(3),
            Project($doc("ids" -> true, "_id" -> false)),
            UnwindField("ids"),
            PipelineOperator(
              $doc(
                "$lookup" -> $doc(
                  "from" -> colls.puzzle.name.value,
                  "as"   -> "puzzle",
                  "let"  -> $doc("id" -> "$ids"),
                  "pipeline" -> $arr(
                    $doc(
                      "$match" -> $doc(
                        "$expr" -> $doc(
                          $doc("$eq" -> $arr("$_id", "$$id"))
                        )
                      )
                    ),
                    $doc(
                      "$match" -> $doc(
                        Puzzle.BSONFields.day $exists false,
                        Puzzle.BSONFields.themes $ne PuzzleTheme.oneMove.key.value
                      )
                    )
                  )
                )
              )
            ),
            UnwindField("puzzle"),
            ReplaceRootField("puzzle"),
            AddFields($doc("dayScore" -> $doc("$multiply" -> $arr("$plays", "$vote")))),
            Sort(Descending("dayScore")),
            Limit(1)
          )
        }
      }
      .flatMap { docOpt =>
        docOpt.flatMap(PuzzleBSONReader.readOpt) ?? { puzzle =>
          colls.puzzle {
            _.update.one($id(puzzle.id), $set(F.day -> DateTime.now))
          } inject puzzle.some
        }
      }
}

object DailyPuzzle {
  type Try = () => Fu[Option[DailyPuzzle.WithHtml]]

  case class WithHtml(puzzle: Puzzle, html: String)

  case class Render(puzzle: Puzzle, fen: chess.format.FEN, lastMove: String)
}
