package lila.tournament

import play.api.libs.json._

import lila.common.LightUser
import lila.rating.PerfType

final class ScheduleJsonView(lightUser: LightUser.Getter) {

  import JsonView._
  import Condition.JSONHandlers._

  def apply(tournaments: VisibleTournaments): Fu[JsObject] = for {
    created <- tournaments.created.map(tournamentJson).sequenceFu
    started <- tournaments.started.map(tournamentJson).sequenceFu
    finished <- tournaments.finished.map(tournamentJson).sequenceFu
  } yield Json.obj(
    "created" -> created,
    "started" -> started,
    "finished" -> finished
  )

  private def tournamentJson(tour: Tournament): Fu[JsObject] =
    tour.winnerId.??(lightUser).map { winner =>
      Json.obj(
        "id" -> tour.id,
        "createdBy" -> tour.createdBy,
        "system" -> tour.system.toString.toLowerCase,
        "minutes" -> tour.minutes,
        "clock" -> tour.clock,
        "position" -> tour.position.some.filterNot(_.initial).map(positionJson),
        "rated" -> tour.mode.rated,
        "fullName" -> tour.fullName,
        "nbPlayers" -> tour.nbPlayers,
        "private" -> tour.`private`,
        "variant" -> Json.obj(
          "key" -> tour.variant.key,
          "short" -> tour.variant.shortName,
          "name" -> tour.variant.name
        ),
        "secondsToStart" -> tour.secondsToStart,
        "startsAt" -> tour.startsAt,
        "finishesAt" -> tour.finishesAt,
        "status" -> tour.status.id,
        "schedule" -> tour.schedule.map(scheduleJson),
        "winner" -> winner.map(userJson),
        "conditions" -> tour.conditions.ifNonEmpty,
        "perf" -> tour.perfType.map(perfJson)
      )
    }

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
