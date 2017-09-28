package lila.relay

import play.api.libs.json._

object JsonView {

  implicit val relayWrites = OWrites[Relay] { r =>
    Json.obj(
      "id" -> r.id.value,
      "name" -> r.name,
      "description" -> r.description,
      "ownerId" -> r.ownerId
    )
  }

  case class JsData(relay: JsObject, study: JsObject, analysis: JsObject)

  def makeData(relay: Relay, studyData: lila.study.JsonView.JsData) = JsData(
    relay = relayWrites writes relay,
    study = studyData.study,
    analysis = studyData.analysis
  )
}
