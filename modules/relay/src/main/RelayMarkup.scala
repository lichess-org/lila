package lila.relay

import scala.concurrent.duration._

import lila.common.Markdown

final class RelayMarkup {

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
    .maximumSize(256)
    .build[Markdown, String]()

  def apply(tour: RelayTour)(markup: Markdown): String = cache.get(markup, renderer(s"relay:${tour.id}"))
}
