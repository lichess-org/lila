package lila.clas

import lila.db.dsl._
import reactivemongo.api.bson._

private[clas] object BsonHandlers {

  implicit val teacherIdBSONHandler = stringAnyValHandler[Teacher.Id](_.value, Teacher.Id.apply)
  implicit val teacherBSONHandler   = Macros.handler[Teacher]

  implicit val clasIdBSONHandler = stringAnyValHandler[Clas.Id](_.value, Clas.Id.apply)
  implicit val clasBSONHandler   = Macros.handler[Clas]
}
