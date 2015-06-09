package lila.tournament

import scala.concurrent.duration._

private[tournament] final class Cached {

  private val nameCache = lila.memo.MixedCache[String, Option[String]](
    ((id: String) => TournamentRepo byId id map2 { (tour: Tournament) => tour.fullName }),
    timeToLive = 6 hours,
    default = _ => none)

  def name(id: String): Option[String] = nameCache get id

  private val staleViewCache = lila.memo.AsyncCache[String, Option[Tournament]](
    (id: String) => TournamentRepo byId id,
    timeToLive = 2 seconds)

  def tour(id: String): Fu[Option[Tournament]] = staleViewCache(id)
}
