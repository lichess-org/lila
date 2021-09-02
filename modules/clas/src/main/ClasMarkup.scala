package lila.clas

import scala.concurrent.duration._

final class ClasMarkup {

  private val renderer =
    new lila.common.Markdown(
      autoLink = true,
      list = true,
      strikeThrough = true,
      header = true
    )

  private val cache = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(20 minutes)
    .maximumSize(512)
    .build[Int, String]()

  def apply(clas: Clas): String =
    cache.get(clas.wall.hashCode, _ => renderer(s"clas:${clas.id}")(clas.wall))
}
