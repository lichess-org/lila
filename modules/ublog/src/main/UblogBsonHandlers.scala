package lila.ublog

import lila.db.dsl._
import reactivemongo.api.bson._
import play.api.i18n.Lang
import lila.common.Iso
import org.joda.time.DateTime

private[ublog] object UblogBsonHandlers {

  import lila.memo.PicfitImage.imageIdBSONHandler
  import UblogPost.{ LightPost, Likes, PreviewPost, Rank }

  implicit val postIdBSONHandler      = stringAnyValHandler[UblogPost.Id](_.value, UblogPost.Id.apply)
  implicit val langBsonHandler        = stringAnyValHandler[Lang](_.code, Lang.apply)
  implicit val likesBSONHandler       = intAnyValHandler[Likes](_.value, Likes.apply)
  implicit val rankBSONHandler        = dateIsoHandler[Rank](Iso[DateTime, Rank](Rank.apply, _.value))
  implicit val postBSONHandler        = Macros.handler[UblogPost]
  implicit val lightPostBSONHandler   = Macros.handler[LightPost]
  implicit val previewPostBSONHandler = Macros.handler[PreviewPost]

  val lightPostProjection = $doc("title" -> true)
  val previewPostProjection =
    $doc("title" -> true, "user" -> true, "intro" -> true, "image" -> true, "liveAt" -> true)
}
