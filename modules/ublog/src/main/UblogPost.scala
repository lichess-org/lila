package lila.ublog

import org.joda.time.DateTime

import lila.memo.{ PicfitImage, PicfitUrl }
import lila.user.User
import play.api.i18n.Lang

case class UblogPost(
    _id: UblogPost.Id,
    blog: UblogBlog.Id,
    title: String,
    intro: String,
    markdown: String,
    language: Lang,
    image: Option[UblogImage],
    topics: List[UblogTopic],
    live: Boolean,
    created: UblogPost.Recorded,
    updated: Option[UblogPost.Recorded],
    lived: Option[UblogPost.Recorded],
    likes: UblogPost.Likes,
    views: UblogPost.Views
) extends UblogPost.BasePost {

  def isBy(u: User) = created.by == u.id

  def indexable = live && topics.exists(t => UblogTopic.chessExists(t.value))
}

case class UblogImage(id: PicfitImage.Id, alt: Option[String] = None, credit: Option[String] = None)

object UblogPost {

  case class Id(value: String) extends AnyVal with StringValue

  case class Recorded(by: User.ID, at: DateTime)

  case class Likes(value: Int)     extends AnyVal
  case class Views(value: Int)     extends AnyVal { def inc = Views(value + 1) }
  case class Rank(value: DateTime) extends AnyVal

  case class Create(post: UblogPost) extends AnyVal

  case class LightPost(_id: UblogPost.Id, title: String) {
    def id   = _id
    def slug = UblogPost slug title
  }

  trait BasePost {
    val _id: UblogPost.Id
    val blog: UblogBlog.Id
    val title: String
    val intro: String
    val image: Option[UblogImage]
    val created: Recorded
    val lived: Option[Recorded]
    def id   = _id
    def slug = UblogPost slug title
  }

  case class PreviewPost(
      _id: UblogPost.Id,
      blog: UblogBlog.Id,
      title: String,
      intro: String,
      image: Option[UblogImage],
      created: Recorded,
      lived: Option[Recorded]
  ) extends BasePost

  case class BlogPreview(nbPosts: Int, latests: List[PreviewPost])

  def slug(title: String) = {
    val s = lila.common.String slugify title
    if (s.isEmpty) "-" else s
  }

  object thumbnail {
    sealed abstract class Size(val width: Int) {
      def height = width * 10 / 16
    }
    case object Large extends Size(880)
    case object Small extends Size(400)
    type SizeSelector = thumbnail.type => Size

    def apply(picfitUrl: PicfitUrl, image: PicfitImage.Id, size: SizeSelector) =
      picfitUrl.thumbnail(image, size(thumbnail).width, size(thumbnail).height)
  }
}
