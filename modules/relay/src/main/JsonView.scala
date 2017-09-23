package lila.relay

import play.api.libs.json._

object JsonView {

  case class JsData(study: JsObject, analysis: JsObject)
}
