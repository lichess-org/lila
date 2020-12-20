package lila.relay

import play.api.libs.json._

import lila.common.Json.jodaWrites
import lila.common.config.BaseUrl

final class JsonView(markup: RelayMarkup, baseUrl: BaseUrl) {

  import JsonView._

  implicit private val relayWrites = OWrites[Relay] { r =>
    Json
      .obj(
        "id"          -> r.id,
        "url"         -> s"$baseUrl/broadcast/${r.slug}/${r.id}",
        "name"        -> r.name,
        "description" -> r.description
      )
      .add("credit", r.credit)
      .add("markup" -> r.markup.map(markup.apply))
      .add("startsAt" -> r.startsAt)
      .add("startedAt" -> r.startedAt)
      .add("official" -> r.official.option(true))
      .add("finished" -> r.finished.option(true))
  }

  def makeData(
      relay: Relay,
      studyData: lila.study.JsonView.JsData,
      canContribute: Boolean
  ) =
    JsData(
      relay = if (canContribute) admin(relay) else public(relay),
      study = studyData.study,
      analysis = studyData.analysis
    )

  def public(r: Relay) = relayWrites writes r

  def admin(r: Relay) =
    public(r)
      .add("markdown" -> r.markup)
      .add("throttle" -> r.sync.delay)
      .add("sync" -> r.sync.some)
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
