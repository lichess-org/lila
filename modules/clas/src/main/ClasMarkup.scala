package lila.clas

final class ClasMarkup(using Executor):

  private val renderer = lila.common.MarkdownRender(
    autoLink = true,
    list = true,
    table = true,
    strikeThrough = true,
    header = true
  )

  private val cache = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(20.minutes)
    .maximumSize(512)
    .build[Markdown, Html]()

  def apply(clas: Clas): Html = cache.get(clas.wall, renderer(s"clas:${clas.id}"))
