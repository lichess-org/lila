package lila
package clas

final class ClasMarkdown(cache: lila.memo.MarkdownCache):

  private val options = lila.memo.MarkdownOptions(
    autoLink = true,
    list = true,
    table = true,
    header = true,
    strikeThrough = true
  )

  def wallHtml(clas: Clas) = cache.toHtml(s"clas:${clas.id}", clas.wall, options)
