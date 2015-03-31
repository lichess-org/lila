package lila.simul

import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.core.commands._
import scala.concurrent.duration._

import chess.Status
import chess.variant.Variant
import lila.db.Types.Coll
import lila.user.User

private[simul] final class SimulApi(
    simulColl: Coll) {

  import lila.db.BSON.BSONJodaDateTimeHandler
  import reactivemongo.bson.Macros
  private implicit val StatusBSONHandler = new BSONHandler[BSONInteger, Status] {
    def read(bsonInt: BSONInteger): Status = Status(bsonInt.value) err s"No such status: ${bsonInt.value}"
    def write(x: Status) = BSONInteger(x.id)
  }
  private implicit val VariantBSONHandler = new BSONHandler[BSONInteger, Variant] {
    def read(bsonInt: BSONInteger): Variant = Variant(bsonInt.value) err s"No such variant: ${bsonInt.value}"
    def write(x: Variant) = BSONInteger(x.id)
  }
  private implicit val ClockBSONHandler = Macros.handler[SimulClock]
  private implicit val PlayerBSONHandler = Macros.handler[SimulPlayer]
  private implicit val PairingBSONHandler = Macros.handler[SimulPairing]
  private implicit val SimulBSONHandler = Macros.handler[Simul]

  def find(id: Simul.ID): Fu[Option[Simul]] =
    simulColl.find(BSONDocument("_id" -> id)).one[Simul]

  def createSimul(setup: SimulSetup, me: User): Fu[Simul] = {
    val simul = Simul.make(
      name = setup.name,
      clock = SimulClock(setup.clockTime * 60, setup.clockIncrement),
      nbPlayers = setup.nbPlayers,
      variants = setup.variants.flatMap(chess.variant.Variant.apply),
      createdBy = me.id)
    simulColl insert simul inject simul
  }
}
