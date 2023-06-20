package lila.storm

import lila.common.config.MaxPerPage
import lila.common.{ Bus, LichessDay }
import lila.common.paginator.Paginator
import lila.db.dsl.*
import lila.db.paginator.Adapter
import lila.user.User
import lila.user.UserRepo
import reactivemongo.api.ReadPreference

// stores data of the best run of the day
// plus the number of runs
case class StormDay(
    _id: StormDay.Id,
    score: Int,
    moves: Int,
    errors: Int,
    combo: Int,
    time: Int,
    highest: IntRating,
    runs: Int
):

  def add(run: StormForm.RunData) = {
    if (run.score > score)
      copy(
        score = run.score,
        moves = run.moves,
        errors = run.errors,
        combo = run.combo,
        time = run.time,
        highest = run.highest
      )
    else this
  }.copy(runs = runs + 1)

  def accuracyPercent: Float = 100 * (moves - errors) / moves.toFloat

object StormDay:

  case class Id(userId: UserId, day: LichessDay)
  object Id:
    def today(userId: UserId)     = Id(userId, LichessDay.today)
    def lastWeek(userId: UserId)  = Id(userId, LichessDay daysAgo 7)
    def lastMonth(userId: UserId) = Id(userId, LichessDay daysAgo 30)
    def allTime(userId: UserId)   = Id(userId, LichessDay(0))

  def empty(id: Id) = StormDay(id, 0, 0, 0, 0, 0, IntRating(0), 0)

final class StormDayApi(coll: Coll, highApi: StormHighApi, userRepo: UserRepo, sign: StormSign)(using
    Executor
):

  import StormDay.*
  import StormBsonHandlers.given

  def addRun(
      data: StormForm.RunData,
      user: Option[User],
      mobile: Boolean
  ): Fu[Option[StormHigh.NewHigh]] =
    lila.mon.storm.run.score(user.isDefined).record(data.score).unit
    user so { u =>
      if (mobile || sign.check(u, ~data.signed))
        Bus.publish(lila.hub.actorApi.puzzle.StormRun(u.id, data.score), "stormRun")
        highApi get u.id flatMap { prevHigh =>
          val todayId = Id today u.id
          coll
            .one[StormDay]($id(todayId))
            .map {
              _.getOrElse(StormDay empty todayId) add data
            }
            .flatMap { day =>
              coll.update.one($id(day._id), day, upsert = true)
            }
            .flatMap { _ =>
              val high = highApi.update(u.id, prevHigh, data.score)
              userRepo.addStormRun(u.id, data.score) inject high
            }
        }
      else
        if (data.time > 40)
          if (data.score > 99) logger.warn(s"badly signed run from ${u.username} $data")
          lila.mon.storm.run
            .sign(data.signed match {
              case None              => "missing"
              case Some("")          => "empty"
              case Some("undefined") => "undefined"
              case _                 => "wrong"
            })
            .increment()
        fuccess(none)
    }

  def history(userId: UserId, page: Int): Fu[Paginator[StormDay]] =
    Paginator(
      adapter = new Adapter[StormDay](
        collection = coll,
        selector = idRegexFor(userId),
        projection = none,
        sort = $sort desc "_id"
      ),
      page,
      MaxPerPage(30)
    )

  def apiHistory(userId: UserId, days: Int): Fu[List[StormDay]] =
    coll
      .find(idRegexFor(userId))
      .sort($sort desc "_id")
      .cursor[StormDay](ReadPreference.secondaryPreferred)
      .list(days)

  private def idRegexFor(userId: UserId) = $doc("_id" $startsWith s"${userId}:")
