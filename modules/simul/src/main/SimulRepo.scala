package lidraughts.simul

import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.core.commands._

import draughts.Status
import draughts.variant.Variant
import lidraughts.db.BSON
import lidraughts.db.BSON.BSONJodaDateTimeHandler
import lidraughts.db.dsl._

private[simul] final class SimulRepo(simulColl: Coll) {

  val coll = simulColl

  import BSONHandlers._
  private val createdSelect = $doc("status" -> SimulStatus.Created.id)
  private val startedSelect = $doc("status" -> SimulStatus.Started.id)
  private val finishedSelect = $doc("status" -> SimulStatus.Finished.id)
  private val notFinishedSelect = $doc("status" $ne SimulStatus.Finished.id)
  private val createdSort = $doc("createdAt" -> -1)
  val uniqueSelect = $doc("spotlight" $exists true)

  def find(id: Simul.ID): Fu[Option[Simul]] =
    simulColl.byId[Simul](id)

  def byIds(ids: List[Simul.ID]): Fu[List[Simul]] =
    simulColl.byIds[Simul](ids)

  def exists(id: Simul.ID): Fu[Boolean] =
    simulColl.exists($id(id))

  def uniqueById(id: Simul.ID): Fu[Option[Simul]] =
    simulColl.find($id(id) ++ uniqueSelect).uno[Simul]

  def createdByHostId(hostId: String): Fu[List[Simul]] =
    simulColl.find(createdSelect ++ $doc("hostId" -> hostId)).list[Simul]()

  def findStarted(id: Simul.ID): Fu[Option[Simul]] =
    find(id) map (_ filter (_.isStarted))

  def findCreated(id: Simul.ID): Fu[Option[Simul]] =
    find(id) map (_ filter (_.isCreated))

  def allCreated: Fu[List[Simul]] =
    simulColl.find(createdSelect).sort(createdSort).list[Simul]()

  def allCreatedFeaturable: Fu[List[Simul]] = simulColl.find(
    createdSelect ++ $doc("spotlight" $exists false) ++ $doc("createdAt" $gte DateTime.now.minusMinutes(20))
  ).sort(createdSort).list[Simul]()

  def allUniqueFeaturable: Fu[List[Simul]] = simulColl.find(
    notFinishedSelect ++ uniqueSelect ++ $doc(
      "spotlight.startsAt" $gt DateTime.now.minusDays(1) $lt DateTime.now.plusDays(14)
    )
  ).sort($doc("spotlight.startsAt" -> 1)).list[Simul]()

  def allUniqueWithCommentaryIds: Fu[List[Simul.ID]] = simulColl.find(
    notFinishedSelect ++ uniqueSelect
  ).sort($doc("spotlight.startsAt" -> 1)).list[Simul]().map(_ filter (_.hasCeval) map (_.id))

  def allUniqueWithPublicCommentaryIds: Fu[List[Simul.ID]] = simulColl.find(
    notFinishedSelect ++ uniqueSelect
  ).sort($doc("spotlight.startsAt" -> 1)).list[Simul]().map(_ filter (_.hasPublicCeval) map (_.id))

  def allStarted: Fu[List[Simul]] = simulColl.find(
    startedSelect
  ).sort(createdSort).list[Simul]()

  def allFinished(max: Int): Fu[List[Simul]] = simulColl.find(
    finishedSelect
  ).sort(createdSort).list[Simul](max)

  def allNotFinished: Fu[List[Simul]] =
    simulColl.find(notFinishedSelect).list[Simul]()

  def uniques(max: Int): Fu[List[Simul]] =
    simulColl.find(uniqueSelect)
      .sort($doc("startsAt" -> -1))
      .list[Simul](max)

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

  def cleanup = simulColl.remove(
    createdSelect ++ $doc(
      "createdAt" -> $doc("$lt" -> (DateTime.now minusMinutes 60))
    )
  )
}
