package lila.ublog

import lila.common.{ Bus, Markdown, MarkdownRender, MarkdownToastUi }
import lila.core.config
import lila.core.misc.lpv.{ AllPgnsFromText, LpvEmbed }
import lila.memo.CacheApi

final class UblogMarkup(
    baseUrl: config.BaseUrl,
    assetBaseUrl: config.AssetBaseUrl,
    cacheApi: CacheApi,
    netDomain: config.NetDomain,
    assetDomain: config.AssetDomain
)(using Executor, Scheduler)(using mode: play.api.Mode):

  type PgnSourceId = String

  private val pgnCache =
    cacheApi.notLoadingSync[PgnSourceId, LpvEmbed](256, "ublogMarkup.pgn"):
      _.expireAfterWrite(1 second).build()

  private val renderer = MarkdownRender(
    autoLink = true,
    list = true,
    strikeThrough = true,
    header = true,
    blockQuote = true,
    code = true,
    table = true,
    pgnExpand = MarkdownRender.PgnSourceExpand(netDomain, pgnCache.getIfPresent).some,
    assetDomain.some
  )

  def apply(post: UblogPost) = cache
    .get((post.id, post.markdown))
    .map: html =>
      scalatags.Text.all.raw(html.value)

  private val cache = cacheApi[(UblogPostId, Markdown), Html](2048, "ublog.markup"):
    _.maximumSize(2048)
      .expireAfterWrite(if mode.isProd then 20 minutes else 1 second)
      .buildAsyncFuture: (id, markdown) =>
        Bus
          .ask("lpv")(AllPgnsFromText(markdown.value, _))
          .andThen { case scala.util.Success(pgns) =>
            pgnCache.putAll(pgns)
          }
          .inject(process(id)(markdown))

  private def process(id: UblogPostId): Markdown => Html = replaceGameGifs.apply
    .andThen(MarkdownToastUi.unescapeAtUsername.apply)
    .andThen(renderer(s"ublog:${id}"))
    .andThen(MarkdownToastUi.imageParagraph)
    .andThen(MarkdownToastUi.unescapeUnderscoreInLinks.apply)

  // replace game GIFs URLs with actual game URLs that can be embedded
  private object replaceGameGifs:
    private val regex = (assetBaseUrl.value + """/game/export/gif(/white|/black|)/(\w{8})\.gif""").r
    val apply         = (_: Markdown).map(regex.replaceAllIn(_, baseUrl.value + "/$2$1"))
