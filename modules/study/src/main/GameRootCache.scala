package lila.study

import scala.concurrent.duration._
import com.github.blemale.scaffeine.Cache

object GameRootCache {

  private val cache: Cache[String, Node.Root] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(10 minutes)
    .maximumSize(128)
    .build[String, Node.Root]()

  // for games in progress
  private def makeKey(gm: Node.GameMainline): String =
    s"${gm.id}${gm.usis.size}#${gm.part}"

  def apply(gm: Node.GameMainline): Node.Root =
    cache.get(makeKey(gm), _ => GameToRoot(gm))

}
