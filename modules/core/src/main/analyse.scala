package lila.core
package analyse

import play.api.libs.json.JsObject

case class AnalysisProgress(payload: () => JsObject)
