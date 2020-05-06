package lila.swiss

import scala.concurrent.duration._

import lila.db.dsl._
import lila.hub.LightTeam.TeamID
import lila.memo._

final private class SwissCache(
    colls: SwissColls,
    cacheApi: CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  val name = cacheApi.sync[Swiss.Id, Option[String]](
    name = "swiss.name",
    initialCapacity = 4096,
    compute = id => colls.swiss.primitiveOne[String]($id(id), "name"),
    default = _ => none,
    strategy = Syncache.WaitAfterUptime(20 millis),
    expireAfter = Syncache.ExpireAfterAccess(20 minutes)
  )

  private[swiss] val featuredInTeamCache =
    cacheApi[TeamID, List[Swiss.Id]](256, "swiss.visibleByTeam") {
      _.expireAfterAccess(30 minutes)
        .buildAsyncFuture { teamId =>
          val max = 5
          for {
            enterable <- colls.swiss.primitive[Swiss.Id](
              $doc("teamId" -> teamId, "finishedAt" $exists false),
              $sort asc "startsAt",
              nb = max,
              "_id"
            )
            finished <- colls.swiss.primitive[Swiss.Id](
              $doc("teamId" -> teamId, "finishedAt" $exists true),
              $sort desc "startsAt",
              nb = max - enterable.size,
              "_id"
            )
          } yield enterable ::: finished
        }
    }
}
