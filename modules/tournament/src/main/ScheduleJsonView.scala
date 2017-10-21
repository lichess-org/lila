package lila.tournament

import play.api.libs.json._

import lila.common.LightUser
import lila.rating.PerfType

final class ScheduleJsonView(lightUser: LightUser.Getter) {

  import JsonView._

  def apply(tournaments: VisibleTournaments): Fu[JsObject] = for {
    created <- tournaments.created.map(tournamentJson).sequenceFu
    started <- tournaments.started.map(tournamentJson).sequenceFu
    finished <- tournaments.finished.map(tournamentJson).sequenceFu
  } yield Json.obj(
    "created" -> created,
    "started" -> started,
    "finished" -> finished
  )

  private def tournamentJson(tour: Tournament): Fu[JsObject] = for {
    owner <- tour.nonLichessCreatedBy.??(lightUser)
    winner <- tour.winnerId.??(lightUser)
  } yield Json.obj(
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
    "winner" -> winner.map(userJson),
    "perf" -> tour.perfType.map(perfJson)
  ).add("hasMaxRating", tour.conditions.maxRating.isDefined)
    .add("major", owner.exists(_.title.isDefined))
    .add("private", tour.`private`)
    .add("position", tour.position.some.filterNot(_.initial) map positionJson)
    .add("schedule", tour.schedule map scheduleJson)

  private def userJson(u: LightUser) = Json.obj(
    "id" -> u.id,
    "name" -> u.name,
    "title" -> u.title
  )

  private val perfPositions: Map[PerfType, Int] = {
    import PerfType._
    List(Bullet, Blitz, Classical, UltraBullet) ::: variants
  }.zipWithIndex.toMap

  private def perfJson(p: PerfType) = Json.obj(
    "icon" -> p.iconChar.toString,
    "key" -> p.key,
    "name" -> p.name,
    "position" -> ~perfPositions.get(p)
  )

}
