package lila.ublog

import com.github.blemale.scaffeine.Cache

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

  private val cache: Cache[Int, String] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(20 minutes)
    .maximumSize(512)
    .build()

  def apply(post: UblogPost): String = cache.get(
    post.markdown.hashCode,
    _ =>
      Chronometer
        .sync(renderer(post.markdown))
        .mon(_.ublog.markdown.time)
        .logIfSlow(100, logger)(_ => s"slow markdown ${post.id} by ${post.user} size:${post.markdown.size}")
        .result
  )
}
