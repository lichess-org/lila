package lila.report

import play.api.libs.json._

import lila.common.PimpedJson._

object JsonView {

  private implicit val reasonWrites = stringIsoWriter(Reason.reasonIso)
  private implicit val roomWrites = stringIsoWriter(Room.roomIso)
  private implicit val inquiryWrites: Writes[Report.Inquiry] = Json.writes[Report.Inquiry]

  implicit val reportWrites: Writes[Report] = Json.writes[Report]
}
