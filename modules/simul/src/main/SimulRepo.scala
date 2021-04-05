package lila.simul

import org.joda.time.DateTime
import reactivemongo.api.bson._

import chess.Status
import chess.variant.Variant
import lila.db.BSON
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import lila.user.User

final private[simul] class SimulRepo(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  implicit private val SimulStatusBSONHandler = tryHandler[SimulStatus](
    { case BSONInteger(v) => SimulStatus(v) toTry s"No such simul status: $v" },
    x => BSONInteger(x.id)
  )
  implicit private val ChessStatusBSONHandler = lila.game.BSONHandlers.StatusBSONHandler
  implicit private val VariantBSONHandler = tryHandler[Variant](
    { case BSONInteger(v) => Variant(v) toTry s"No such variant: $v" },
    x => BSONInteger(x.id)
  )
  import chess.Clock.Config
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
        hostColor = r.strO("hostColor").flatMap(chess.Color.fromName) | chess.White
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
    coll.byId[Simul](id)

  def byIds(ids: List[Simul.ID]): Fu[List[Simul]] =
    coll.byIds[Simul](ids)

  def exists(id: Simul.ID): Fu[Boolean] =
    coll.exists($id(id))

  def findStarted(id: Simul.ID): Fu[Option[Simul]] =
    find(id) map (_ filter (_.isStarted))

  def findCreated(id: Simul.ID): Fu[Option[Simul]] =
    find(id) map (_ filter (_.isCreated))

  def findPending(hostId: User.ID): Fu[List[Simul]] =
    coll.list[Simul](createdSelect ++ $doc("hostId" -> hostId))

  def byTeamLeaders(teamId: String, hostIds: Seq[User.ID]): Fu[List[Simul]] =
    coll
      .find(
        createdSelect ++
          $doc("hostId" $in hostIds, "team" $in List(BSONString(teamId)))
      )
      .hint(coll hint $doc("hostId" -> 1))
      .cursor[Simul]()
      .list()

  private val featurableSelect = $doc("featurable" -> true)

  def allCreatedFeaturable: Fu[List[Simul]] =
    coll
      .find(
        // hits partial index hostSeenAt_-1
        createdSelect ++ featurableSelect ++ $doc(
          "hostSeenAt" $gte DateTime.now.minusSeconds(12),
          "createdAt" $gte DateTime.now.minusHours(1)
        )
      )
      .sort(createdSort)
      .hint(coll hint $doc("hostSeenAt" -> -1))
      .cursor[Simul]()
      .list() map {
      _.foldLeft(List.empty[Simul]) {
        case (acc, sim) if acc.exists(_.hostId == sim.hostId) => acc
        case (acc, sim)                                       => sim :: acc
      }.reverse
    }

  def allStarted: Fu[List[Simul]] =
    coll
      .find(startedSelect)
      .sort(createdSort)
      .cursor[Simul]()
      .list()

  def allFinishedFeaturable(max: Int): Fu[List[Simul]] =
    coll
      .find(finishedSelect ++ featurableSelect)
      .sort($sort desc "finishedAt")
      .cursor[Simul]()
      .list(max)

  def allNotFinished =
    coll.list[Simul]($doc("status" $ne SimulStatus.Finished.id))

  def create(simul: Simul): Funit =
    coll.insert one {
      SimulBSONHandler.writeTry(simul).get
    } void

  def update(simul: Simul) =
    coll.update
      .one(
        $id(simul.id),
        $set(SimulBSONHandler writeTry simul get)
      )
      .void

  def remove(simul: Simul) =
    coll.delete.one($id(simul.id)).void

  def setHostGameId(simul: Simul, gameId: String) =
    coll.update
      .one(
        $id(simul.id),
        $set("hostGameId" -> gameId)
      )
      .void

  def setHostSeenNow(simul: Simul) =
    coll.update
      .one(
        $id(simul.id),
        $set("hostSeenAt" -> DateTime.now)
      )
      .void

  def setText(simul: Simul, text: String) =
    coll.update
      .one(
        $id(simul.id),
        $set("text" -> text)
      )
      .void

  def cleanup =
    coll.delete.one(
      createdSelect ++ $doc(
        "createdAt" -> $doc("$lt" -> (DateTime.now minusMinutes 60))
      )
    )
}
