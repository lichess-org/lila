package lila.report

import lila.db.dsl.{ *, given }
import reactivemongo.api.bson._

object BSONHandlers {

  implicit val ReasonBSONHandler = isoHandler[Reason, String](Reason.reasonIso)
  implicit val RoomBSONHandler   = isoHandler[Room, String](Room.roomIso)
  import Report.{ Atom, Done, Inquiry, Score }
  given BSONDocumentHandler[Inquiry] = Macros.handler
  given BSONDocumentHandler[Done] = Macros.handler
  implicit val ReporterIdBSONHandler = stringIsoHandler[ReporterId](ReporterId.reporterIdIso)
  implicit val ScoreIdBSONHandler    = doubleIsoHandler[Score](Report.scoreIso)
  given BSONDocumentHandler[Atom] = Macros.handler
  given BSONDocumentHandler[Report] = Macros.handler
}
