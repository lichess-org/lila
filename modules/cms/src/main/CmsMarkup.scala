package lila.cms

import lila.common.{ Markdown, MarkdownRender, MarkdownToastUi }
import lila.core.id.CmsPageId
import lila.memo.CacheApi

final class CmsMarkup(cacheApi: CacheApi)(using Executor, play.api.Mode):

  private val renderer = MarkdownRender(
    autoLink = true,
    list = true,
    strikeThrough = true,
    header = true,
    blockQuote = true,
    code = true,
    table = true
  )

  def apply(page: CmsPage): Fu[Html] = cache.get((page.id, page.markdown))

  private val cache = cacheApi[(CmsPageId, Markdown), Html](64, "cms.markup"):
    _.expireAfterWrite(15.minutes)
      .buildAsyncFuture: (id, markdown) =>
        fuccess(process(id)(markdown))

  private def process(id: CmsPageId): Markdown => Html =
    MarkdownToastUi.unescapeAtUsername.apply
      .andThen(renderer(s"cms:$id"))
      .andThen(MarkdownToastUi.imageParagraph)
      .andThen(MarkdownToastUi.unescapeUnderscoreInLinks.apply)
