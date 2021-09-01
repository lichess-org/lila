package lila.ublog

import lila.db.dsl._
import reactivemongo.api.bson._

private[ublog] object UblogBsonHandlers {

  import lila.memo.PicfitImage.imageIdBSONHandler
  implicit val postIdBSONHandler = stringAnyValHandler[UblogPost.Id](_.value, UblogPost.Id.apply)
  implicit val postBSONHandler   = Macros.handler[UblogPost]
  import UblogPost.LightPost
  implicit val lightPostBSONHandler = Macros.handler[LightPost]
}
