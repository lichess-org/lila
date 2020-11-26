package views.html.base

import com.github.blemale.scaffeine.LoadingCache
import scala.concurrent.duration._
import lila.app.ui.ScalatagsTemplate._

object markdown {

  private val renderer = new lila.common.Markdown

  private val cache: LoadingCache[String, String] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterWrite(10 minutes)
    .maximumSize(256)
    .build(renderer.apply)

  def apply(text: String): Frag = raw(cache get text)
}
