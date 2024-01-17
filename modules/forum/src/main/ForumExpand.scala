package lila.forum

import scalatags.Text.all.{ raw, Frag }

import lila.base.RawHtml
import lila.common.config

final class ForumTextExpand(askEmbed: lila.ask.AskEmbed)(using Executor, Scheduler):

  private def one(post: ForumPost)(using config.NetDomain): Fu[ForumPost.WithFrag] =
    lila.common.Bus
      .ask("lpv")(lila.hub.actorApi.lpv.LpvLinkRenderFromText(post.text, _))
      .map: linkRender =>
        raw:
          RawHtml.nl2br {
            RawHtml.addLinks(post.text, expandImg = true, linkRender = linkRender.some).value
          }.value
      .zip(askEmbed.repo.preload(post.text))
      .map: (body, _) =>
        ForumPost.WithFrag(post, body)

  def manyPosts(posts: Seq[ForumPost])(using config.NetDomain): Fu[Seq[ForumPost.WithFrag]] =
    posts.traverse(one)
