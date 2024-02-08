package lila.blog

import java.time.LocalDate
import io.prismic.Fragment.Image.View

case class BlogPost(doc: io.prismic.Document, coll: String = "blog", imgSize: String = "wide"):
  export doc.*
  def title: Option[String]    = getText(s"$coll.title")
  def shortlede: String        = ~getText(s"$coll.shortlede")
  def date: Option[LocalDate]  = getDate(s"$coll.date").map(_.value)
  def imageObj: Option[View]   = getImage(s"$coll.image", imgSize)
  def image: Option[String]    = imageObj.map(_.url)
  def forKids: Boolean         = getText(s"$coll.kidsafe").forall(_ == "true")
  def author: Option[String]   = getText(s"$coll.author")
  def category: Option[String] = getText(s"$coll.category")

case class MiniPost(
    id: String,
    slug: String,
    title: String,
    shortlede: String,
    date: LocalDate,
    image: String,
    forKids: Boolean
):
  def isOld = date.isBefore(LocalDate.now.minusDays(7))

object MiniPost:

  def apply(post: BlogPost): Option[MiniPost] = for
    title <- post.title
    date  <- post.date
    image <- post.image
  yield MiniPost(post.id, post.slug, title, post.shortlede, date, image, post.forKids)
