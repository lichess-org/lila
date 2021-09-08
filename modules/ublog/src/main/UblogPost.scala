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
    image: Option[PicfitImage.Id],
    topics: List[UblogPost.Topic],
    live: Boolean,
    created: UblogPost.Recorded,
    updated: Option[UblogPost.Recorded],
    lived: Option[UblogPost.Recorded],
    likes: UblogPost.Likes,
    views: UblogPost.Views
) extends UblogPost.BasePost {

  def isBy(u: User) = created.by == u.id
}

object UblogPost {

  case class Id(value: String) extends AnyVal with StringValue

  case class Recorded(by: User.ID, at: DateTime)

  case class Topic(value: String) extends AnyVal with StringValue

  object Topic {
    val all = List(
      "Chess",
      "Variant",
      "Chess960",
      "Crazyhouse",
      "Chess960",
      "King of the Hill",
      "Three-check",
      "Antichess",
      "Atomic",
      "Horde",
      "Racing Kings",
      "Puzzle",
      "Opening",
      "Endgame",
      "Tactics",
      "Strategy",
      "Software",
      "Lichess",
      "Off topic"
    )
    val exists                   = all.toSet
    def get(str: String)         = exists(str) option Topic(str)
    def fromStrList(str: String) = str.split(',').toList.flatMap(get).distinct
  }

  case class Likes(value: Int)     extends AnyVal
  case class Views(value: Int)     extends AnyVal
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
    val image: Option[PicfitImage.Id]
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
      image: Option[PicfitImage.Id],
      created: Recorded,
      lived: Option[Recorded]
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
