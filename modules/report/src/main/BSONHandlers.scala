package lila.report

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import reactivemongo.bson._

object BSONHandlers {

  implicit val ReasonBSONHandler = isoHandler[Reason, String, BSONString](Reason.reasonIso)
  implicit val RoomBSONHandler = isoHandler[Room, String, BSONString](Room.roomIso)
  import Report.{ Inquiry, Score, Atom }
  implicit val InquiryBSONHandler = Macros.handler[Inquiry]
  implicit val ReporterIdBSONHandler = stringIsoHandler[ReporterId](ReporterId.reporterIdIso)
  implicit val ScoreIdBSONHandler = doubleIsoHandler[Score](Report.scoreIso)
  implicit val AtomBSONHandler = Macros.handler[Atom]
  implicit val ReportBSONHandler = Macros.handler[Report]
}
