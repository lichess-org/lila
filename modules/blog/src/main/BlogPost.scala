package lila.blog

import java.time.LocalDate

case class BlogPost(doc: io.prismic.Document, coll: String = "blog", imgSize: String = "wide"):
  export doc.*
  def title     = getText(s"$coll.title")
  def shortlede = ~getText(s"$coll.shortlede")
  def date      = getDate(s"$coll.date").map(_.value)
  def image     = getImage(s"$coll.image", imgSize).map(_.url)
  def forKids   = getText(s"$coll.kidsafe").fold(true)(_ == "true")

case class MiniPost(
    id: String,
    slug: String,
    title: String,
    shortlede: String,
    date: LocalDate,
    image: String,
    forKids: Boolean
)

object MiniPost:

  def apply(post: BlogPost): Option[MiniPost] = for
    title <- post.title
    date  <- post.date
    image <- post.image
  yield MiniPost(post.id, post.slug, title, post.shortlede, date, image, post.forKids)
