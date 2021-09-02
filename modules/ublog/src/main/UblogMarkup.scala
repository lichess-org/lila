package lila.ublog

import com.github.blemale.scaffeine.LoadingCache

import scala.concurrent.duration._

final class UblogMarkup {

  private val renderer =
    new lila.common.Markdown(
      autoLink = true,
      list = true,
      strikeThrough = true,
      header = true,
      code = true
    )

  private val cache: LoadingCache[String, String] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(20 minutes)
    .maximumSize(512)
    .build(renderer.apply)

  def apply(text: String): String = cache.get(text)
}
