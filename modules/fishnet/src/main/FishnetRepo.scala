package lila.fishnet

import org.joda.time.DateTime
import reactivemongo.api.bson._
import scala.concurrent.duration._

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import lila.memo.CacheApi._

final private class FishnetRepo(
    analysisColl: Coll,
    clientColl: Coll,
    cacheApi: lila.memo.CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._

  private val clientCache = cacheApi[Client.Key, Option[Client]](32, "fishnet.client") {
    _.expireAfterWrite(10 minutes)
      .buildAsyncFuture { key =>
        clientColl.one[Client](selectClient(key))
      }
  }

  def getClient(key: Client.Key)        = clientCache get key
  def getEnabledClient(key: Client.Key) = getClient(key).map { _.filter(_.enabled) }
  def getOfflineClient: Fu[Client] =
    getEnabledClient(Client.offline.key) getOrElse fuccess(Client.offline)
  def updateClientInstance(client: Client, instance: Client.Instance): Fu[Client] =
    client.updateInstance(instance).fold(fuccess(client)) { updated =>
      clientColl.update.one(selectClient(client.key), $set("instance" -> updated.instance)) >>-
        clientCache.invalidate(client.key) inject updated
    }
  def addClient(client: Client)     = clientColl.insert.one(client)
  def deleteClient(key: Client.Key) = clientColl.delete.one(selectClient(key)) >>- clientCache.invalidate(key)
  def enableClient(key: Client.Key, v: Boolean): Funit =
    clientColl.update.one(selectClient(key), $set("enabled" -> v)).void >>- clientCache.invalidate(key)
  def allRecentClients =
    clientColl.list[Client](
      $doc(
        "instance.seenAt" $gt Client.Instance.recentSince
      )
    )

  def lichessClients =
    clientColl.list[Client](
      $doc(
        "enabled" -> true,
        "userId" $startsWith "lichess-"
      )
    )

  def addAnalysis(ana: Work.Analysis)    = analysisColl.insert.one(ana).void
  def getAnalysis(id: Work.Id)           = analysisColl.find(selectWork(id)).one[Work.Analysis]
  def updateAnalysis(ana: Work.Analysis) = analysisColl.update.one(selectWork(ana.id), ana).void
  def deleteAnalysis(ana: Work.Analysis) = analysisColl.delete.one(selectWork(ana.id)).void
  def giveUpAnalysis(ana: Work.Analysis) = deleteAnalysis(ana) >>- logger.warn(s"Give up on analysis $ana")
  def updateOrGiveUpAnalysis(ana: Work.Analysis) =
    if (ana.isOutOfTries) giveUpAnalysis(ana) else updateAnalysis(ana)

  object status {
    private def system(v: Boolean)   = $doc("sender.system" -> v)
    private def acquired(v: Boolean) = $doc("acquired" $exists v)
    private def oldestSeconds(system: Boolean): Fu[Int] =
      analysisColl
        .find($doc("sender.system" -> system) ++ acquired(false), $doc("createdAt" -> true).some)
        .sort($sort asc "createdAt")
        .one[Bdoc]
        .map(~_.flatMap(_.getAsOpt[DateTime]("createdAt").map { date =>
          (nowSeconds - date.getSeconds).toInt atLeast 0
        }))

    def compute =
      for {
        all            <- analysisColl.countSel($empty)
        userAcquired   <- analysisColl.countSel(system(false) ++ acquired(true))
        userQueued     <- analysisColl.countSel(system(false) ++ acquired(false))
        userOldest     <- oldestSeconds(false)
        systemAcquired <- analysisColl.countSel(system(true) ++ acquired(true))
        systemQueued =
          all - userAcquired - userQueued - systemAcquired // because counting this is expensive (no useful index)
        systemOldest <- oldestSeconds(true)
      } yield Monitor.Status(
        user = Monitor.StatusFor(acquired = userAcquired, queued = userQueued, oldest = userOldest),
        system = Monitor.StatusFor(acquired = systemAcquired, queued = systemQueued, oldest = systemOldest)
      )
  }

  def getSimilarAnalysis(work: Work.Analysis): Fu[Option[Work.Analysis]] =
    analysisColl.one[Work.Analysis]($doc("game.id" -> work.game.id))

  def selectWork(id: Work.Id)       = $id(id.value)
  def selectClient(key: Client.Key) = $id(key.value)

  private[fishnet] def toKey(keyOrUser: String): Fu[Client.Key] =
    clientColl.primitiveOne[String](
      $or(
        "_id" $eq keyOrUser,
        "userId" $eq lila.user.User.normalize(keyOrUser)
      ),
      "_id"
    ) orFail "client not found" map Client.Key.apply
}
