package lila.ublog

import org.joda.time.DateTime

import lila.memo.PicfitImage
import lila.user.User

case class UblogPost(
    _id: UblogPost.Id,
    user: User.ID,
    title: String,
    intro: String,
    markdown: String,
    image: Option[PicfitImage.Id],
    live: Boolean,
    createdAt: DateTime,
    updatedAt: DateTime,
    liveAt: Option[DateTime]
) {

  def id = _id

  lazy val slug = {
    val s = lila.common.String slugify title
    if (s.isEmpty) "-" else s
  }

  def isBy(u: User) = user == u.id
}

object UblogPost {

  case class Id(value: String) extends AnyVal with StringValue
}
