package lila.report

import play.api.libs.json._

object JsonView {

  implicit val reportWrites: Writes[Report] = Json.writes[Report]
}
