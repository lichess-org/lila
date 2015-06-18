package lila.tournament

import play.api.libs.json._

import lila.common.LightUser
import lila.common.PimpedJson._
import lila.game.{ Game, GameRepo }
import lila.rating.PerfType
import lila.user.User

final class ScheduleJsonView(
    getLightUser: String => Option[LightUser]) {

  def apply(tournaments: VisibleTournaments) = Json.obj(
    "created" -> tournaments.created.map(tournamentJson),
    "started" -> tournaments.started.map(tournamentJson),
    "finished" -> tournaments.finished.map(tournamentJson))

  private def tournamentJson(tour: Tournament) = Json.obj(
    "id" -> tour.id,
    "createdBy" -> tour.createdBy,
    "system" -> tour.system.toString.toLowerCase,
    "minutes" -> tour.minutes,
    "clock" -> clockJson(tour.clock),
    "position" -> tour.position.some.filterNot(_.initial).map(positionJson),
    "rated" -> tour.mode.rated,
    "fullName" -> tour.fullName,
    "nbPlayers" -> tour.nbPlayers,
    "private" -> tour.`private`,
    "variant" -> Json.obj(
      "key" -> tour.variant.key,
      "short" -> tour.variant.shortName,
      "name" -> tour.variant.name),
    "secondsToStart" -> tour.secondsToStart,
    "startsAt" -> tour.startsAt,
    "startsAtString" -> org.joda.time.format.ISODateTimeFormat.dateTime.print(tour.startsAt),
    "finishesAt" -> tour.finishesAt,
    "schedule" -> tour.schedule.map(scheduleJson),
    "winner" -> tour.winnerId.flatMap(getLightUser).map(userJson),
    "perf" -> tour.perfType.map(perfJson))

  private def scheduleJson(s: Schedule) = Json.obj(
    "freq" -> s.freq.name,
    "speed" -> s.speed.name)

  private def clockJson(c: TournamentClock) = Json.obj(
    "limit" -> c.limit,
    "increment" -> c.increment)

  private def positionJson(s: chess.StartingPosition) = Json.obj(
    "eco" -> s.eco,
    "name" -> s.name,
    "fen" -> s.fen)

  private def userJson(u: LightUser) = Json.obj(
    "id" -> u.id,
    "name" -> u.name,
    "title" -> u.title)

  private def perfJson(p: PerfType) = Json.obj(
    "icon" -> p.iconChar.toString,
    "name" -> p.name,
    "index" -> PerfType.all.indexOf(p))

}
