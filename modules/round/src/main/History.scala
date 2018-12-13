package lila.round

import org.joda.time.DateTime
import reactivemongo.api.commands.GetLastError
import reactivemongo.bson._

import lila.db.dsl._
import lila.game.Event
import lila.socket.Socket.SocketVersion

/**
 * NOT THREAD SAFE
 * Designed for use within a sequential actor (or a Duct)
 */
private[round] final class History(
    load: Fu[VersionedEvents],
    persist: VersionedEvents => Unit,
    withPersistence: Boolean
) {

  private var events: VersionedEvents = _

  def getVersion: SocketVersion = {
    waitForLoadedEvents
    events.headOption.fold(SocketVersion(0))(_.version)
  }

  // none if version asked is > to history version
  // none if an event is missing (asked too old version)
  def getEventsSince(v: SocketVersion): Option[List[VersionedEvent]] = {
    waitForLoadedEvents
    val version = getVersion
    if (v > version) None
    else if (v == version) Some(Nil)
    else {
      val delta = version.value - v.value
      lila.mon.round.history.getEventsDelta(delta)
      if (delta > History.size) lila.mon.round.history.getEventsTooFar()
      events.takeWhile(_.version > v).reverse.some filter {
        _.headOption.fold(true)(_.version == v.inc)
      }
    }
  }

  def getRecentEvents(maxEvents: Int): List[VersionedEvent] = {
    waitForLoadedEvents
    events.take(maxEvents).reverse
  }

  def addEvents(xs: List[Event]): VersionedEvents = {
    waitForLoadedEvents
    val vevs = xs.foldLeft(List.empty[VersionedEvent] -> getVersion) {
      case ((vevs, v), e) => (VersionedEvent(e, v.inc) :: vevs, v.inc)
    }._1
    events = vevs ::: events.take(History.size - vevs.size)
    if (persistenceEnabled) persist(events)
    vevs.reverse
  }

  private def waitForLoadedEvents: Unit = {
    if (events == null) {
      events = load awaitSeconds 3
    }
  }

  private var persistenceEnabled = withPersistence

  def enablePersistence: Unit = {
    if (!persistenceEnabled) {
      persistenceEnabled = true
      if (events != null) persist(events)
    }
  }
}

private[round] object History {

  val size = 20

  def apply(coll: Coll)(gameId: String, withPersistence: Boolean): History = new History(
    load = serverStarting ?? load(coll, gameId, withPersistence),
    persist = persist(coll, gameId) _,
    withPersistence = withPersistence
  )

  private def serverStarting = !lila.common.PlayApp.startedSinceMinutes(5)

  private def load(coll: Coll, gameId: String, withPersistence: Boolean): Fu[VersionedEvents] =
    coll.byId[Bdoc](gameId).map {
      _.flatMap(_.getAs[VersionedEvents]("e")) ?? (_.reverse)
    } addEffect {
      case events if events.nonEmpty && !withPersistence => coll.remove($id(gameId)).void
      case _ =>
    }

  private def persist(coll: Coll, gameId: String)(vevs: List[VersionedEvent]) =
    if (vevs.nonEmpty) coll.update(
      $doc("_id" -> gameId),
      $doc(
        "$set" -> $doc("e" -> vevs.reverse),
        "$setOnInsert" -> $doc("d" -> DateTime.now)
      ),
      upsert = true,
      writeConcern = GetLastError.Unacknowledged
    )
}
