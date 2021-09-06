package lila.ublog

import lila.db.dsl._
import reactivemongo.api.bson._
import play.api.i18n.Lang

private[ublog] object UblogBsonHandlers {

  import lila.memo.PicfitImage.imageIdBSONHandler
  implicit val postIdBSONHandler = stringAnyValHandler[UblogPost.Id](_.value, UblogPost.Id.apply)
  implicit val langBsonHandler   = stringAnyValHandler[Lang](_.code, Lang.apply)
  implicit val postBSONHandler   = Macros.handler[UblogPost]
  import UblogPost.{ LightPost, PreviewPost }
  implicit val lightPostBSONHandler   = Macros.handler[LightPost]
  implicit val previewPostBSONHandler = Macros.handler[PreviewPost]

  val lightPostProjection = $doc("title" -> true)
  val previewPostProjection =
    $doc("title" -> true, "user" -> true, "intro" -> true, "image" -> true, "liveAt" -> true)
}
