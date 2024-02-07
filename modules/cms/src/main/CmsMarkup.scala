package lila.cms

import lila.common.{ config, MarkdownToastUi, Bus, LpvEmbed, Markdown, MarkdownRender }
import lila.hub.actorApi.lpv.AllPgnsFromText
import lila.memo.CacheApi

final class CmsMarkup(
    baseUrl: config.BaseUrl,
    assetBaseUrl: config.AssetBaseUrl,
    cacheApi: CacheApi
)(using Executor, play.api.Mode):

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

  private val cache = cacheApi[(CmsPage.Id, Markdown), Html](64, "cms.markup"):
    _.expireAfterWrite(15 minutes)
      .buildAsyncFuture: (id, markdown) =>
        fuccess(process(id)(markdown).pp(markdown))

  private def process(id: CmsPage.Id): Markdown => Html =
    MarkdownToastUi.unescapeAtUsername.apply andThen
      renderer(s"cms:$id") andThen
      MarkdownToastUi.imageParagraph andThen
      MarkdownToastUi.unescapeUnderscoreInLinks.apply
