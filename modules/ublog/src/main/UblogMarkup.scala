package lila.ublog

import scala.concurrent.duration._
import lila.common.Chronometer

final class UblogMarkup {

  private val renderer =
    new lila.common.Markdown(
      autoLink = true,
      list = true,
      strikeThrough = true,
      header = true,
      code = true,
      table = true
    )

  private val cache = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(20 minutes)
    .maximumSize(1024)
    .build[Int, String]()

  def apply(post: UblogPost): String =
    cache.get(post.markdown.hashCode, _ => renderer(s"ublog:${post.id}")(post.markdown))
}
