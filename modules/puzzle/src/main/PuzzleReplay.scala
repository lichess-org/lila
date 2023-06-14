package lila.puzzle

import reactivemongo.api.bson.BSONNull
import scala.util.chaining.*

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.user.User

case class PuzzleReplay(
    days: PuzzleDashboard.Days,
    theme: PuzzleTheme.Key,
    nb: Int,
    remaining: Vector[PuzzleId]
):

  def i = nb - remaining.size

  def step = copy(remaining = remaining drop 1)

final class PuzzleReplayApi(
    colls: PuzzleColls,
    cacheApi: CacheApi
)(using Executor):

  import BsonHandlers.given

  private val maxPuzzles = 100

  private val replays = cacheApi.notLoading[UserId, PuzzleReplay](512, "puzzle.replay")(
    _.expireAfterWrite(1 hour).buildAsync()
  )

  def apply(
      user: User,
      maybeDays: Option[PuzzleDashboard.Days],
      theme: PuzzleTheme.Key
  ): Fu[Option[(Puzzle, PuzzleReplay)]] =
    maybeDays map { days =>
      replays.getFuture(user.id, _ => createReplayFor(user, days, theme)) flatMap { current =>
        if (current.days == days && current.theme == theme && current.remaining.nonEmpty) fuccess(current)
        else createReplayFor(user, days, theme) tap { replays.put(user.id, _) }
      } flatMap { replay =>
        replay.remaining.headOption so { id =>
          colls.puzzle(_.byId[Puzzle](id)) map2 (_ -> replay)
        }
      }
    } getOrElse fuccess(None)

  def onComplete(round: PuzzleRound, days: PuzzleDashboard.Days, angle: PuzzleAngle): Funit =
    angle.asTheme so { theme =>
      replays.getIfPresent(round.userId) so {
        _ map { replay =>
          if (replay.days == days && replay.theme == theme)
            replays.put(round.userId, fuccess(replay.step))
        }
      }
    }

  private def createReplayFor(
      user: User,
      days: PuzzleDashboard.Days,
      theme: PuzzleTheme.Key
  ): Fu[PuzzleReplay] =
    colls
      .round {
        _.aggregateOne() { framework =>
          import framework.*
          Match(
            $doc(
              "u" -> user.id,
              "d" $gt nowInstant.minusDays(days),
              "w" $ne true
            )
          ) -> List(
            Sort(Ascending("d")),
            PipelineOperator(
              $lookup.pipelineFull(
                from = colls.puzzle.name.value,
                as = "puzzle",
                let = $doc("pid" -> $doc("$arrayElemAt" -> $arr($doc("$split" -> $arr("$_id", ":")), 1))),
                pipe = List(
                  $doc(
                    "$match" -> $doc(
                      $expr {
                        if (theme == PuzzleTheme.mix.key) $doc("$eq" -> $arr("$_id", "$$pid"))
                        else
                          $doc(
                            $and(
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
            ),
            Unwind("puzzle"),
            Group(BSONNull)("ids" -> PushField("puzzle._id"))
          )
        }
      }
      .map {
        ~_.flatMap(_.getAsOpt[Vector[PuzzleId]]("ids"))
      } map { ids =>
      PuzzleReplay(days, theme, ids.size, ids)
    }
