package lila.ublog

import lila.memo.{ PicfitImage, PicfitUrl }
import lila.user.User
import play.api.i18n.Lang
import reactivemongo.api.bson.Macros.Annotations.Key

case class UblogPost(
    @Key("_id") id: UblogPostId,
    blog: UblogBlog.Id,
    title: String,
    intro: String,
    markdown: Markdown,
    language: Lang,
    image: Option[UblogImage],
    topics: List[UblogTopic],
    live: Boolean,
    discuss: Option[Boolean],
    created: UblogPost.Recorded,
    updated: Option[UblogPost.Recorded],
    lived: Option[UblogPost.Recorded],
    likes: UblogPost.Likes,
    views: UblogPost.Views
) extends UblogPost.BasePost:

  def isBy[U: UserIdOf](u: U) = created.by is u

  def indexable = live && topics.exists(UblogTopic.chessExists)
  def allText   = s"$title $intro $markdown"

case class UblogImage(id: PicfitImage.Id, alt: Option[String] = None, credit: Option[String] = None)

object UblogPost:

  case class Recorded(by: UserId, at: Instant)

  opaque type Likes = Int
  object Likes extends OpaqueInt[Likes]
  opaque type Views = Int
  object Views extends OpaqueInt[Views]

  opaque type RankDate = Instant
  object RankDate extends OpaqueInstant[RankDate]

  case class Create(post: UblogPost) extends AnyVal

  case class LightPost(@Key("_id") id: UblogPostId, title: String):
    def slug = UblogPost slug title

  trait BasePost:
    val id: UblogPostId
    val blog: UblogBlog.Id
    val title: String
    val intro: String
    val image: Option[UblogImage]
    val created: Recorded
    val lived: Option[Recorded]
    def slug = UblogPost slug title

  case class PreviewPost(
      @Key("_id") id: UblogPostId,
      blog: UblogBlog.Id,
      title: String,
      intro: String,
      image: Option[UblogImage],
      created: Recorded,
      lived: Option[Recorded],
      topics: List[UblogTopic]
  ) extends BasePost

  case class BlogPreview(nbPosts: Int, latests: List[PreviewPost])

  def slug(title: String) =
    val s = lila.common.String slugify title
    if s.isEmpty then "-" else s

  object thumbnail:
    enum Size(val width: Int):
      def height = width * 10 / 16
      case Large extends Size(880)
      case Small extends Size(400)
    type SizeSelector = thumbnail.type => Size

    def apply(picfitUrl: PicfitUrl, image: PicfitImage.Id, size: SizeSelector) =
      picfitUrl.thumbnail(image, size(thumbnail).width, size(thumbnail).height)
