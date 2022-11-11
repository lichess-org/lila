package lila.clas

import lila.db.dsl.{ *, given }
import reactivemongo.api.bson._

private[clas] object BsonHandlers {

  import Clas.Recorded
  given BSONDocumentHandler[Recorded] = Macros.handler

  given BSONHandler[Clas.Id] = stringAnyValHandler(_.value, Clas.Id.apply)
  given BSONDocumentHandler[Clas] = Macros.handler

  given BSONHandler[Student.Id] = stringAnyValHandler(_.value, Student.Id.apply)
  given BSONDocumentHandler[Student] = Macros.handler

  given BSONHandler[ClasInvite.Id] = stringAnyValHandler(_.value, ClasInvite.Id.apply)
  given BSONDocumentHandler[ClasInvite] = Macros.handler
}
