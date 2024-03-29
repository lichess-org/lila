package lila.hub
package ublog

trait UblogPost:
  val id: UblogPostId
  val created: UblogPost.Recorded

object UblogPost:

  case class Recorded(by: UserId, at: Instant)

  case class Create(post: UblogPost) extends AnyVal

  case class LightPost(id: UblogPostId, title: String):
    def slug = UblogPost.slug(title)

  def slug(title: String) =
    val s = lila.common.String.slugify(title)
    if s.isEmpty then "-" else s

trait UblogApi:
  def liveLightsByIds(ids: List[UblogPostId]): Fu[List[UblogPost.LightPost]]
