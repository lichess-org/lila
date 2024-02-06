package lila.cms

import play.api.Mode

import lila.common.config
import lila.common.{ Bus, LpvEmbed, Markdown, MarkdownRender }
import lila.hub.actorApi.lpv.AllPgnsFromText
import lila.memo.CacheApi
import lila.common.MarkdownToastUi

final class CmsMarkup(
    baseUrl: config.BaseUrl,
    assetBaseUrl: config.AssetBaseUrl,
    cacheApi: CacheApi,
    netDomain: config.NetDomain,
    assetDomain: config.AssetDomain
)(using Executor)(using mode: Mode):

  private val renderer = MarkdownRender(
    autoLink = true,
    list = true,
    strikeThrough = true,
    header = true,
    blockQuote = true,
    code = true,
    table = true,
    assetDomain = assetDomain.some
  )

  def apply(page: CmsPage) = cache
    .get((page.id, page.markdown))
    .map: html =>
      scalatags.Text.all.raw(html.value)

  private val cache = cacheApi[(CmsPage.Id, Markdown), Html](64, "cms.markup"):
    _.expireAfterWrite(if mode == Mode.Prod then 15 minutes else 1 second)
      .buildAsyncFuture: (id, markdown) =>
        fuccess(process(id)(markdown))

  private def process(id: CmsPage.Id): Markdown => Html =
    MarkdownToastUi.unescapeAtUsername.apply andThen
      renderer(s"cms:${id}") andThen
      MarkdownToastUi.imageParagraph andThen
      MarkdownToastUi.unescapeUnderscoreInLinks.apply

  // replace game GIFs URLs with actual game URLs that can be embedded
  private object replaceGameGifs:
    private val regex = (assetBaseUrl.value + """/game/export/gif(/white|/black|)/(\w{8})\.gif""").r
    val apply         = (_: Markdown).map(regex.replaceAllIn(_, baseUrl.value + "/$2$1"))
