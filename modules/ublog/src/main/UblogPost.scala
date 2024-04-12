package lila.ublog

import reactivemongo.api.bson.Macros.Annotations.Key

import lila.core.i18n.Language
import lila.memo.{ PicfitImage, PicfitUrl }

import lila.core.data.OpaqueInstant

case class UblogPost(
    @Key("_id") id: UblogPostId,
    blog: UblogBlog.Id,
    title: String,
    intro: String,
    markdown: Markdown,
    language: Language,
    image: Option[UblogImage],
    topics: List[UblogTopic],
    live: Boolean,
    discuss: Option[Boolean],
    created: UblogPost.Recorded,
    updated: Option[UblogPost.Recorded],
    lived: Option[UblogPost.Recorded],
    likes: UblogPost.Likes,
    views: UblogPost.Views,
    rankAdjustDays: Option[Int],
    pinned: Option[Boolean]
) extends UblogPost.BasePost
    with lila.core.ublog.UblogPost:

  def isBy[U: UserIdOf](u: U) = created.by.is(u)

  def indexable = live && topics.exists(UblogTopic.chessExists)
  def allText   = s"$title $intro $markdown"

  def allows                    = UblogBlog.Allows(created.by)
  def canView(using Option[Me]) = live || allows.draft

case class UblogImage(id: PicfitImage.Id, alt: Option[String] = None, credit: Option[String] = None)

object UblogPost:

  export lila.core.ublog.UblogPost.*

  def slug(title: String) =
    val s = lila.common.String.slugify(title)
    if s.isEmpty then "-" else s

  opaque type Likes = Int
  object Likes extends OpaqueInt[Likes]
  opaque type Views = Int
  object Views extends OpaqueInt[Views]

  opaque type RankDate = Instant
  object RankDate extends OpaqueInstant[RankDate]

  trait BasePost extends lila.core.ublog.UblogPost:
    val blog: UblogBlog.Id
    val title: String
    val intro: String
    val image: Option[UblogImage]
    val created: Recorded
    val lived: Option[Recorded]
    def slug      = UblogPost.slug(title)
    def isLichess = created.by.is(UserId.lichess)

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

  def randomId = UblogPostId(scalalib.ThreadLocalRandom.nextString(8))

  object thumbnail:
    enum Size(val width: Int):
      def height = width * 10 / 16
      case Large extends Size(880)
      case Small extends Size(400)
    type SizeSelector = thumbnail.type => Size

    def apply(picfitUrl: PicfitUrl, image: PicfitImage.Id, size: SizeSelector) =
      picfitUrl.thumbnail(image, size(thumbnail).width, size(thumbnail).height)
