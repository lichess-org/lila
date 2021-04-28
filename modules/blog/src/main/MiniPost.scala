package lila.blog

import org.joda.time.DateTime

case class MiniPost(
    id: String,
    slug: String,
    title: String,
    shortlede: String,
    date: DateTime,
    image: String,
    langCode: String
)

object MiniPost {

  def fromDocument(coll: String, imgSize: String = "icon", langCode: String = "en-US")(doc: io.prismic.Document): Option[MiniPost] = {
    for {
      title <- doc getText s"$coll.title"
      shortlede = ~(doc getText s"$coll.shortlede")
      date  <- doc getDate s"$coll.date" map (_.value)
      image <- doc.getImage(s"$coll.image", imgSize).map(_.url)
    } yield MiniPost(doc.id, doc.slug, title, shortlede, date.toDateTimeAtStartOfDay, image, langCode)
  }
}
