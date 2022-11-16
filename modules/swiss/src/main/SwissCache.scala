package lila.swiss

import org.joda.time.DateTime
import scala.concurrent.duration.*

import lila.db.dsl.{ *, given }
import lila.hub.LightTeam.TeamID
import lila.memo.*
import lila.memo.CacheApi.*

final class SwissCache(
    mongo: SwissMongo,
    cacheApi: CacheApi
)(using ec: scala.concurrent.ExecutionContext):

  import BsonHandlers.given

  object swissCache:

    private val cache = cacheApi[Swiss.Id, Option[Swiss]](512, "swiss.swiss") {
      _.expireAfterWrite(1 second)
        .buildAsyncFuture(id => mongo.swiss.byId[Swiss](id.value))
    }
    def clear(id: Swiss.Id) = cache invalidate id

    def byId                          = cache.get
    def notFinishedById(id: Swiss.Id) = byId(id).dmap(_.filter(_.isNotFinished))
    def createdById(id: Swiss.Id)     = byId(id).dmap(_.filter(_.isCreated))
    def startedById(id: Swiss.Id)     = byId(id).dmap(_.filter(_.isStarted))

  val name = cacheApi.sync[Swiss.Id, Option[String]](
    name = "swiss.name",
    initialCapacity = 4096,
    compute = id => mongo.swiss.primitiveOne[String]($id(id), "name"),
    default = _ => none,
    strategy = Syncache.WaitAfterUptime(20 millis),
    expireAfter = Syncache.ExpireAfterAccess(20 minutes)
  )

  val roundInfo = cacheApi[Swiss.Id, Option[Swiss.RoundInfo]](32, "swiss.roundInfo") {
    _.expireAfterWrite(1 minute)
      .buildAsyncFuture { id =>
        mongo.swiss.byId[Swiss](id.value).map2(_.roundInfo)
      }
  }

  private[swiss] object featuredInTeam:
    private val compute = (teamId: TeamID) => {
      val max = 5
      for {
        enterable <- mongo.swiss.primitive[Swiss.Id](
          $doc("teamId" -> teamId, "finishedAt" $exists false),
          $sort asc "startsAt",
          nb = max,
          "_id"
        )
        finished <- mongo.swiss.primitive[Swiss.Id](
          $doc("teamId" -> teamId, "finishedAt" $exists true),
          $sort desc "startsAt",
          nb = max - enterable.size,
          "_id"
        )
      } yield enterable ::: finished
    }
    private val cache = cacheApi[TeamID, List[Swiss.Id]](256, "swiss.visibleByTeam") {
      _.expireAfterAccess(30 minutes)
        .buildAsyncFuture(compute)
    }

    def get(teamId: TeamID)        = cache get teamId
    def invalidate(teamId: TeamID) = cache.put(teamId, compute(teamId))
