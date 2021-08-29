package lila.ublog

import lila.db.dsl._
import reactivemongo.api.bson._

private[ublog] object UblogBsonHandlers {

  implicit val postIdBSONHandler = stringAnyValHandler[UblogPost.Id](_.value, UblogPost.Id.apply)
  implicit val postBSONHandler   = Macros.handler[UblogPost]
}
