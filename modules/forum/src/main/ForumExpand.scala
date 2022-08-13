package lila.forum

import scala.concurrent.ExecutionContext
import scalatags.Text.all._

import lila.base.RawHtml
import lila.common.config
import lila.common.String.html.richText

final class ForumTextExpand(implicit ec: scala.concurrent.ExecutionContext, scheduler: akka.actor.Scheduler) {

  def one(text: String)(implicit netDomain: config.NetDomain): Fu[Frag] =
    lila.common.Bus.ask("lpv")(lila.hub.actorApi.lpv.LpvLinkRenderFromText(text, _)) map { linkRender =>
      raw {
        RawHtml.nl2br {
          RawHtml.addLinks(text, expandImg = true, linkRender = linkRender.some)
        }
      }
    }

  def many(texts: Seq[String])(implicit netDomain: config.NetDomain): Fu[Seq[Frag]] =
    texts.map(one).sequenceFu

  def manyPosts(posts: Seq[Post])(implicit netDomain: config.NetDomain): Fu[Seq[Post.WithFrag]] =
    many(posts.map(_.text)) map {
      _ zip posts map { case (body, post) =>
        Post.WithFrag(post, body)
      }
    }
}
