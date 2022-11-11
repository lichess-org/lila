package lila.clas

import lila.db.dsl.{ *, given }
import reactivemongo.api.bson.*

private object BsonHandlers:

  given BSONDocumentHandler[Clas.Recorded] = Macros.handler

  given BSONHandler[Clas.Id]      = stringAnyValHandler(_.value, Clas.Id.apply)
  given BSONDocumentHandler[Clas] = Macros.handler

  given studentIdHandler: BSONHandler[Student.Id] = stringAnyValHandler(_.value, Student.Id.apply)
  given BSONDocumentHandler[Student]              = Macros.handler

  given inviteIdHandler: BSONHandler[ClasInvite.Id] = stringAnyValHandler(_.value, ClasInvite.Id.apply)
  given BSONDocumentHandler[ClasInvite]             = Macros.handler
