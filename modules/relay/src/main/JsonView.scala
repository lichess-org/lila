package lila.relay

import play.api.libs.json._

import lila.common.Json.jodaWrites

final class JsonView(markup: RelayMarkup) {

  import JsonView._

  implicit val relayWrites = OWrites[Relay] { r =>
    Json
      .obj(
        "id"          -> r.id,
        "slug"        -> r.slug,
        "name"        -> r.name,
        "description" -> r.description,
        "ownerId"     -> r.ownerId,
        "sync"        -> r.sync
      )
      .add("credit", r.credit)
      .add("markup" -> r.markup.map(markup.apply))
  }

  def makeData(relay: Relay, studyData: lila.study.JsonView.JsData) = JsData(
    relay = relayWrites writes relay,
    study = studyData.study,
    analysis = studyData.analysis
  )

  def apiShow(r: Relay) =
    relayWrites
      .writes(r)
      .add("markdown" -> r.markup)
      .add("startsAt" -> r.startsAt)
      .add("startedAt" -> r.startedAt)
      .add("official" -> r.official.option(true))
      .add("throttle" -> r.sync.delay)
}

object JsonView {

  case class JsData(relay: JsObject, study: JsObject, analysis: JsObject)

  implicit val syncLogEventWrites = Json.writes[SyncLog.Event]

  implicit val idWrites: Writes[Relay.Id] = Writes[Relay.Id] { id =>
    JsString(id.value)
  }

  implicit private val syncWrites: OWrites[Relay.Sync] = OWrites[Relay.Sync] { s =>
    Json.obj(
      "ongoing" -> s.ongoing,
      "log"     -> s.log.events,
      "url"     -> s.upstream.map(_.url)
    )
  }
}
