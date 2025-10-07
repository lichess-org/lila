package lila
package event

final class EventMarkdown(cache: lila.memo.MarkdownCache):

  private val options = lila.memo.MarkdownOptions(
    autoLink = true,
    list = true,
    table = true,
    header = false,
    strikeThrough = true
  )

  def of(event: Event): Fu[Option[Html]] = event.description.so: md =>
    cache.toHtml(s"event:${event.id}", md, options).dmap(some)
