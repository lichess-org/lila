package lila.round

import scala.concurrent.duration.Duration

import lila.game.Event
import lila.memo.{ Builder ⇒ MemoBuilder }

final class History(ttl: Duration) {

  private var privateVersion = 0
  private val events = MemoBuilder.expiry[Int, VersionedEvent](ttl)

  def version = privateVersion

  // none if version asked is > to history version
  // none if an event is missing (asked too old version)
  def since(v: Int): Option[List[VersionedEvent]] = version |> { u ⇒
    if (v > u) None
    else if (v == u) Some(Nil)
    else ((v + 1 to u).toList map get).flatten.some
  }

  private def get(v: Int): Option[VersionedEvent] = Option(events getIfPresent v)

  def +=(event: Event): VersionedEvent = {
    privateVersion = version + 1
    VersionedEvent(
      version = version,
      typ = event.typ,
      data = event.data,
      only = event.only,
      owner = event.owner,
      watcher = event.watcher) ~ { events.put(version, _) }
  }
}
