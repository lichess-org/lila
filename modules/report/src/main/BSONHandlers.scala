package lila.report

import reactivemongo.api.bson._

import lila.db.dsl._

object BSONHandlers {

  implicit val ReasonBSONHandler: BSONHandler[Reason] = isoHandler[Reason, String](Reason.reasonIso)
  implicit val RoomBSONHandler: BSONHandler[Room]     = isoHandler[Room, String](Room.roomIso)
  import Report.Atom
  import Report.Inquiry
  import Report.Score
  implicit val InquiryBSONHandler: BSONDocumentHandler[Inquiry] = Macros.handler[Inquiry]
  implicit val ReporterIdBSONHandler: BSONHandler[ReporterId] =
    stringIsoHandler[ReporterId](ReporterId.reporterIdIso)
  implicit val ScoreIdBSONHandler: BSONHandler[Score]     = doubleIsoHandler[Score](Report.scoreIso)
  implicit val AtomBSONHandler: BSONDocumentHandler[Atom] = Macros.handler[Atom]
  implicit val ReportBSONHandler: BSONDocumentHandler[Report] = Macros.handler[Report]
}
