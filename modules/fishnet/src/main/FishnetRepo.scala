package lila.fishnet

import reactivemongo.api.bson._
import scala.concurrent.duration._
import org.joda.time.DateTime

import lila.db.dsl._
import lila.memo.CacheApi._

final private class FishnetRepo(
    colls: FishnetColls,
    cacheApi: lila.memo.CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._

  val analysisColl = colls.analysis
  val puzzleColl   = colls.puzzle
  val clientColl   = colls.client

  private val clientCache = cacheApi[Client.Key, Option[Client]](16, "fishnet.client") {
    _.expireAfterWrite(10 minutes)
      .buildAsyncFuture { key =>
        clientColl.one[Client](selectClient(key))
      }
  }

  def getClient(key: Client.Key)        = clientCache get key
  def getEnabledClient(key: Client.Key) = getClient(key).map { _.filter(_.enabled) }
  def getOfflineClient: Fu[Client] = getEnabledClient(Client.offline.key) getOrElse fuccess(Client.offline)
  def updateClient(client: Client): Funit =
    clientColl.update.one(selectClient(client.key), client, upsert = true).void >>-
      clientCache.invalidate(client.key)
  def updateClientInstance(client: Client, instance: Client.Instance): Fu[Client] =
    client.updateInstance(instance).fold(fuccess(client)) { updated =>
      updateClient(updated) inject updated
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

  def lishogiClients =
    clientColl.list[Client](
      $doc(
        "enabled" -> true,
        "userId" $startsWith "lishogi-"
      )
    )

  def addAnalysis(ana: Work.Analysis)    = analysisColl.insert.one(ana).void
  def getAnalysis(id: Work.Id)           = analysisColl.find(selectWork(id)).one[Work.Analysis]
  def updateAnalysis(ana: Work.Analysis) = analysisColl.update.one(selectWork(ana.id), ana).void
  def deleteAnalysis(ana: Work.Analysis) = analysisColl.delete.one(selectWork(ana.id)).void
  def giveUpAnalysis(ana: Work.Analysis) = deleteAnalysis(ana) >>- logger.warn(s"Give up on analysis $ana")
  def updateOrGiveUpAnalysis(ana: Work.Analysis) =
    if (ana.isOutOfTries) giveUpAnalysis(ana) else updateAnalysis(ana)

  def addPuzzle(puzzle: Work.Puzzle)         = puzzleColl.insert.one(puzzle).void
  def addPuzzles(puzzles: List[Work.Puzzle]) = puzzleColl.insert.many(puzzles).void
  def getPuzzle(id: Work.Id)                 = puzzleColl.find(selectWork(id)).one[Work.Puzzle]
  def countUserPuzzles(userId: String)       = puzzleColl.countSel($doc("source.user.submittedBy" -> userId))
  def updatePuzzle(puzzle: Work.Puzzle)      = puzzleColl.update.one(selectWork(puzzle.id), puzzle).void
  def deletePuzzle(puzzle: Work.Puzzle)      = puzzleColl.delete.one(selectWork(puzzle.id)).void
  def giveUpPuzzle(puzzle: Work.Puzzle) = deletePuzzle(puzzle) >>- logger.warn(s"Give up on puzzle $puzzle")
  def updateOrGiveUpPuzzle(puzzle: Work.Puzzle) =
    if (puzzle.isOutOfTries) giveUpPuzzle(puzzle) else updatePuzzle(puzzle)

  object status {
    private def system(v: Boolean)   = $doc("sender.system" -> v)
    private def acquired(v: Boolean) = $doc("acquired" $exists v)
    private def oldestSeconds(system: Boolean): Fu[Int] =
      analysisColl.ext
        .find($doc("sender.system" -> system) ++ acquired(false), $doc("createdAt" -> true))
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
        systemOldest     <- oldestSeconds(true)
        puzzleVerifiable <- puzzleColl.countSel($doc("verifiable" -> true))
        puzzleCandidates <- puzzleColl.countSel($doc("verifiable" -> false))
      } yield Monitor.Status(
        user = Monitor.StatusFor(acquired = userAcquired, queued = userQueued, oldest = userOldest),
        system = Monitor.StatusFor(acquired = systemAcquired, queued = systemQueued, oldest = systemOldest),
        puzzles = Monitor.StatusPuzzle(verifiable = puzzleVerifiable, candidates = puzzleCandidates)
      )
  }

  def getSimilarAnalysis(work: Work.Analysis): Fu[Option[Work.Analysis]] =
    analysisColl.one[Work.Analysis]($doc("game.id" -> work.game.id))

  def selectVariants(variantsKeys: List[Int]) = $doc("game.variant" $in variantsKeys)

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
