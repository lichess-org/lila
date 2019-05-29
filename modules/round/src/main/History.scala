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
  import History.Types._

  private var events: VersionedEvents = _

  def getVersion: SocketVersion = {
    waitForLoadedEvents
    events.headOption.fold(SocketVersion(0))(_.version)
  }

  def versionDebugString: String = {
    s"${events.lastOption.fold(-1)(_.version.value)}:${
      events.headOption.fold(-1)(_.version.value)
    }"
  }

  def getEventsSince(v: SocketVersion, mon: Option[lila.mon.round.history.PlatformHistory]): EventResult = {
    val version = getVersion
    if (v > version) VersionTooHigh
    else if (v == version) UpToDate
    else {
      mon.foreach { m =>
        m.getEventsDelta(version.value - v.value)
        m.getEventsCount()
      }
      val filteredEvents = events.takeWhile(_.version > v).reverse
      filteredEvents match {
        case e :: _ if e.version == v.inc => Events(filteredEvents)
        case _ =>
          mon.foreach(_.getEventsTooFar())
          InsufficientHistory
      }
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
  def versionCheck(v: SocketVersion): Option[VersionedEvents] =
    getEventsSince(v, none) match {
      case Events(evs) if evs.headOption.exists(_ hasSeconds 10) => Some(evs)
      case Events(_) | UpToDate => Some(Nil)
      case _ => None
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
  object Types {
    sealed trait EventResult
    object VersionTooHigh extends EventResult
    object UpToDate extends EventResult
    object InsufficientHistory extends EventResult
    final case class Events(value: VersionedEvents) extends EventResult
  }

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
