package lila.relay

import play.api.libs.json._
import scala.concurrent.duration._

import lila.common.config.BaseUrl
import lila.common.Json.jodaWrites

final class JsonView(baseUrl: BaseUrl, markup: RelayMarkup) {

  import JsonView._

  implicit private val roundWithTourWrites = OWrites[RelayRound.WithTour] { rt =>
    Json
      .obj(
        "id"   -> rt.round.id,
        "url"  -> s"$baseUrl${rt.path}",
        "name" -> rt.round.name,
        "tour" -> Json
          .obj(
            "id"          -> rt.tour.id,
            "url"         -> s"$baseUrl/${rt.tour.slug}/${rt.tour.id}",
            "description" -> rt.tour.description,
            "active"      -> rt.tour.active
          )
          .add("credit", rt.tour.credit)
          .add("markup" -> rt.tour.markup.map(markup.apply))
      )
      .add("startsAt" -> rt.round.startsAt)
      .add("startedAt" -> rt.round.startedAt)
      .add("finished" -> rt.round.finished.option(true))
  }

  def makeData(
      rt: RelayRound.WithTour,
      studyData: lila.study.JsonView.JsData,
      canContribute: Boolean
  ) =
    JsData(
      relay = if (canContribute) admin(rt) else public(rt),
      study = studyData.study,
      analysis = studyData.analysis
    )

  def public(r: RelayRound.WithTour) = roundWithTourWrites writes r

  def admin(rt: RelayRound.WithTour) =
    public(rt)
      .add("markdown" -> rt.tour.markup)
      .add("throttle" -> rt.round.sync.delay)
      .add("sync" -> rt.round.sync.some)
}

object JsonView {

  case class JsData(relay: JsObject, study: JsObject, analysis: JsObject)

  implicit val syncLogEventWrites = Json.writes[SyncLog.Event]

  implicit val roundIdWrites: Writes[RelayRound.Id] = Writes[RelayRound.Id] { id =>
    JsString(id.value)
  }

  implicit val tourIdWrites: Writes[RelayTour.Id] = Writes[RelayTour.Id] { id =>
    JsString(id.value)
  }

  implicit private val syncWrites: OWrites[RelayRound.Sync] = OWrites[RelayRound.Sync] { s =>
    Json.obj(
      "ongoing" -> s.ongoing,
      "log"     -> s.log.events
    ) ++
      s.upstream.?? {
        case RelayRound.Sync.UpstreamUrl(url) => Json.obj("url" -> url)
        case RelayRound.Sync.UpstreamIds(ids) => Json.obj("ids" -> ids)
      }
  }
}
