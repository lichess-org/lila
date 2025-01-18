package lila.report

import lila.db.dsl._
import reactivemongo.api.bson._

object BSONHandlers {

  implicit val ReasonBSONHandler: BSONHandler[Reason] = isoHandler[Reason, String](Reason.reasonIso)
  implicit val RoomBSONHandler: BSONHandler[Room]   = isoHandler[Room, String](Room.roomIso)
  import Report.{ Atom, Inquiry, Score }
  implicit val InquiryBSONHandler: BSONDocumentHandler[Inquiry]    = Macros.handler[Inquiry]
  implicit val ReporterIdBSONHandler: BSONHandler[ReporterId] = stringIsoHandler[ReporterId](ReporterId.reporterIdIso)
  implicit val ScoreIdBSONHandler: BSONHandler[Score]    = doubleIsoHandler[Score](Report.scoreIso)
  implicit val AtomBSONHandler: BSONDocumentHandler[Atom]       = Macros.handler[Atom]
  implicit val ReportBSONHandler: BSONDocumentHandler[Report]     = Macros.handler[Report]
}
