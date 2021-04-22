package lila.relay

import com.github.blemale.scaffeine.LoadingCache
import play.api.libs.json._
import scala.concurrent.duration._

import lila.common.config.BaseUrl
import lila.common.Json.jodaWrites

final class JsonView(baseUrl: BaseUrl) {

  import JsonView._

  private val markdown = new lila.common.Markdown
  private val markdownCache: LoadingCache[String, String] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(10 minutes)
    .maximumSize(64)
    .build(markdown.apply)

  implicit private val relayWrites = OWrites[Relay] { r =>
    Json
      .obj(
        "id"          -> r.id,
        "url"         -> s"$baseUrl/broadcast/${r.slug}/${r.id}",
        "name"        -> r.name,
        "description" -> r.description
      )
      .add("credit", r.credit)
      .add("markup" -> r.markup.map(markdownCache.get))
      .add("startsAt" -> r.startsAt)
      .add("startedAt" -> r.startedAt)
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
      "log"     -> s.log.events
    ) ++
      s.upstream.?? {
        case Relay.Sync.UpstreamUrl(url) => Json.obj("url" -> url)
        case Relay.Sync.UpstreamIds(ids) => Json.obj("ids" -> ids)
      }
  }
}
