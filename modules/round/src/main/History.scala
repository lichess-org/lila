package lila.round

import scala.concurrent.duration.Duration
import akka.actor._

import actorApi._
import lila.game.Event
import lila.memo.{ Builder ⇒ MemoBuilder }

private[round] final class History(ttl: Duration) extends Actor {

  private var version = 0
  private val events = MemoBuilder.expiry[Int, VersionedEvent](ttl)

  def receive = {

    case GetVersion ⇒ sender ! version

    // none if version asked is > to history version
    // none if an event is missing (asked too old version)
    case GetEventsSince(v: Int) ⇒ sender ! MaybeEvents(
      if (v > version) None
      else if (v == version) Some(Nil)
      else ((v + 1 to version).toList map get).flatten.some
    )

    case AddEvents(xs) ⇒ sender ! {
      xs map { e ⇒
        version = version + 1
        VersionedEvent(
          version = version,
          typ = e.typ,
          data = e.data,
          only = e.only,
          owner = e.owner,
          watcher = e.watcher) ~ { events.put(version, _) }
      }
    }
  }

  private def get(v: Int): Option[VersionedEvent] = Option(events getIfPresent v)
}
