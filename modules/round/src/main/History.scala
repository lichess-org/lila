package lila.round

import org.joda.time.DateTime
import reactivemongo.api.commands.GetLastError
import reactivemongo.bson._

import lila.db.dsl._
import lila.game.Event
import lila.socket.Socket.SocketVersion
import VersionedEvent.EpochSeconds

/**
 * NOT THREAD SAFE
 * Designed for use within a sequential actor (or a Duct)
 */
private final class History(
    load: Fu[VersionedEvents],
    persist: VersionedEvents => Unit,
    withPersistence: Boolean
) {

  private var events: VersionedEvents = _

  def getVersion: SocketVersion = {
    waitForLoadedEvents
    events.headOption.fold(SocketVersion(0))(_.version)
  }

  // none if version asked is > history version
  // none if an event is missing (asked too old version)
  def getEventsSince(v: SocketVersion, mon: Option[lila.mon.round.history.PlatformHistory]): Option[List[VersionedEvent]] = {
    val version = getVersion
    if (v > version) None
    else if (v == version) Some(Nil)
    else {
      val delta = version.value - v.value
      mon.foreach(_ getEventsDelta delta)
      val result = events.takeWhile(_.version > v).reverse.some filter {
        _.headOption.fold(true)(_.version == v.inc)
      }
      mon.ifTrue(result.isEmpty).foreach(_.getEventsTooFar())
      result
    }
  }

  /* if v+1 refers to an old event,
   * then the client probably has skipped events somehow.
   * Log and send new events.
   * None => client is too late, or has greater version than server. Resync.
   * Some(List.empty) => all is good, do nothing
   * Some(List.nonEmpty) => late client, send new events
   *
   * We check the event age because if the client sends a
   * versionCheck ping while the server sends an event,
   * we can get a false positive.
   * */
  def versionCheck(v: SocketVersion): Option[List[VersionedEvent]] =
    getEventsSince(v, none) map { evs =>
      if (evs.headOption.exists(_ hasSeconds 10)) evs else Nil
    }

  def getRecentEvents(maxEvents: Int): List[VersionedEvent] = {
    waitForLoadedEvents
    events.take(maxEvents).reverse
  }

  def addEvents(xs: List[Event]): VersionedEvents = {
    waitForLoadedEvents
    val date = nowSeconds
    val vevs = xs.foldLeft(List.empty[VersionedEvent] -> getVersion) {
      case ((vevs, v), e) => (VersionedEvent(e, v.inc, date) :: vevs, v.inc)
    }._1
    events = slideEvents(vevs, events, date)
    if (persistenceEnabled) persist(events)
    vevs.reverse
  }

  private def slideEvents(newEvents: VersionedEvents, history: VersionedEvents, date: EpochSeconds): VersionedEvents = {
    val expiration: EpochSeconds = date - History.expireAfterSeconds
    var nb = events.size
    newEvents ::: history.takeWhile { e =>
      nb += 1
      nb <= History.maxSize && e.date > expiration
    }
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

private object History {

  private val maxSize = 25
  private val expireAfterSeconds = 20

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
