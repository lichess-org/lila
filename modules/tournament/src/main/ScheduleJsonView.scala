package lila.tournament

import play.api.libs.json._

import lila.common.LightUser
import lila.rating.PerfType

final class ScheduleJsonView(lightUser: LightUser.Getter) {

  import JsonView._

  def apply(tournaments: VisibleTournaments): Fu[JsObject] = for {
    created <- tournaments.created.map(fullJson).sequenceFu
    started <- tournaments.started.map(fullJson).sequenceFu
    finished <- tournaments.finished.map(fullJson).sequenceFu
  } yield Json.obj(
    "created" -> created,
    "started" -> started,
    "finished" -> finished
  )

  def featured(tournaments: List[Tournament]): Fu[JsObject] =
    tournaments.map(fullJson).sequenceFu map { objs =>
      Json.obj("featured" -> objs)
    }

  def calendar(tournaments: List[Tournament]): JsObject = Json.obj(
    "since" -> tournaments.headOption.map(_.startsAt.withTimeAtStartOfDay),
    "to" -> tournaments.lastOption.map(_.finishesAt.withTimeAtStartOfDay plusDays 1),
    "tournaments" -> JsArray(tournaments.map(baseJson))
  )

  private def baseJson(tour: Tournament): JsObject = Json.obj(
    "id" -> tour.id,
    "createdBy" -> tour.createdBy,
    "system" -> tour.system.toString.toLowerCase,
    "minutes" -> tour.minutes,
    "clock" -> tour.clock,
    "rated" -> tour.mode.rated,
    "fullName" -> tour.fullName,
    "nbPlayers" -> tour.nbPlayers,
    "variant" -> Json.obj(
      "key" -> tour.variant.key,
      "short" -> tour.variant.shortName,
      "name" -> tour.variant.name
    ),
    "secondsToStart" -> tour.secondsToStart,
    "startsAt" -> tour.startsAt,
    "finishesAt" -> tour.finishesAt,
    "status" -> tour.status.id,
    "perf" -> tour.perfType.map(perfJson)
  ).add("hasMaxRating", tour.conditions.maxRating.isDefined)
    .add("private", tour.isPrivate)
    .add("position", tour.position.some.filterNot(_.initial) map positionJson)
    .add("schedule", tour.schedule map scheduleJson)

  private def fullJson(tour: Tournament): Fu[JsObject] = for {
    owner <- tour.nonLichessCreatedBy ?? lightUser
    winner <- tour.winnerId ?? lightUser
  } yield baseJson(tour) ++ Json.obj(
    "winner" -> winner.map(userJson)
  ).add("major", owner.exists(_.title.isDefined))

  private def userJson(u: LightUser) = Json.obj(
    "id" -> u.id,
    "name" -> u.name,
    "title" -> u.title
  )

  private val perfPositions: Map[PerfType, Int] = {
    import PerfType._
    List(Bullet, Blitz, Rapid, UltraBullet) ::: variants
  }.zipWithIndex.toMap

  private def perfJson(p: PerfType) = Json.obj(
    "icon" -> p.iconChar.toString,
    "key" -> p.key,
    "name" -> p.name,
    "position" -> ~perfPositions.get(p)
  )

}
