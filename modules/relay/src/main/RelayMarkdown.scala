package lila
package relay

final class RelayMarkdown(cache: lila.memo.MarkdownCache):

  private val options = lila.memo.MarkdownOptions(
    autoLink = true,
    list = true,
    table = true,
    header = true,
    strikeThrough = true
  )

  def of(tour: RelayTour): Fu[Option[Html]] = tour.markup.so: md =>
    cache.toHtml(s"relay:${tour.id}", md, options).dmap(some)

  def sync(tour: RelayTour, md: Markdown): Html =
    cache.toHtmlSync(s"relay:${tour.id}", md, options)
