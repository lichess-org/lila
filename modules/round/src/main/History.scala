package lila.round

import java.util.ArrayDeque
import scala.collection.JavaConverters._

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
    load: Fu[List[VersionedEvent]],
    persist: ArrayDeque[VersionedEvent] => Unit,
    withPersistence: Boolean
) {
  import History.Types._

  // TODO: After scala 2.13, use scala's ArrayDeque
  private[this] var events: ArrayDeque[VersionedEvent] = _

  private[this] var versionHolder: SocketVersion = _

  def getVersion: SocketVersion = {
    waitForLoadedEvents
    versionHolder
  }

  def versionDebugString: String = {
    waitForLoadedEvents
    Option(events.peekLast).fold("-:-") { l =>
      s"${events.peekFirst}:${l.version}@${l.date - nowSeconds}s"
    }
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

      // TODO: If version always increases by 1, it should be safe to slice
      // by count instead of checking each event's version.
      //
      // Ugly while loop for perf, see lila-jmh-benchmarks.
      val it = events.descendingIterator
      var filteredEvents: List[VersionedEvent] = Nil
      var e = null.asInstanceOf[VersionedEvent]
      while (it.hasNext && {
        e = it.next()
        e.version > v
      }) filteredEvents = e :: filteredEvents

      filteredEvents match {
        case e :: _ if e.version == v.inc => Events(filteredEvents)
        case _ =>
          mon.foreach(_.getEventsTooFar())
          InsufficientHistory
      }
    }
  }

  def getRecentEvents(maxEvents: Int): List[VersionedEvent] = {
    waitForLoadedEvents
    val it = events.descendingIterator
    var evs: List[VersionedEvent] = Nil
    var count = maxEvents
    while (count > 0 && it.hasNext) {
      evs = it.next() :: evs
      count -= 1
    }
    evs
  }

  def addEvents(xs: List[Event]): List[VersionedEvent] = {
    waitForLoadedEvents
    val date = nowSeconds

    removeTail(History.maxSize - xs.size)
    pruneEvents(date - History.expireAfterSeconds)
    val veBuff = List.newBuilder[VersionedEvent]
    xs.foldLeft(getVersion.inc) {
      case (vnext, e) =>
        val ve = VersionedEvent(e, vnext, date)
        events.addLast(ve)
        versionHolder = vnext
        veBuff += ve
        vnext.inc
    }
    if (persistenceEnabled) persist(events)
    veBuff.result
  }

  private def removeTail(maxSize: Int) = {
    if (maxSize <= 0) events.clear()
    else {
      var toRemove = events.size - maxSize
      while (toRemove > 0) {
        events.pollFirst
        toRemove -= 1
      }
    }
  }

  private def pruneEvents(minDate: EpochSeconds) = {
    while ({
      val e = events.peekFirst
      (e ne null) && e.date < minDate
    }) events.pollFirst
  }

  private def waitForLoadedEvents: Unit = {
    if (events == null) {
      val evs = load awaitSeconds 3
      events = new ArrayDeque[VersionedEvent]
      evs.foreach(events.add)
      versionHolder = Option(events.peekLast).fold(SocketVersion(0))(_.version)
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
    final case class Events(value: List[VersionedEvent]) extends EventResult
  }

  private final val maxSize = 25
  private final val expireAfterSeconds = 20

  def apply(coll: Coll)(gameId: String, withPersistence: Boolean): History = new History(
    load = serverStarting ?? load(coll, gameId, withPersistence),
    persist = persist(coll, gameId) _,
    withPersistence = withPersistence
  )

  private def serverStarting = !lila.common.PlayApp.startedSinceMinutes(5)

  private def load(coll: Coll, gameId: String, withPersistence: Boolean): Fu[List[VersionedEvent]] =
    coll.byId[Bdoc](gameId).map { doc =>
      ~doc.flatMap(_.getAs[List[VersionedEvent]]("e"))
    } addEffect {
      case events if events.nonEmpty && !withPersistence => coll.remove($id(gameId)).void
      case _ =>
    }

  private def persist(coll: Coll, gameId: String)(vevs: ArrayDeque[VersionedEvent]) =
    if (!vevs.isEmpty) coll.update(
      $doc("_id" -> gameId),
      $doc(
        "$set" -> $doc("e" -> vevs.toArray(Array[VersionedEvent]())),
        "$setOnInsert" -> $doc("d" -> DateTime.now)
      ),
      upsert = true,
      writeConcern = GetLastError.Unacknowledged
    )
}
