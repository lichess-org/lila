package lila.simul

import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.core.commands._

import chess.Status
import chess.variant.Variant
import lila.db.BSON
import lila.db.dsl.Coll
import lila.game.{ Game, GameRepo }
import lila.user.{ User, UserRepo }

private[simul] final class SimulRepo(simulColl: Coll) {

  import lila.db.BSON.BSONJodaDateTimeHandler
  import reactivemongo.bson.Macros
  private implicit val SimulStatusBSONHandler = new BSONHandler[BSONInteger, SimulStatus] {
    def read(bsonInt: BSONInteger): SimulStatus = SimulStatus(bsonInt.value) err s"No such simul status: ${bsonInt.value}"
    def write(x: SimulStatus) = BSONInteger(x.id)
  }
  private implicit val ChessStatusBSONHandler = lila.game.BSONHandlers.StatusBSONHandler
  private implicit val VariantBSONHandler = new BSONHandler[BSONInteger, Variant] {
    def read(bsonInt: BSONInteger): Variant = Variant(bsonInt.value) err s"No such variant: ${bsonInt.value}"
    def write(x: Variant) = BSONInteger(x.id)
  }
  private implicit val ClockBSONHandler = Macros.handler[SimulClock]
  private implicit val PlayerBSONHandler = Macros.handler[SimulPlayer]
  private implicit val ApplicantBSONHandler = Macros.handler[SimulApplicant]
  private implicit val SimulPairingBSONHandler = new BSON[SimulPairing] {
    def reads(r: BSON.Reader) = SimulPairing(
      player = r.get[SimulPlayer]("player"),
      gameId = r str "gameId",
      status = r.get[Status]("status"),
      wins = r boolO "wins",
      hostColor = r.strO("hostColor").flatMap(chess.Color.apply) | chess.White)
    def writes(w: BSON.Writer, o: SimulPairing) = BSONDocument(
      "player" -> o.player,
      "gameId" -> o.gameId,
      "status" -> o.status,
      "wins" -> o.wins,
      "hostColor" -> o.hostColor.name)
  }

  private implicit val SimulBSONHandler = Macros.handler[Simul]

  private val createdSelect = BSONDocument("status" -> SimulStatus.Created.id)
  private val startedSelect = BSONDocument("status" -> SimulStatus.Started.id)
  private val finishedSelect = BSONDocument("status" -> SimulStatus.Finished.id)
  private val createdSort = BSONDocument("createdAt" -> -1)

  def find(id: Simul.ID): Fu[Option[Simul]] =
    simulColl.find(BSONDocument("_id" -> id)).one[Simul]

  def exists(id: Simul.ID): Fu[Boolean] =
    simulColl.count(BSONDocument("_id" -> id).some) map (0 !=)

  def createdByHostId(hostId: String): Fu[List[Simul]] =
    simulColl.find(createdSelect ++ BSONDocument("hostId" -> hostId))
      .cursor[Simul]().collect[List]()

  def findStarted(id: Simul.ID): Fu[Option[Simul]] =
    find(id) map (_ filter (_.isStarted))

  def findCreated(id: Simul.ID): Fu[Option[Simul]] =
    find(id) map (_ filter (_.isCreated))

  def allCreated: Fu[List[Simul]] = simulColl.find(
    createdSelect
  ).sort(createdSort).cursor[Simul]().collect[List]()

  def allCreatedFeaturable: Fu[List[Simul]] = simulColl.find(
    createdSelect ++ BSONDocument(
      "createdAt" -> BSONDocument("$gte" -> DateTime.now.minusMinutes(15)),
      "hostRating" -> BSONDocument("$gte" -> 1700)
    )
  ).sort(createdSort).cursor[Simul]().collect[List]()

  def allStarted: Fu[List[Simul]] = simulColl.find(
    startedSelect
  ).sort(createdSort).cursor[Simul]().collect[List]()

  def allFinished(max: Int): Fu[List[Simul]] = simulColl.find(
    finishedSelect
  ).sort(createdSort).cursor[Simul]().collect[List](max)

  def allNotFinished =
    simulColl.find(
      BSONDocument("status" -> BSONDocument("$ne" -> SimulStatus.Finished.id))
    ).cursor[Simul]().collect[List]()

  def create(simul: Simul): Funit =
    simulColl insert simul void

  def update(simul: Simul) =
    simulColl.update(BSONDocument("_id" -> simul.id), simul).void

  def remove(simul: Simul) =
    simulColl.remove(BSONDocument("_id" -> simul.id)).void

  def setHostGameId(simul: Simul, gameId: String) = simulColl.update(
    BSONDocument("_id" -> simul.id),
    BSONDocument("$set" -> BSONDocument("hostGameId" -> gameId))
  ).void

  def setHostSeenNow(simul: Simul) = simulColl.update(
    BSONDocument("_id" -> simul.id),
    BSONDocument("$set" -> BSONDocument("hostSeenAt" -> DateTime.now))
  ).void

  def cleanup = simulColl.remove(
    createdSelect ++ BSONDocument(
      "createdAt" -> BSONDocument("$lt" -> (DateTime.now minusMinutes 60))))
}
