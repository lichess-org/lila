package lila.ublog

import org.joda.time.DateTime

import lila.user.User

case class UblogPost(
    _id: UblogPost.Id,
    user: User.ID,
    title: String,
    intro: String,
    markdown: String,
    createdAt: DateTime,
    updatedAt: DateTime
) {

  lazy val slug = {
    val s = lila.common.String slugify title
    if (s.isEmpty) "-" else s
  }
}

object UblogPost {

  def make(user: User, title: String, intro: String, markdown: String) =
    UblogPost(
      _id = Id(lila.common.ThreadLocalRandom nextString 8),
      user = user.id,
      title = title,
      intro = intro,
      markdown = markdown,
      createdAt = DateTime.now,
      updatedAt = DateTime.now
    )

  case class Id(value: String) extends AnyVal with StringValue
}
