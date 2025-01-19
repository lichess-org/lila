package lila.clas

import reactivemongo.api.bson._

import lila.db.dsl._

private[clas] object BsonHandlers {

  import Clas.Recorded
  implicit val recordedBSONHandler: BSONDocumentHandler[Recorded] = Macros.handler[Recorded]

  implicit val clasIdBSONHandler: BSONHandler[Clas.Id] =
    stringAnyValHandler[Clas.Id](_.value, Clas.Id.apply)
  implicit val clasBSONHandler: BSONDocumentHandler[Clas] = Macros.handler[Clas]

  implicit val studentIdBSONHandler: BSONHandler[Student.Id] =
    stringAnyValHandler[Student.Id](_.value, Student.Id.apply)
  implicit val studentBSONHandler: BSONDocumentHandler[Student] = Macros.handler[Student]

  implicit val inviteIdBSONHandler: BSONHandler[ClasInvite.Id] =
    stringAnyValHandler[ClasInvite.Id](_.value, ClasInvite.Id.apply)
  implicit val inviteBSONHandler: BSONDocumentHandler[ClasInvite] = Macros.handler[ClasInvite]
}
