package lila.swiss

import org.joda.time.DateTime
import scala.concurrent.duration.*

import lila.db.dsl.{ *, given }
import lila.memo.*
import lila.memo.CacheApi.*

final class SwissCache(
    mongo: SwissMongo,
    cacheApi: CacheApi
)(using ec: scala.concurrent.ExecutionContext):

  import BsonHandlers.given

  object swissCache:

    private val cache = cacheApi[SwissId, Option[Swiss]](512, "swiss.swiss") {
      _.expireAfterWrite(1 second)
        .buildAsyncFuture(id => mongo.swiss.byId[Swiss](id))
    }
    def clear(id: SwissId) = cache invalidate id

    def byId                         = cache.get
    def notFinishedById(id: SwissId) = byId(id).dmap(_.filter(_.isNotFinished))
    def createdById(id: SwissId)     = byId(id).dmap(_.filter(_.isCreated))
    def startedById(id: SwissId)     = byId(id).dmap(_.filter(_.isStarted))

  val name = cacheApi.sync[SwissId, Option[String]](
    name = "swiss.name",
    initialCapacity = 4096,
    compute = id => mongo.swiss.primitiveOne[String]($id(id), "name"),
    default = _ => none,
    strategy = Syncache.Strategy.WaitAfterUptime(20 millis),
    expireAfter = Syncache.ExpireAfter.Access(20 minutes)
  )

  val roundInfo = cacheApi[SwissId, Option[Swiss.RoundInfo]](32, "swiss.roundInfo") {
    _.expireAfterWrite(1 minute)
      .buildAsyncFuture { id =>
        mongo.swiss.byId[Swiss](id).map2(_.roundInfo)
      }
  }

  private[swiss] object featuredInTeam:
    private val compute = (teamId: TeamId) => {
      val max = 5
      for {
        enterable <- mongo.swiss.primitive[SwissId](
          $doc("teamId" -> teamId, "finishedAt" $exists false),
          $sort asc "startsAt",
          nb = max,
          "_id"
        )
        finished <- mongo.swiss.primitive[SwissId](
          $doc("teamId" -> teamId, "finishedAt" $exists true),
          $sort desc "startsAt",
          nb = max - enterable.size,
          "_id"
        )
      } yield enterable ::: finished
    }
    private val cache = cacheApi[TeamId, List[SwissId]](256, "swiss.visibleByTeam") {
      _.expireAfterAccess(30 minutes)
        .buildAsyncFuture(compute)
    }

    def get(teamId: TeamId)        = cache get teamId
    def invalidate(teamId: TeamId) = cache.put(teamId, compute(teamId))
