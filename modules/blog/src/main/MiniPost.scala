package lila.blog

import org.joda.time.DateTime

case class MiniPost(
    id: String,
    title: String,
    shortlede: String,
    date: DateTime,
    image: String
)

object MiniPost {

  def fromDocument(coll: String, imgSize: String = "icon")(
      doc: io.prismic.Document
  ): Option[MiniPost] =
    for {
      title <- doc getText s"$coll.title"
      shortlede = ~(doc getText s"$coll.shortlede")
      date  <- doc getDate s"$coll.date" map (_.value)
      image <- doc.getImage(s"$coll.image", imgSize).map(_.url)
    } yield MiniPost(doc.id, title, shortlede, date.toDateTimeAtStartOfDay, image)
}
