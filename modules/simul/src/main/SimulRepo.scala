package lila.simul

import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.core.commands._

import chess.Status
import chess.variant.Variant
import lila.db.BSON
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._

private[simul] final class SimulRepo(simulColl: Coll) {

  private implicit val SimulStatusBSONHandler = new BSONHandler[BSONInteger, SimulStatus] {
    def read(bsonInt: BSONInteger): SimulStatus = SimulStatus(bsonInt.value) err s"No such simul status: ${bsonInt.value}"
    def write(x: SimulStatus) = BSONInteger(x.id)
  }
  private implicit val ChessStatusBSONHandler = lila.game.BSONHandlers.StatusBSONHandler
  private implicit val VariantBSONHandler = new BSONHandler[BSONInteger, Variant] {
    def read(bsonInt: BSONInteger): Variant = Variant(bsonInt.value) err s"No such variant: ${bsonInt.value}"
    def write(x: Variant) = BSONInteger(x.id)
  }
  private implicit val ClockBSONHandler = {
    import chess.Clock.Config
    implicit val clockHandler = Macros.handler[Config]
    Macros.handler[SimulClock]
  }
  private implicit val PlayerBSONHandler = Macros.handler[SimulPlayer]
  private implicit val ApplicantBSONHandler = Macros.handler[SimulApplicant]
  private implicit val SimulPairingBSONHandler = new BSON[SimulPairing] {
    def reads(r: BSON.Reader) = SimulPairing(
      player = r.get[SimulPlayer]("player"),
      gameId = r str "gameId",
      status = r.get[Status]("status"),
      wins = r boolO "wins",
      hostColor = r.strO("hostColor").flatMap(chess.Color.apply) | chess.White
    )
    def writes(w: BSON.Writer, o: SimulPairing) = $doc(
      "player" -> o.player,
      "gameId" -> o.gameId,
      "status" -> o.status,
      "wins" -> o.wins,
      "hostColor" -> o.hostColor.name
    )
  }

  private implicit val SimulBSONHandler = Macros.handler[Simul]

  private val createdSelect = $doc("status" -> SimulStatus.Created.id)
  private val startedSelect = $doc("status" -> SimulStatus.Started.id)
  private val finishedSelect = $doc("status" -> SimulStatus.Finished.id)
  private val createdSort = $doc("createdAt" -> -1)

  def find(id: Simul.ID): Fu[Option[Simul]] =
    simulColl.byId[Simul](id)

  def byIds(ids: List[Simul.ID]): Fu[List[Simul]] =
    simulColl.byIds[Simul](ids)

  def exists(id: Simul.ID): Fu[Boolean] =
    simulColl.exists($id(id))

  def createdByHostId(hostId: String): Fu[List[Simul]] =
    simulColl.find(createdSelect ++ $doc("hostId" -> hostId)).list[Simul]()

  def findStarted(id: Simul.ID): Fu[Option[Simul]] =
    find(id) map (_ filter (_.isStarted))

  def findCreated(id: Simul.ID): Fu[Option[Simul]] =
    find(id) map (_ filter (_.isCreated))

  def allCreated: Fu[List[Simul]] =
    simulColl.find(createdSelect).sort(createdSort).list[Simul]()

  def allCreatedFeaturable: Fu[List[Simul]] = simulColl.find(
    createdSelect ++ $doc("createdAt" $gte DateTime.now.minusMinutes(15))
  ).sort(createdSort).list[Simul]()

  def allStarted: Fu[List[Simul]] = simulColl.find(
    startedSelect
  ).sort(createdSort).list[Simul]()

  def allFinished(max: Int): Fu[List[Simul]] = simulColl.find(
    finishedSelect
  ).sort(createdSort).list[Simul](max)

  def allNotFinished =
    simulColl.find($doc("status" $ne SimulStatus.Finished.id)).list[Simul]()

  def create(simul: Simul): Funit =
    simulColl insert simul void

  def update(simul: Simul) =
    simulColl.update($id(simul.id), simul).void

  def remove(simul: Simul) =
    simulColl.remove($id(simul.id)).void

  def setHostGameId(simul: Simul, gameId: String) = simulColl.update(
    $id(simul.id),
    $set("hostGameId" -> gameId)
  ).void

  def setHostSeenNow(simul: Simul) = simulColl.update(
    $id(simul.id),
    $set("hostSeenAt" -> DateTime.now)
  ).void

  def setText(simul: Simul, text: String) = simulColl.update(
    $id(simul.id),
    $set("text" -> text)
  ).void

  def cleanup = simulColl.remove(
    createdSelect ++ $doc(
      "createdAt" -> $doc("$lt" -> (DateTime.now minusMinutes 60))
    )
  )
}
