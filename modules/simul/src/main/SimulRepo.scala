package lila.simul

import org.joda.time.DateTime
import reactivemongo.api.bson._

import shogi.Status
import shogi.variant.Variant
import lila.db.BSON
import lila.db.dsl._
import lila.user.User

final private[simul] class SimulRepo(simulColl: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  implicit private val SimulStatusBSONHandler = tryHandler[SimulStatus](
    { case BSONInteger(v) => SimulStatus(v) toTry s"No such simul status: $v" },
    x => BSONInteger(x.id)
  )
  implicit private val ShogiStatusBSONHandler = lila.game.BSONHandlers.StatusBSONHandler
  implicit private val VariantBSONHandler = quickHandler[Variant](
    { case BSONInteger(v) => Variant.orDefault(v) },
    x => BSONInteger(x.id)
  )
  import shogi.Clock.Config
  implicit private val clockHandler         = Macros.handler[Config]
  implicit private val ClockBSONHandler     = Macros.handler[SimulClock]
  implicit private val PlayerBSONHandler    = Macros.handler[SimulPlayer]
  implicit private val ApplicantBSONHandler = Macros.handler[SimulApplicant]
  implicit private val SimulPairingBSONHandler = new BSON[SimulPairing] {
    def reads(r: BSON.Reader) =
      SimulPairing(
        player = r.get[SimulPlayer]("player"),
        gameId = r str "gameId",
        status = r.get[Status]("status"),
        wins = r boolO "wins",
        hostColor = r.strO("hostColor").flatMap(shogi.Color.fromName) | shogi.Sente
      )
    def writes(w: BSON.Writer, o: SimulPairing) =
      $doc(
        "player"    -> o.player,
        "gameId"    -> o.gameId,
        "status"    -> o.status,
        "wins"      -> o.wins,
        "hostColor" -> o.hostColor.name
      )
  }

  implicit private val SimulBSONHandler = Macros.handler[Simul]

  private val createdSelect  = $doc("status" -> SimulStatus.Created.id)
  private val startedSelect  = $doc("status" -> SimulStatus.Started.id)
  private val finishedSelect = $doc("status" -> SimulStatus.Finished.id)
  private val createdSort    = $sort desc "createdAt"

  def find(id: Simul.ID): Fu[Option[Simul]] =
    simulColl.byId[Simul](id)

  def byIds(ids: List[Simul.ID]): Fu[List[Simul]] =
    simulColl.byIds[Simul](ids)

  def exists(id: Simul.ID): Fu[Boolean] =
    simulColl.exists($id(id))

  def findStarted(id: Simul.ID): Fu[Option[Simul]] =
    find(id) map (_ filter (_.isStarted))

  def findCreated(id: Simul.ID): Fu[Option[Simul]] =
    find(id) map (_ filter (_.isCreated))

  def findPending(hostId: String): Fu[List[Simul]] =
    simulColl.list[Simul](createdSelect ++ $doc("hostId" -> hostId))

  private val featurableSelect = $doc("featurable" -> true)

  def allCreatedFeaturable: Fu[List[Simul]] =
    simulColl
      .find(
        // hits partial index hostSeenAt_-1
        createdSelect ++ featurableSelect ++ $doc(
          "hostSeenAt" $gte DateTime.now.minusSeconds(12)
        )
      )
      .sort(createdSort)
      .cursor[Simul]()
      .list(10) map {
      _.foldLeft(List.empty[Simul]) {
        case (acc, sim)
            if !acc.exists(_.hostId == sim.hostId) && (sim.popular || sim.createdAt.isAfter(
              DateTime.now.minusHours(4)
            )) =>
          sim :: acc
        case (acc, _) => acc
      }.sortBy(-_.nbAccepted)
    }

  def hostId(id: Simul.ID): Fu[Option[User.ID]] =
    simulColl.primitiveOne[User.ID]($id(id), "hostId")

  def allStarted: Fu[List[Simul]] =
    simulColl.ext
      .find(startedSelect)
      .sort(createdSort)
      .cursor[Simul]()
      .list()

  def allFinishedFeaturable(max: Int): Fu[List[Simul]] =
    simulColl.ext
      .find(finishedSelect ++ featurableSelect)
      .sort($sort desc "finishedAt")
      .cursor[Simul]()
      .list(max)

  def allNotFinished =
    simulColl.list[Simul]($doc("status" $ne SimulStatus.Finished.id))

  def create(simul: Simul, featurable: Boolean): Funit =
    simulColl.insert one {
      SimulBSONHandler.writeTry(simul).get ++ featurable.??(featurableSelect)
    } void

  def update(simul: Simul) =
    simulColl.update
      .one(
        $id(simul.id),
        $set(SimulBSONHandler writeTry simul get) ++
          simul.estimatedStartAt.isEmpty ?? ($unset("estimatedStartAt"))
      )
      .void

  def remove(simul: Simul) =
    simulColl.delete.one($id(simul.id)).void

  def setHostGameId(simul: Simul, gameId: String) =
    simulColl.update
      .one(
        $id(simul.id),
        $set("hostGameId" -> gameId)
      )
      .void

  def setHostSeenNow(simul: Simul) =
    simulColl.update
      .one(
        $id(simul.id),
        $set("hostSeenAt" -> DateTime.now)
      )
      .void

  def setText(simul: Simul, text: String) =
    simulColl.update
      .one(
        $id(simul.id),
        $set("text" -> text)
      )
      .void

  def cleanup =
    simulColl.delete.one(
      createdSelect ++ $doc(
        "createdAt" -> $doc("$lt" -> (DateTime.now minusMinutes 60))
      )
    )
}
