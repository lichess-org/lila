package lila.simul

import scala.concurrent.duration._

private[simul] final class Cached(repo: SimulRepo) {

  private val nameCache = lila.memo.MixedCache[Simul.ID, Option[String]](
    ((id: Simul.ID) => repo find id map2 { (simul: Simul) => simul.fullName }),
    timeToLive = 6 hours,
    default = _ => none,
    logger = logger)

  def name(id: String) = nameCache get id
}
