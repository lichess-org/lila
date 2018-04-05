package lila.fishnet

import reactivemongo.bson._
import scala.concurrent.duration._

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._

private final class FishnetRepo(
    analysisColl: Coll,
    clientColl: Coll,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  import BSONHandlers._

  private val clientCache = asyncCache.clearable[Client.Key, Option[Client]](
    name = "fishnet.client",
    f = key => clientColl.find(selectClient(key)).uno[Client],
    expireAfter = _.ExpireAfterWrite(5 minutes)
  )

  def getClient(key: Client.Key) = clientCache get key
  def getEnabledClient(key: Client.Key) = getClient(key).map { _.filter(_.enabled) }
  def getOfflineClient: Fu[Client] = getEnabledClient(Client.offline.key) getOrElse fuccess(Client.offline)
  def updateClient(client: Client): Funit =
    clientColl.update(selectClient(client.key), client, upsert = true).void >>- clientCache.invalidate(client.key)
  def updateClientInstance(client: Client, instance: Client.Instance): Fu[Client] =
    client.updateInstance(instance).fold(fuccess(client)) { updated =>
      updateClient(updated) inject updated
    }
  def addClient(client: Client) = clientColl.insert(client)
  def deleteClient(key: Client.Key) = clientColl.remove(selectClient(key)) >>- clientCache.invalidate(key)
  def enableClient(key: Client.Key, v: Boolean): Funit =
    clientColl.update(selectClient(key), $set("enabled" -> v)).void >>- clientCache.invalidate(key)
  def allRecentClients = clientColl.find($doc(
    "instance.seenAt" $gt Client.Instance.recentSince
  )).cursor[Client]().gather[List]()
  def lichessClients = clientColl.find($doc(
    "enabled" -> true,
    "userId" $startsWith "lichess-"
  )).cursor[Client]().gather[List]()

  def addAnalysis(ana: Work.Analysis) = analysisColl.insert(ana).void
  def getAnalysis(id: Work.Id) = analysisColl.find(selectWork(id)).uno[Work.Analysis]
  def updateAnalysis(ana: Work.Analysis) = analysisColl.update(selectWork(ana.id), ana).void
  def deleteAnalysis(ana: Work.Analysis) = analysisColl.remove(selectWork(ana.id)).void
  def giveUpAnalysis(ana: Work.Analysis) = deleteAnalysis(ana) >>- logger.warn(s"Give up on analysis $ana")
  def updateOrGiveUpAnalysis(ana: Work.Analysis) = if (ana.isOutOfTries) giveUpAnalysis(ana) else updateAnalysis(ana)
  def countAnalysis(acquired: Boolean) = analysisColl.count($doc(
    "acquired" $exists acquired
  ).some)
  def countUserAnalysis = analysisColl.count($doc(
    "sender.system" -> false
  ).some)
  def getAnalysisByGameId(gameId: String) = analysisColl.find($doc(
    "game.id" -> gameId
  )).uno[Work.Analysis]

  def getSimilarAnalysis(work: Work.Analysis): Fu[Option[Work.Analysis]] =
    analysisColl.find($doc("game.id" -> work.game.id)).uno[Work.Analysis]

  def selectWork(id: Work.Id) = $id(id.value)
  def selectClient(key: Client.Key) = $id(key.value)

  private[fishnet] def toKey(keyOrUser: String): Fu[Client.Key] =
    clientColl.primitiveOne[String]($or(
      "_id" $eq keyOrUser,
      "userId" $eq keyOrUser
    ), "_id") flatten "client not found" map Client.Key.apply
}
