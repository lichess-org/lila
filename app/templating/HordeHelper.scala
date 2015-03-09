package lila.app
package templating

import reactivemongo.bson._
import scala.concurrent.duration._

trait HordeHelper {

  object isHordeAvailable {
    private val coll = lila.db.Env.current("flag")
    private val cache = lila.memo.MixedCache.single[Boolean](
      f = coll.find(BSONDocument("_id" -> "horde")).one[BSONDocument].map {
        _ ?? (~_.getAs[Boolean]("enabled"))
      },
      timeToLive = 1 minute,
      default = false)
    private val authorized = Set("thibault", "happy0", "chesswhiz")
    def apply(implicit ctx: lila.api.Context): Boolean =
      (ctx.userId ?? authorized.contains) || (cache get true)
  }
}
