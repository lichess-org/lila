package lila.report

import play.api.libs.json._

object JsonView {

  private implicit val reasonWrites = Writes[Reason] { v => JsString(v.key) }
  private implicit val inquiryWrites: Writes[Report.Inquiry] = Json.writes[Report.Inquiry]

  implicit val reportWrites: Writes[Report] = Json.writes[Report]
}
