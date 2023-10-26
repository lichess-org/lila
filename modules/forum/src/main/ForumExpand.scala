package lila.forum

import scalatags.Text.all.{ raw, Frag }

import lila.base.RawHtml
import lila.common.config

final class ForumTextExpand(using Executor, Scheduler):

  private def one(text: String)(using config.NetDomain): Fu[Frag] =
    lila.common.Bus
      .ask("lpv")(lila.hub.actorApi.lpv.LpvLinkRenderFromText(text, _))
      .map: linkRender =>
        raw:
          RawHtml.nl2br {
            RawHtml.addLinks(text, expandImg = true, linkRender = linkRender.some).value
          }.value

  def manyPosts(posts: Seq[ForumPost])(using config.NetDomain): Fu[Seq[ForumPost.WithFrag]] =
    posts
      .map(_.text)
      .traverse(one)
      .map:
        _ zip posts map { (body, post) =>
          ForumPost.WithFrag(post, body)
        }
