package lila.fishnet

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi.*

final private class FishnetRepo(
    analysisColl: Coll,
    clientColl: Coll,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  import BSONHandlers.given

  private val clientCache = cacheApi[Client.Key, Option[Client]](256, "fishnet.client"):
    _.expireAfterWrite(20.minutes).buildAsyncFuture: key =>
      clientColl.one[Client]($id(key))

  def getEnabledClient(key: Client.Key) = clientCache.get(key).dmap { _.filter(_.enabled) }
  def getOfflineClient: Fu[Client] =
    getEnabledClient(Client.offline.key).getOrElse(fuccess(Client.offline))
  def updateClientInstance(client: Client, instance: Client.Instance): Fu[Client] =
    client
      .updateInstance(instance)
      .fold(fuccess(client)): updated =>
        for
          _ <- clientColl.update.one($id(client.key), $set("instance" -> updated.instance))
          _ = clientCache.invalidate(client.key)
        yield updated
  def addClient(client: Client) = clientColl.insert.one(client)
  def deleteClient(key: Client.Key) = for _ <- clientColl.delete.one($id(key))
  yield clientCache.invalidate(key)
  def enableClient(key: Client.Key, v: Boolean): Funit =
    for _ <- clientColl.update.one($id(key), $set("enabled" -> v)) yield clientCache.invalidate(key)
  def allRecentClients =
    clientColl.list[Client]:
      $doc:
        "instance.seenAt".$gt(Client.Instance.recentSince)

  def addAnalysis(ana: Work.Analysis) = analysisColl.insert.one(ana).void
  def getAnalysis(id: Work.Id) = analysisColl.byId[Work.Analysis](id)
  def updateAnalysis(ana: Work.Analysis) = analysisColl.update.one($id(ana.id), ana).void
  def deleteAnalysis(ana: Work.Analysis) = analysisColl.delete.one($id(ana.id)).void
  def updateOrGiveUpAnalysis(ana: Work.Analysis, update: Work.Analysis => Work.Analysis) =
    if ana.isOutOfTries then
      logger.warn(s"Give up on analysis $ana")
      deleteAnalysis(ana)
    else updateAnalysis(update(ana))

  object status:
    private def system(v: Boolean) = $doc("sender.system" -> v)
    private def acquired(v: Boolean) = $doc("acquired".$exists(v))
    private def oldestSeconds(system: Boolean): Fu[Int] =
      analysisColl
        .find($doc("sender.system" -> system) ++ acquired(false), $doc("createdAt" -> true).some)
        .sort($sort.asc("createdAt"))
        .one[Bdoc]
        .map(~_.flatMap(_.getAsOpt[Instant]("createdAt").map { date =>
          (nowSeconds - date.toSeconds).toInt.atLeast(0)
        }))

    def compute = for
      all <- analysisColl.countSel($empty)
      userAcquired <- analysisColl.countSel(system(false) ++ acquired(true))
      userQueued <- analysisColl.countSel(system(false) ++ acquired(false))
      userOldest <- oldestSeconds(false)
      systemAcquired <- analysisColl.countSel(system(true) ++ acquired(true))
      // because counting this is expensive (no useful index)
      systemQueued = all - userAcquired - userQueued - systemAcquired
      systemOldest <- oldestSeconds(true)
    yield Monitor.Status(
      user = Monitor.StatusFor(acquired = userAcquired, queued = userQueued, oldest = userOldest),
      system = Monitor.StatusFor(acquired = systemAcquired, queued = systemQueued, oldest = systemOldest)
    )

  def getSimilarAnalysis(work: Work.Analysis): Fu[Option[Work.Analysis]] =
    analysisColl.one[Work.Analysis]($doc("game.id" -> work.game.id))

  private[fishnet] def toKey(keyOrUser: String): Fu[Client.Key] =
    clientColl
      .primitiveOne[String](
        $or(
          "_id".$eq(keyOrUser),
          "userId".$eq(UserStr(keyOrUser).id)
        ),
        "_id"
      )
      .orFail("client not found")
      .map { Client.Key(_) }
