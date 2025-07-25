package lila.report

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

object BSONHandlers:

  import Reason.given
  import Room.given
  given BSONHandler[Reason] = stringIsoHandler
  given BSONHandler[Room] = stringIsoHandler
  given BSONDocumentHandler[Report.Inquiry] = Macros.handler
  given BSONDocumentHandler[Report.Done] = Macros.handler
  given BSONDocumentHandler[Report.Atom] = Macros.handler
  given BSONDocumentHandler[Report] = Macros.handler
