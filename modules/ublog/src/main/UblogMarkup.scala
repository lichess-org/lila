package lila.ublog

import lila.common.{ Bus, Markdown, MarkdownRender, MarkdownToastUi }
import lila.core.config
import lila.core.misc.lpv.{ LpvEmbed, Lpv as LpvBus }
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
    cacheApi.notLoadingSync[PgnSourceId, LpvEmbed](32, "ublogMarkup.pgn"):
      _.expireAfterWrite(2.second).build()

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
    .get((post.id, post.markdown, maxPgn(post)))
    .map: html =>
      scalatags.Text.all.raw(html.value)

  private def maxPgn(post: UblogPost) = Max(if post.isLichess then 25 else 20)

  private val cache = cacheApi[(UblogPostId, Markdown, Max), Html](1024, "ublog.markup"):
    _.maximumSize(2048)
      .expireAfterWrite(if mode.isProd then 20.minutes else 1.second)
      .buildAsyncFuture: (id, markdown, max) =>
        Bus
          .ask(LpvBus.AllPgnsFromText(markdown.value, max, _))
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
    val apply: Update[Markdown] = _.map(regex.replaceAllIn(_, baseUrl.value + "/$2$1"))
