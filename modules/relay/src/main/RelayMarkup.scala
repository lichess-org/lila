package lila.relay

import scala.concurrent.duration._

final class RelayMarkup {

  private val renderer =
    new lila.common.Markdown(
      autoLink = true,
      list = true,
      table = true,
      strikeThrough = true,
      header = true
    )

  private val cache = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(20 minutes)
    .maximumSize(256)
    .build[String, String]()

  def apply(tour: RelayTour)(markup: String): String = cache.get(markup, renderer(s"relay:${tour.id}"))
}
