package lila.relay

import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.core.commands._

import BSONHandlers._
import lila.db.BSON._
import lila.db.Implicits._

private final class RelayRepo(coll: Coll) {

  private def selectId(id: String) = BSONDocument("_id" -> id)
  private def selectFicsIdName(ficsId: Int, name: String) = BSONDocument(
    "ficsId" -> ficsId,
    "name" -> name)
  private val selectStarted = BSONDocument("status" -> Relay.Status.Started.id)

  def byId(id: String): Fu[Option[Relay]] = coll.find(selectId(id)).one[Relay]

  def byFicsIdName(ficsId: Int, name: String): Fu[Option[Relay]] =
    coll.find(selectFicsIdName(ficsId, name)).one[Relay]

  def started: Fu[List[Relay]] = coll.find(selectStarted).cursor[Relay].collect[List]()

  def upsert(ficsId: Int, name: String, status: Relay.Status) =
    byFicsIdName(ficsId, name) flatMap {
      case None        => coll insert Relay.make(ficsId, name, status)
      case Some(relay) => coll.update(selectId(relay.id), relay.copy(status = status))
    } void

  def setGames(relay: Relay, games: List[Relay.Game]): Funit =
    coll.update(
      selectId(relay.id),
      BSONDocument("$set" -> BSONDocument("games" -> games))
    ).void

  def gameIdByFicsId(ficsId: Int): Fu[Option[String]] = coll.find(
    selectStarted ++ BSONDocument("games.ficsId" -> ficsId)
  ).one[Relay].map {
      _.flatMap(_ gameIdByFicsId ficsId)
    }
}
