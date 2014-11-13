package lila.tournament

import scala.concurrent.duration._

private[tournament] final class Cached {

  private val nameCache = lila.memo.MixedCache[String, Option[String]](
    ((id: String) => TournamentRepo byId id map2 { (tour: Tournament) => tour.fullName }),
    timeToLive = 1 hour,
    default = _ => none)

  def name(id: String) = nameCache get id
}
