package lila.forum

import scalatags.Text.all.raw

import lila.common.RawHtml
import lila.core.config.NetDomain

final class ForumTextExpand(markdown: lila.memo.MarkdownCache, askApi: lila.core.ask.AskApi)(using
    Executor,
    Scheduler
):

  val markdownOptions = lila.memo.MarkdownOptions(
    autoLink = true,
    list = true,
    table = true,
    header = true,
    strikeThrough = true,
    blockQuote = true,
    code = true,
    timestamp = true,
    sourceMap = true,
    removeHtmlEntities = true,
    maxPgns = lila.memo.Max(10)
  )

  private def one(post: ForumPost)(using NetDomain): Fu[ForumPost.WithFrag] =
    if post.hasMarkdown then
      for
        html <- markdown.toHtml(s"forum:${post.id}", Markdown(post.text), markdownOptions)
        editable <- askApi.decode(post.text)
      yield ForumPost.WithFrag(post.copy(text = editable), html.frag)
    else
      lila.common.Bus
        .ask(lila.core.misc.lpv.Lpv.LinkRenderFromText(post.text, _))
        .map: linkRender =>
          raw:
            RawHtml.nl2br {
              RawHtml.addLinks(post.text, expandImg = true, linkRender = linkRender.some).value
            }.value
        .flatMap: body =>
          askApi
            .decode(post.text)
            .map: editable =>
              ForumPost.WithFrag(post.copy(text = editable), body)

  def manyPosts(posts: Seq[ForumPost])(using NetDomain): Fu[Seq[ForumPost.WithFrag]] =
    posts.traverse(one)

  def preview(text: String): Html =
    markdown.toHtmlSyncWithoutPgnEmbeds("forum:preview", Markdown(text), markdownOptions)
