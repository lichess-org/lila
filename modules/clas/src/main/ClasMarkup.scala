package lila.clas

import scala.concurrent.duration._

final class ClasMarkup {

  private val renderer =
    new lila.common.MarkdownRender(
      autoLink = true,
      list = true,
      table = true,
      strikeThrough = true,
      header = true
    )

  private val cache = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(20 minutes)
    .maximumSize(512)
    .build[lila.common.Markdown, String]()

  def apply(clas: Clas): String = cache.get(clas.wall, renderer(s"clas:${clas.id}"))
}
