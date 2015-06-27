package lila.relay

import scala.concurrent.duration._

private[relay] final class Cached(repo: RelayRepo) {

  private val nameCache = lila.memo.MixedCache[String, Option[String]](
    ((id: String) => repo byId id map2 { (relay: Relay) => relay.name }),
    timeToLive = 6 hours,
    default = _ => none)

  def name(id: String) = nameCache get id

  private val miniStartedCache = lila.memo.AsyncCache.single[List[Relay.Mini]](
    repo.startedNonEmpty map2 Relay.Mini.apply,
    // repo.recent(3) map2 Relay.Mini.apply,
    timeToLive = 10 seconds)

  def miniStarted = miniStartedCache(true)
}
