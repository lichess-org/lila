package lila.puzzle

import reactivemongo.api.bson.BSONNull

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import scalalib.model.Days

case class PuzzleReplay(
    days: Days,
    theme: PuzzleTheme.Key,
    nb: Int,
    remaining: Vector[PuzzleId]
):

  def i = nb - remaining.size

  def step = copy(remaining = remaining.drop(1))

final class PuzzleReplayApi(
    colls: PuzzleColls,
    cacheApi: CacheApi
)(using Executor):

  import BsonHandlers.given

  private val maxPuzzles = 100

  private val replays = cacheApi.notLoading[UserId, PuzzleReplay](512, "puzzle.replay"):
    _.expireAfterWrite(1.hour).buildAsync()

  def apply(
      maybeDays: Option[Days],
      theme: PuzzleTheme.Key
  )(using me: Me): Fu[Option[(Puzzle, PuzzleReplay)]] =
    maybeDays.so: days =>
      for
        current <- replays.getFuture(me.userId, _ => createReplayFor(me, days, theme))
        replay <-
          if current.days == days && current.theme == theme && current.remaining.nonEmpty
          then fuccess(current)
          else createReplayFor(me, days, theme).tap { replays.put(me.userId, _) }
        puzzle <- replay.remaining.headOption.so: id =>
          colls.puzzle(_.byId[Puzzle](id))
      yield puzzle.map(_ -> replay)

  def onComplete(round: PuzzleRound, days: Days, angle: PuzzleAngle): Funit =
    angle.asTheme.so: theme =>
      replays
        .getIfPresent(round.userId)
        .so:
          _.map: replay =>
            if replay.days == days && replay.theme == theme then
              replays.put(round.userId, fuccess(replay.step))

  private def createReplayFor(user: User, days: Days, theme: PuzzleTheme.Key): Fu[PuzzleReplay] =
    colls
      .round:
        _.aggregateOne(): framework =>
          import framework.*
          Match(
            $doc(
              "u" -> user.id,
              "d".$gt(nowInstant.minusDays(days.value)),
              "w".$ne(true)
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
                      $expr:
                        if theme == PuzzleTheme.mix.key then $doc("$eq" -> $arr("$_id", "$$pid"))
                        else
                          $doc:
                            $and(
                              $doc("$eq" -> $arr("$_id", "$$pid")),
                              $doc("$in" -> $arr(theme, "$themes"))
                            )
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
      .map:
        ~_.flatMap(_.getAsOpt[Vector[PuzzleId]]("ids"))
      .map: ids =>
        PuzzleReplay(days, theme, ids.size, ids)
