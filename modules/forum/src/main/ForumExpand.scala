package lila.forum

import scala.concurrent.ExecutionContext
import scalatags.Text.all._

import lila.common.String.html.richText
import lila.game.GameTextExpand
import lila.base.RawHtml

final class ForumTextExpand(gameExpand: GameTextExpand)(implicit
    ec: ExecutionContext
) {

  def one(text: String): Fu[Frag] = gameExpand.fromText(text) map { linkRender =>
    raw {
      RawHtml.nl2br {
        RawHtml.addLinks(text, expandImg = true, linkRender = linkRender.some)
      }
    }
  }

  def many(texts: Seq[String]): Fu[Seq[Frag]] = texts.map(one).sequenceFu

  def manyPosts(posts: Seq[Post]): Fu[Seq[Post.WithFrag]] =
    many(posts.map(_.text)) map {
      _ zip posts map { case (body, post) =>
        Post.WithFrag(post, body)
      }
    }
}
