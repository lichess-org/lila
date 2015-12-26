package lila.perfStat

import play.api.libs.json._

import lila.common.LightUser
import lila.rating.PerfType

final class JsonView(getLightUser: String => Option[LightUser]) {

  def apply(ps: PerfStat) = perfStatWrites writes ps

  private implicit def perfTypeWriter: OWrites[PerfType] = OWrites { pt =>
    Json.obj("key" -> pt.key, "name" -> pt.name)
  }
  private implicit def userIdWriter: OWrites[UserId] = OWrites { u =>
    val light = getLightUser(u.value)
    Json.obj(
      "id" -> u.value,
      "name" -> light.fold(u.value)(_.name),
      "title" -> light.flatMap(_.title))
  }
  private implicit val ratingAtWrites = Json.writes[RatingAt]
  private implicit val resultWrites = Json.writes[Result]
  private implicit val streakWrites = Json.writes[Streak]
  private implicit val playStreakWrites = Json.writes[PlayStreak]
  private implicit val resultStreakWrites = Json.writes[ResultStreak]
  private implicit val avgWrites = Json.writes[Avg]
  private implicit val countWrites = Json.writes[Count]
  private implicit val perfStatWrites = Json.writes[PerfStat]
}
