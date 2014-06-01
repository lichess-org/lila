package lila.blog

case class MiniPost(
  id: String,
  slug: String,
  title: String,
  shortlede: String,
  date: String,
  image: String)

object MiniPost {

  def fromDocument(doc: io.prismic.Document): Option[MiniPost] = for {
    title <- doc getText "blog.title"
    shortlede <- doc getText "blog.shortlede"
    date <- doc getText "blog.date"
    image <- doc.getImage("blog.image", "column").map(_.url)
  } yield MiniPost(doc.id, doc.slug, title, shortlede, date, image)
}
