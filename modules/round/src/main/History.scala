package lila.round

import scala.concurrent.duration.Duration

import actorApi._
import akka.actor._
import reactivemongo.bson._

import lila.db.Types.Coll
import lila.game.Event
import lila.socket.actorApi.GetVersion

/**
 * NOT THREAD SAFE
 * Designed for use within a sequential actor
 */
private[round] final class History(
    load: Fu[VersionedEvents],
    persist: VersionedEvents => Unit) {

  // private var version = 0
  private var events: VersionedEvents = _

  // TODO optimize
  def getVersion: Int = {
    waitForLoadedEvents
    events.headOption.??(_.version)
  }

  // none if version asked is > to history version
  // none if an event is missing (asked too old version)
  def getEventsSince(v: Int): Option[List[VersionedEvent]] = {
    waitForLoadedEvents
    val version = getVersion
    if (v > version) None
    else if (v == version) Some(Nil)
    else events.filter(_.version > v).some
  }

  def addEvents(xs: List[Event]): VersionedEvents = {
    waitForLoadedEvents
    val vevs = xs.foldLeft(List.empty[VersionedEvent] -> getVersion) {
      case ((vevs, v), e) => (VersionedEvent(e, v + 1) :: vevs, v + 1)
    }._1
    events = (vevs ::: events) take History.size
    vevs.reverse ~ persist
  }

  private def waitForLoadedEvents {
    if (events == null) events = load.await.reverse
  }
}

private[round] object History {

  val size = 30

  def apply(coll: Coll)(gameId: String): History = new History(
    load = load(coll, gameId),
    persist = persist(coll, gameId) _)

  private def load(coll: Coll, gameId: String): Fu[VersionedEvents] =
    coll.find(BSONDocument("_id" -> gameId)).one[BSONDocument].map {
      _.flatMap(_.getAs[VersionedEvents]("e")) | Nil
    }

  private def persist(coll: Coll, gameId: String)(vevs: List[VersionedEvent]) {
    coll.uncheckedUpdate(
      BSONDocument("_id" -> gameId),
      BSONDocument("$push" -> BSONDocument(
        "e" -> BSONDocument(
          "$each" -> vevs,
          "$slice" -> -History.size))),
      upsert = true
    )
  }
}
