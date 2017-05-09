package lila.report

import play.api.libs.json._

object JsonView {

  private implicit val ReasonWrites = Writes[Reason] { v => JsString(v.key) }

  implicit val reportWrites: Writes[Report] = Json.writes[Report]
}
