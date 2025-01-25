package lila.clas

import scala.concurrent.duration._

import com.github.blemale.scaffeine.LoadingCache

final class ClasMarkup {

  private val renderer = new lila.common.Markdown(autoLink = true)

  private val cache: LoadingCache[String, String] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(20 minutes)
    .maximumSize(256)
    .build(renderer.apply)

  def apply(text: String): String = cache.get(text)
}
