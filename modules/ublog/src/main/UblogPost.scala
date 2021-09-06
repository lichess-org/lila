package lila.ublog

import org.joda.time.DateTime

import lila.memo.{ PicfitImage, PicfitUrl }
import lila.user.User
import play.api.i18n.Lang

case class UblogPost(
    _id: UblogPost.Id,
    user: User.ID,
    title: String,
    intro: String,
    markdown: String,
    language: Lang,
    image: Option[PicfitImage.Id],
    live: Boolean,
    createdAt: DateTime,
    updatedAt: DateTime,
    liveAt: Option[DateTime],
    likes: UblogPost.Likes
) extends UblogPost.BasePost {

  def isBy(u: User) = user == u.id
}

object UblogPost {

  case class Id(value: String) extends AnyVal with StringValue

  case class Likes(value: Int) extends AnyVal
  case class Liking(likes: Likes, me: Boolean)
  val emptyLiking = Liking(Likes(0), me = false)

  case class Rank(value: DateTime) extends AnyVal
  object Rank {
    def compute(likes: Likes, liveAt: DateTime) =
      Rank {
        liveAt plusHours likesToHours(likes)
      }
    private def likesToHours(likes: Likes): Int =
      if (likes.value < 1) 0
      else (5 * math.log(likes.value) + 1).toInt.min(likes.value) * 24
  }

  case class Create(post: UblogPost) extends AnyVal

  case class LightPost(_id: UblogPost.Id, title: String) {
    def id   = _id
    def slug = UblogPost slug title
  }

  trait BasePost {
    val _id: UblogPost.Id
    val user: User.ID
    val title: String
    val intro: String
    val image: Option[PicfitImage.Id]
    val liveAt: Option[DateTime]
    def id   = _id
    def slug = UblogPost slug title
  }

  case class PreviewPost(
      _id: UblogPost.Id,
      user: User.ID,
      title: String,
      intro: String,
      image: Option[PicfitImage.Id],
      liveAt: Option[DateTime]
  ) extends BasePost

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
