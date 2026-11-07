package lila.feed

import com.github.blemale.scaffeine.LoadingCache

import lila.core.config.NetDomain
import lila.core.data.{ Html, Markdown }
import lila.core.lilaism.Lilaism.*

final class FeedMarkdown(using NetDomain, Executor):

  def apply(feed: Feed.Update): Html = cache.get(feed.id -> feed.content)

  private val renderer = lila.markdown.MarkdownRender(
    autoLink = false,
    strikeThrough = true,
    timestamp = true,
    linkRel = ""
  )

  private val cache: LoadingCache[(Feed.ID, Markdown), Html] =
    scalalib.cache.scaffeineNoScheduler
      .expireAfterWrite(1.day)
      .build((id, md) => renderer(s"dailyFeed:${id}")(md))
