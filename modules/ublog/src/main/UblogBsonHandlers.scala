package lila.ublog

import org.joda.time.DateTime
import play.api.i18n.Lang
import reactivemongo.api.bson._
import scala.util.{ Success, Try }

import lila.common.Iso
import lila.db.dsl.{ *, given }

private object UblogBsonHandlers {

  import lila.memo.PicfitImage.imageIdBSONHandler
  import UblogPost.{ LightPost, Likes, PreviewPost, Rank, Recorded, Views }

  given BSONHandler[UblogBlog.Id] = tryHandler(
    { case BSONString(v) => UblogBlog.Id(v).toTry(s"Invalid blog id $v") },
    id => BSONString(id.full)
  )
  given BSONDocumentHandler[UblogBlog] = Macros.handler

  given BSONHandler[UblogPost.Id] = stringAnyValHandler(_.value, UblogPost.Id)
  implicit val topicBsonHandler  = stringAnyValHandler[UblogTopic](_.value, UblogTopic.apply)
  implicit val topicsBsonHandler = implicitly[BSONReader[List[UblogTopic]]]
    .afterRead(_.filter(t => UblogTopic.exists(t.value)))
  implicit val langBsonHandler        = stringAnyValHandler[Lang](_.code, Lang.apply)
  given BSONDocumentHandler[Recorded] = Macros.handler
  given BSONDocumentHandler[UblogImage] = Macros.handler
  implicit val likesBSONHandler       = intAnyValHandler[Likes](_.value, Likes)
  implicit val viewsBSONHandler       = intAnyValHandler[Views](_.value, Views)
  implicit val rankBSONHandler        = dateIsoHandler[Rank](Iso[DateTime, Rank](Rank, _.value))
  given BSONDocumentHandler[UblogPost] = Macros.handler
  given BSONDocumentHandler[LightPost] = Macros.handler
  given BSONDocumentHandler[PreviewPost] = Macros.handler

  val lightPostProjection = $doc("title" -> true)
  val previewPostProjection =
    $doc(
      "blog"    -> true,
      "title"   -> true,
      "intro"   -> true,
      "image"   -> true,
      "created" -> true,
      "lived"   -> true,
      "topics"  -> true
    )
}
