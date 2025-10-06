package lila.memo

import scalalib.future.TimeoutException

import lila.common.{ Bus, Markdown, MarkdownRender, MarkdownToastUi }
import lila.core.config
import lila.core.misc.lpv.{ LpvEmbed, Lpv as LpvBus }

case class MarkdownOptions(
    autoLink: Boolean = false,
    list: Boolean = false,
    table: Boolean = false,
    header: Boolean = false,
    strikeThrough: Boolean = false,
    blockQuote: Boolean = false,
    code: Boolean = false,
    maxPgns: Max = Max(0),
    imageUpload: Boolean = false
)
object MarkdownOptions:
  val all = MarkdownOptions(
    autoLink = true,
    list = true,
    table = true,
    header = true,
    strikeThrough = true,
    blockQuote = true,
    code = true,
    maxPgns = Max(0)
  )

final class MarkdownCache(
    cacheApi: CacheApi,
    netDomain: config.NetDomain,
    assetDomain: config.AssetDomain
)(using Executor, Scheduler)(using mode: play.api.Mode):

  private val pgnCache =
    cacheApi.notLoadingSync[String, LpvEmbed](32, "memo.markdown.pgn"):
      _.expireAfterWrite(2.second).build()

  private val pgnExpand = MarkdownRender.PgnSourceExpand(netDomain, pgnCache.getIfPresent)

  private val renderMap = scala.collection.concurrent.TrieMap[MarkdownOptions, MarkdownRender]()

  private val cache = cacheApi[(String, Markdown, MarkdownOptions), Html](1024, "memo.markdown"):
    _.maximumSize(8192)
      .expireAfterWrite(if mode.isProd then 20.minutes else 1.second)
      .buildAsyncFuture: (key, markdown, opts) =>
        val processor = bodyProcessor(key, opts)
        if opts.maxPgns == Max(0) then fuccess(processor(markdown))
        else
          Bus
            .ask(LpvBus.AllPgnsFromText(markdown.value, opts.maxPgns, _), 3.seconds)
            .chronometer
            .logIfSlow(300, logger): result =>
              s"AllPgnsFromText for markdown $key - found ${result.size} embeds"
            .result
            .monSuccess(_.markdown.pgnsFromText)
            .andThen:
              case scala.util.Success(pgns) => pgnCache.putAll(pgns)
            .recoverWith:
              case TimeoutException(msg) => Future.failed(TimeoutException(msg.take(100)))
            .inject(processor(markdown))

  def toHtml(key: String, markdown: Markdown, opts: MarkdownOptions) =
    cache.get((key, markdown, opts))

  def toHtmlSync(key: String, markdown: Markdown, opts: MarkdownOptions) =
    cache
      .getIfPresent((key, markdown, opts))
      .flatMap(_.value.collect { case scala.util.Success(html) => html })
      .getOrElse:
        val processor = bodyProcessor(key, opts)
        val html = processor(markdown)
        cache.put((key, markdown, opts), fuccess(html))
        html

  private def getRenderer(opts: MarkdownOptions): MarkdownRender =
    renderMap.getOrElseUpdate(
      opts,
      MarkdownRender(
        autoLink = opts.autoLink,
        list = opts.list,
        strikeThrough = opts.strikeThrough,
        header = opts.header,
        blockQuote = opts.blockQuote,
        code = opts.code,
        table = opts.table,
        pgnExpand = pgnExpand.some,
        assetDomain.some
      )
    )

  private def bodyProcessor(key: String, opts: MarkdownOptions): Markdown => Html =
    if opts.imageUpload then toastUiProcessor(key, opts)
    else getRenderer(opts)(key)

  private def toastUiProcessor(
      key: String,
      opts: MarkdownOptions
  ): Markdown => Html =
    MarkdownToastUi.unescapeAtUsername.apply
      .andThen(getRenderer(opts)(key))
      .andThen(MarkdownToastUi.imageParagraph)
      .andThen(MarkdownToastUi.unescapeUnderscoreInLinks.apply)
