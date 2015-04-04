package lila.simul

import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.core.commands._

import chess.Status
import chess.variant.Variant
import lila.db.Types.Coll
import lila.game.{ Game, GameRepo }
import lila.user.{ User, UserRepo }

private[simul] final class SimulRepo(simulColl: Coll) {

  import lila.db.BSON.BSONJodaDateTimeHandler
  import reactivemongo.bson.Macros
  private implicit val SimulStatusBSONHandler = new BSONHandler[BSONInteger, SimulStatus] {
    def read(bsonInt: BSONInteger): SimulStatus = SimulStatus(bsonInt.value) err s"No such simul status: ${bsonInt.value}"
    def write(x: SimulStatus) = BSONInteger(x.id)
  }
  private implicit val ChessStatusBSONHandler = new BSONHandler[BSONInteger, Status] {
    def read(bsonInt: BSONInteger): Status = Status(bsonInt.value) err s"No such chess status: ${bsonInt.value}"
    def write(x: Status) = BSONInteger(x.id)
  }
  private implicit val VariantBSONHandler = new BSONHandler[BSONInteger, Variant] {
    def read(bsonInt: BSONInteger): Variant = Variant(bsonInt.value) err s"No such variant: ${bsonInt.value}"
    def write(x: Variant) = BSONInteger(x.id)
  }
  private implicit val ClockBSONHandler = Macros.handler[SimulClock]
  private implicit val PlayerBSONHandler = Macros.handler[SimulPlayer]
  private implicit val ApplicantBSONHandler = Macros.handler[SimulApplicant]
  private implicit val PairingBSONHandler = Macros.handler[SimulPairing]
  private implicit val SimulBSONHandler = Macros.handler[Simul]

  private val createdSelect = BSONDocument("status" -> SimulStatus.Created.id)
  private val startedSelect = BSONDocument("status" -> SimulStatus.Started.id)
  private val finishedSelect = BSONDocument("status" -> SimulStatus.Finished.id)
  private val createdSort = BSONDocument("createdAt" -> -1)

  def find(id: Simul.ID): Fu[Option[Simul]] =
    simulColl.find(BSONDocument("_id" -> id)).one[Simul]

  def exists(id: Simul.ID): Fu[Boolean] =
    simulColl.db command Count(simulColl.name, BSONDocument("_id" -> id).some) map (0 !=)

  def findStarted(id: Simul.ID): Fu[Option[Simul]] =
    find(id) map (_ filter (_.isStarted))

  def findCreated(id: Simul.ID): Fu[Option[Simul]] =
    find(id) map (_ filter (_.isCreated))

  def allCreated: Fu[List[Simul]] = simulColl.find(
    createdSelect
  ).sort(createdSort).cursor[Simul].collect[List]()

  def allStarted: Fu[List[Simul]] = simulColl.find(
    startedSelect
  ).sort(createdSort).cursor[Simul].collect[List]()

  def allFinished(max: Int): Fu[List[Simul]] = simulColl.find(
    finishedSelect
  ).sort(createdSort).cursor[Simul].collect[List](max)

  def allNotFinished =
    simulColl.find(
      BSONDocument("status" -> BSONDocument("$ne" -> SimulStatus.Finished.id))
    ).cursor[Simul].collect[List]()

  def create(simul: Simul): Funit =
    simulColl insert simul void

  def update(simul: Simul) =
    simulColl.update(BSONDocument("_id" -> simul.id), simul).void

  def cleanup =
    simulColl.remove(
      createdSelect ++ BSONDocument(
        "createdAt" -> BSONDocument("$lt" -> (DateTime.now minusMinutes 30))))
}
