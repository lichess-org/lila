package lila.ublog

import org.joda.time.DateTime
import play.api.i18n.Lang
import reactivemongo.api.bson._
import scala.util.{ Success, Try }

import lila.common.Iso
import lila.db.dsl._

private object UblogBsonHandlers {

  import lila.memo.PicfitImage.imageIdBSONHandler
  import UblogPost.{ LightPost, Likes, PreviewPost, Rank, Recorded, Views }

  implicit val blogIdHandler = tryHandler[UblogBlog.Id](
    { case BSONString(v) => UblogBlog.Id(v).toTry(s"Invalid blog id $v") },
    id => BSONString(id.full)
  )
  implicit val blogBSONHandler = Macros.handler[UblogBlog]

  implicit val postIdBSONHandler      = stringAnyValHandler[UblogPost.Id](_.value, UblogPost.Id)
  implicit val topicBsonHandler       = stringAnyValHandler[UblogPost.Topic](_.value, UblogPost.Topic.apply)
  implicit val langBsonHandler        = stringAnyValHandler[Lang](_.code, Lang.apply)
  implicit val recordedBSONHandler    = Macros.handler[Recorded]
  implicit val likesBSONHandler       = intAnyValHandler[Likes](_.value, Likes)
  implicit val viewsBSONHandler       = intAnyValHandler[Views](_.value, Views)
  implicit val rankBSONHandler        = dateIsoHandler[Rank](Iso[DateTime, Rank](Rank, _.value))
  implicit val postBSONHandler        = Macros.handler[UblogPost]
  implicit val lightPostBSONHandler   = Macros.handler[LightPost]
  implicit val previewPostBSONHandler = Macros.handler[PreviewPost]

  val lightPostProjection = $doc("title" -> true)
  val previewPostProjection =
    $doc(
      "blog"    -> true,
      "title"   -> true,
      "intro"   -> true,
      "image"   -> true,
      "created" -> true,
      "lived"   -> true
    )
}
