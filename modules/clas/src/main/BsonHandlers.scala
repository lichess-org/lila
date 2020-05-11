package lila.clas

import lila.db.dsl._
import reactivemongo.api.bson._

private[clas] object BsonHandlers {

  import Clas.Recorded
  implicit val recordedBSONHandler = Macros.handler[Recorded]

  implicit val clasIdBSONHandler = stringAnyValHandler[Clas.Id](_.value, Clas.Id.apply)
  implicit val clasBSONHandler   = Macros.handler[Clas]

  implicit val studentIdBSONHandler = stringAnyValHandler[Student.Id](_.value, Student.Id.apply)
  implicit val studentBSONHandler   = Macros.handler[Student]

  implicit val inviteIdBSONHandler = stringAnyValHandler[ClasInvite.Id](_.value, ClasInvite.Id.apply)
  implicit val inviteBSONHandler   = Macros.handler[ClasInvite]
}
