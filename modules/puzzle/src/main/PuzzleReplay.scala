package lila.puzzle

import org.joda.time.DateTime
import reactivemongo.api.bson.BSONNull
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.chaining._

import lila.db.dsl._
import lila.memo.CacheApi
import lila.user.User

case class PuzzleReplay(days: Int, theme: PuzzleTheme.Key, nb: Int, remaining: Vector[Puzzle.Id]) {

  def i = nb - remaining.size

  def step = remaining.headOption map { _ -> copy(remaining = remaining drop 1) }
}

final class PuzzleReplayApi(
    colls: PuzzleColls,
    cacheApi: CacheApi
)(implicit ec: ExecutionContext) {

  import BsonHandlers._

  private val maxPuzzles = 100

  private val replays = cacheApi.notLoading[User.ID, PuzzleReplay](512, "puzzle.replay")(
    _.expireAfterWrite(1 hour).buildAsync()
  )

  def apply(user: User, days: Int, theme: PuzzleTheme.Key): Fu[Option[(Puzzle, PuzzleReplay)]] =
    replays.getFuture(user.id, _ => createReplayFor(user, days, theme)) flatMap { current =>
      if (current.days == days && current.theme == theme && current.remaining.nonEmpty) fuccess(current)
      else createReplayFor(user, days, theme) tap { replays.put(user.id, _) }
    } flatMap { replay =>
      replay.pp.step ?? { case (puzzleId, newReplay) =>
        replays.put(user.id, fuccess(newReplay))
        colls.puzzle(_.byId[Puzzle](puzzleId.value)) map2 (_ -> replay)
      }
    }

  private def createReplayFor(user: User, days: Int, theme: PuzzleTheme.Key): Fu[PuzzleReplay] =
    colls
      .round {
        _.aggregateOne() { framework =>
          import framework._
          Match(
            $doc(
              "u" -> user.id,
              "d" $gt DateTime.now.minusDays(days),
              "w" $ne true
            )
          ) -> List(
            Sort(Ascending("d")),
            PipelineOperator(
              $doc(
                "$lookup" -> $doc(
                  "from" -> colls.puzzle.name.value,
                  "as"   -> "puzzle",
                  "let" -> $doc(
                    "pid" -> $doc("$arrayElemAt" -> $arr($doc("$split" -> $arr("$_id", ":")), 1))
                  ),
                  "pipeline" -> $arr(
                    $doc(
                      "$match" -> $doc(
                        "$expr" -> {
                          if (theme == PuzzleTheme.mix.key) $doc("$eq" -> $arr("$_id", "$$pid"))
                          else
                            $doc(
                              "$and" -> $arr(
                                $doc("$eq" -> $arr("$_id", "$$pid")),
                                $doc("$in" -> $arr(theme, "$themes"))
                              )
                            )
                        }
                      )
                    ),
                    $doc("$limit"   -> maxPuzzles),
                    $doc("$project" -> $doc("_id" -> true))
                  )
                )
              )
            ),
            Unwind("puzzle"),
            Group(BSONNull)("ids" -> PushField("puzzle._id"))
          )
        }
      }
      .map {
        ~_.flatMap(_.getAsOpt[Vector[Puzzle.Id]]("ids"))
      } map { ids =>
      PuzzleReplay(days, theme, ids.size, ids)
    }

  private val puzzleLookup =
    $doc(
      "$lookup" -> $doc(
        "from" -> colls.puzzle.name.value,
        "as"   -> "puzzle",
        "let" -> $doc(
          "pid" -> $doc("$arrayElemAt" -> $arr($doc("$split" -> $arr("$_id", ":")), 1))
        ),
        "pipeline" -> $arr(
          $doc(
            "$match" -> $doc(
              "$expr" -> $doc(
                $doc("$eq" -> $arr("$_id", "$$pid"))
              )
            )
          ),
          $doc("$project" -> $doc("_id" -> true))
        )
      )
    )
}
