package lila.relay

import play.api.libs.json._

object JsonView {

  implicit val syncLogEventWrites = Json.writes[SyncLog.Event]

  implicit val idWrites = Writes[Relay.Id] { id =>
    JsString(id.value)
  }

  private implicit val syncWrites = OWrites[Relay.Sync] { s =>
    Json.obj(
      "seconds" -> s.seconds,
      "log" -> s.log.events,
      "url" -> s.upstream.url
    )
  }

  implicit val relayWrites = OWrites[Relay] { r =>
    Json.obj(
      "id" -> r.id,
      "slug" -> r.slug,
      "name" -> r.name,
      "description" -> r.description,
      "ownerId" -> r.ownerId,
      "sync" -> r.sync
    )
  }

  case class JsData(relay: JsObject, study: JsObject, analysis: JsObject)

  def makeData(relay: Relay, studyData: lila.study.JsonView.JsData) = JsData(
    relay = relayWrites writes relay,
    study = studyData.study,
    analysis = studyData.analysis
  )
}
