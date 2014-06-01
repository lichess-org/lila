package lila.round

import scala.concurrent.duration.Duration

import actorApi._
import akka.actor._

import lila.game.Event
import lila.memo.{ Builder => MemoBuilder }
import lila.socket.actorApi.GetVersion

private[round] final class History(ttl: Duration) {

  private var version = 0
  private val events = MemoBuilder.expiry[Int, VersionedEvent](ttl)

  def getVersion = version

  // none if version asked is > to history version
  // none if an event is missing (asked too old version)
  def getEventsSince(v: Int): Option[List[VersionedEvent]] =
    if (v > version) None
    else if (v == version) Some(Nil)
    else ((v + 1 to version).toList map get).flatten |> { events =>
      (events.size == (version - v)) option events
    }

  def addEvents(xs: List[Event]) = xs map { e =>
    version = version + 1
    VersionedEvent(
      version = version,
      typ = e.typ,
      data = e.data,
      only = e.only,
      owner = e.owner,
      watcher = e.watcher,
      troll = e.troll) ~ { events.put(version, _) }
  }

  private def get(v: Int): Option[VersionedEvent] = Option(events getIfPresent v)
}
