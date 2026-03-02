package lila.forum

import scalatags.Text.all.{ Frag, raw }

import lila.common.RawHtml
import lila.core.config.NetDomain

final class ForumTextExpand(markdown: lila.memo.MarkdownCache)(using Executor, Scheduler):

  val markdownOptions = lila.memo.MarkdownOptions(
    autoLink = true,
    list = true,
    table = true,
    header = false,
    strikeThrough = true,
    blockQuote = true,
    code = true,
    timestamp = false,
    maxPgns = lila.memo.Max(10)
  )

  private def one(post: ForumPost)(using NetDomain): Fu[Frag] =
    if post.hasMarkdown then
      markdown.toHtml(s"forum:${post.id}", Markdown(post.text), markdownOptions).map(_.frag)
    else
      lila.common.Bus
        .ask(lila.core.misc.lpv.Lpv.LinkRenderFromText(post.text, _))
        .map: linkRender =>
          raw:
            RawHtml.nl2br {
              RawHtml.addLinks(post.text, expandImg = true, linkRender = linkRender.some).value
            }.value

  def manyPosts(posts: Seq[ForumPost])(using NetDomain): Fu[Seq[ForumPost.WithFrag]] =
    posts.view.toList
      .sequentially(one)
      .map:
        _.zip(posts).map: (body, post) =>
          ForumPost.WithFrag(post, body)
