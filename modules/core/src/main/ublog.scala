package lila.core
package ublog

import lila.core.id.UblogPostId
import lila.core.userId.UserId

trait UblogPost:
  val id: UblogPostId
  val created: UblogPost.Recorded

object UblogPost:

  case class Recorded(by: UserId, at: Instant)

  case class Create(post: UblogPost) extends AnyVal

  case class LightPost(id: UblogPostId, title: String):
    def slug = scalalib.StringOps.slug(title)

trait UblogApi:
  def liveLightsByIds(ids: List[UblogPostId]): Fu[List[UblogPost.LightPost]]
