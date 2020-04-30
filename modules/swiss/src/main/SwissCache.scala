package lila.swiss

import play.api.i18n.Lang
import scala.concurrent.duration._

import lila.memo._
import lila.memo.CacheApi._
import lila.db.dsl._
import lila.user.User

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
}
