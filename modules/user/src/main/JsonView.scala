package lila.user

import lila.rating.{ Perf, Glicko }
import play.api.libs.json.Json

final class JsonView {

  private implicit val countWrites = Json.writes[Count]
  private implicit val glickoWrite = Json.writes[Glicko]
  private implicit val perfWrite = Json.writes[Perf]
  private implicit val perfsWrites = Json.writes[Perfs]

  def me(u: User) = user(u)

  def user(u: User) = Json.obj(
    "id" -> u.id,
    "username" -> u.username,
    "title" -> u.title,
    "rating" -> u.rating,
    "rd" -> u.perfs.standard.glicko.deviation,
    "progress" -> u.progress,
    "playTime" -> u.playTime.map { p =>
      Json.obj(
        "total" -> p.total,
        "tv" -> p.tv)
    })

  def full(u: User) = user(u) ++ Json.obj(
    "count" -> countWrites.writes(u.count),
    "perfs" -> perfsWrites.writes(u.perfs)
  )
}
