package lila.relay

import lila.db.BSON
import lila.db.BSON.BSONJodaDateTimeHandler
import reactivemongo.bson._

object BSONHandlers {

  private implicit val StatusBSONHandler = new BSONHandler[BSONInteger, Relay.Status] {
    def read(bsonInt: BSONInteger): Relay.Status = Relay.Status(bsonInt.value) err s"No such status: ${bsonInt.value}"
    def write(x: Relay.Status) = BSONInteger(x.id)
  }
  import Relay.Game
  implicit val RelayGameBSONHandler = Macros.handler[Game]
  implicit val RelayGamesBSONHandler = new BSON.ListHandler[Game]

  implicit val RelayBSONHandler = new BSON[Relay] {
    def reads(r: BSON.Reader) = Relay(
      id = r str "_id",
      ficsId = r int "ficsId",
      name = r str "name",
      status = r.get[Relay.Status]("status"),
      date = r date "date",
      games = r.get[List[Relay.Game]]("games"))
    def writes(w: BSON.Writer, o: Relay) = BSONDocument(
      "_id" -> o.id,
      "ficsId" -> o.ficsId,
      "name" -> o.name,
      "status" -> o.status,
      "date" -> w.date(o.date),
      "games" -> o.games)
  }

  implicit val ContentBSONHandler = Macros.handler[Content]
}
