package lila.ublog

import reactivemongo.api.bson.Macros.Annotations.Key

import scalalib.ThreadLocalRandom.shuffle
import scalalib.model.Language
import lila.core.id.ImageId

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
    sticky: Option[Boolean],
    ads: Option[Boolean], // boo!
    created: UblogPost.Recorded,
    updated: Option[UblogPost.Recorded],
    lived: Option[UblogPost.Recorded],
    featured: Option[UblogPost.Featured],
    likes: UblogPost.Likes,
    views: UblogPost.Views,
    similar: Option[List[UblogSimilar]],
    automod: Option[UblogAutomod.Assessment]
) extends UblogPost.BasePost
    with lila.core.ublog.UblogPost:

  def isBy[U: UserIdOf](u: U) = created.by.is(u)
  def isUserBlog[U: UserIdOf](u: U) = blog == UblogBlog.Id.User(u.id)

  def indexable = live && topics.exists(UblogTopic.chessExists)
  def allText = s"$title $intro $markdown"

  def allows = UblogBlog.Allows(created.by)
  def canView(using Option[Me]) = live || allows.draft

case class UblogImage(id: ImageId, alt: Option[String] = None, credit: Option[String] = None)

case class UblogSimilar(id: UblogPostId, count: Int)

object UblogPost:

  export lila.core.ublog.UblogPost.*

  def slug(title: String) =
    val s = scalalib.StringOps.slug(title)
    if s.isEmpty then "-" else s

  opaque type Likes = Int
  object Likes extends RelaxedOpaqueInt[Likes]
  opaque type Views = Int
  object Views extends RelaxedOpaqueInt[Views]

  trait BasePost extends lila.core.ublog.UblogPost:
    val blog: UblogBlog.Id
    val title: String
    val intro: String
    val image: Option[UblogImage]
    val created: Recorded
    val updated: Option[Recorded]
    val lived: Option[Recorded]
    val featured: Option[Featured]
    val sticky: Option[Boolean]
    def slug = UblogPost.slug(title)
    def isLichess = created.by.is(UserId.lichess)

  case class PreviewPost(
      @Key("_id") id: UblogPostId,
      blog: UblogBlog.Id,
      title: String,
      intro: String,
      image: Option[UblogImage],
      created: Recorded,
      updated: Option[Recorded],
      lived: Option[Recorded],
      featured: Option[Featured],
      sticky: Option[Boolean],
      topics: List[UblogTopic]
  ) extends BasePost

  case class Featured(by: UserId, at: Option[Instant], until: Option[Instant] = none)

  case class CarouselPosts(
      pinned: List[PreviewPost],
      queue: List[PreviewPost]
  ):
    def shuffled: List[PreviewPost] =
      (pinned ++ shuffle(queue)).toList

    def has(id: UblogPostId): Boolean =
      pinned.exists(_.id == id) || queue.exists(_.id == id)

  case class BlogPreview(nbPosts: Int, latests: List[PreviewPost])

  def randomId = UblogPostId(scalalib.ThreadLocalRandom.nextString(8))

  object thumbnail:
    enum Size(val width: Int):
      def height = width * 10 / 16
      case Large extends Size(880)
      case Small extends Size(400)
    type SizeSelector = thumbnail.type => Size

    def apply(picfitUrl: lila.core.misc.PicfitUrl, image: ImageId, size: SizeSelector): String =
      picfitUrl.thumbnail(image, size(thumbnail).width, size(thumbnail).height)
