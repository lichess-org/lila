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

  implicit private val relayWrites = OWrites[RelayRound] { r =>
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
      relay: RelayRound,
      studyData: lila.study.JsonView.JsData,
      canContribute: Boolean
  ) =
    JsData(
      relay = if (canContribute) admin(relay) else public(relay),
      study = studyData.study,
      analysis = studyData.analysis
    )

  def public(r: RelayRound) = relayWrites writes r

  def admin(r: RelayRound) =
    public(r)
      .add("markdown" -> r.markup)
      .add("throttle" -> r.sync.delay)
      .add("sync" -> r.sync.some)
}

object JsonView {

  case class JsData(relay: JsObject, study: JsObject, analysis: JsObject)

  implicit val syncLogEventWrites = Json.writes[SyncLog.Event]

  implicit val idWrites: Writes[RelayRound.Id] = Writes[RelayRound.Id] { id =>
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
