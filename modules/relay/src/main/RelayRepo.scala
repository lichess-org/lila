package lila.relay

import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.core.commands._

import BSONHandlers._
import lila.db.BSON._
import lila.db.Implicits._

private final class RelayRepo(coll: Coll) {

  private def selectId(id: String) = BSONDocument("_id" -> id)
  private def selectName(name: String) = BSONDocument("name" -> name)
  private val selectStarted = BSONDocument("status" -> Relay.Status.Started.id)

  def byId(id: String): Fu[Option[Relay]] = coll.find(selectId(id)).one[Relay]

  def byName(name: String): Fu[Option[Relay]] = coll.find(selectName(name)).one[Relay]

  def started: Fu[List[Relay]] = coll.find(selectStarted).cursor[Relay].collect[List]()

  def upsert(ficsId: Int, name: String, status: Relay.Status) =
    byName(name) flatMap {
      case None        => coll insert Relay.make(ficsId, name, status)
      case Some(relay) => coll.update(selectId(relay.id), relay.copy(status = status))
    } void

  def finish(relay: Relay) = coll.update(
    selectName(relay.name),
    BSONDocument("$set" -> BSONDocument("status" -> Relay.Status.Finished.id))
  ).void

  def setGames(relay: Relay): Funit =
    coll.update(
      selectId(relay.id),
      BSONDocument("$set" -> BSONDocument("games" -> relay.games))
    ).void

  def gameByFicsId(id: String, ficsId: Int): Fu[Option[Relay.Game]] =
    byId(id) map { _ flatMap (_ gameByFicsId ficsId) }
}
