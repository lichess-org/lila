package lila.forum

import scalatags.Text.all.{ Frag, raw }

import lila.common.RawHtml
import lila.core.config.NetDomain

final class ForumTextExpand(using Executor, Scheduler):

  private def one(text: String)(using NetDomain): Fu[Frag] =
    lila.common.Bus
      .ask("lpv")(lila.core.misc.lpv.LpvLinkRenderFromText(text, _))
      .map: linkRender =>
        raw:
          RawHtml.nl2br {
            RawHtml.addLinks(text, expandImg = true, linkRender = linkRender.some).value
          }.value

  def manyPosts(posts: Seq[ForumPost])(using NetDomain): Fu[Seq[ForumPost.WithFrag]] =
    posts.view
      .map(_.text)
      .toList
      .sequentially(one)
      .map:
        _.zip(posts).map: (body, post) =>
          ForumPost.WithFrag(post, body)
