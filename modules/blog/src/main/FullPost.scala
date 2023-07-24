package lila.blog

import org.joda.time.DateTime

case class FullPost(
    doc: io.prismic.Document,
    id: String,
    coll: String,
    title: String,
    date: DateTime,
    image: String,
    author: String,
    category: String,
    allLangIds: Option[BlogLang.Ids] // only when translated, hacky but whatevs
) {
  def isJapanese = allLangIds.exists(_.ja == id)
}

object FullPost {

  def fromDocument(coll: String)(
      doc: io.prismic.Document
  ): Option[FullPost] =
    for {
      title    <- doc getText s"$coll.title"
      date     <- doc.getDate(s"$coll.date").map(_.value)
      image    <- doc.getImage(s"$coll.image", "main").map(_.url)
      author   <- doc getText s"$coll.author"
      category <- doc getText s"$coll.category"
    } yield FullPost(
      doc = doc,
      id = doc.id,
      coll = coll,
      title = title,
      date = date.toDateTimeAtStartOfDay,
      image = image,
      author = author,
      category = category,
      allLangIds = doc
        .getText(s"$coll.alllangids")
        .flatMap(BlogLang.Ids.apply)
        .filter(ids => doc.id == ids.en || doc.id == ids.ja)
    )

}
