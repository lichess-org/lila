package lila
package relay

final class RelayMarkdown(cache: lila.memo.MarkdownCache):

  private val options = lila.memo.MarkdownOptions(
    autoLink = true,
    list = true,
    table = true,
    header = true,
    strikeThrough = true,
    maxPgns = Max(0)
  )

  def of(tour: RelayTour): Option[Html] = tour.markup.map: md =>
    cache.toHtmlSyncWithoutPgnEmbeds(s"relay:${tour.id}", md, options)
