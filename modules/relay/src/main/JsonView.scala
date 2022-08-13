package lila.relay

import play.api.libs.json._
import scala.concurrent.duration._

import lila.common.config.BaseUrl
import lila.common.Json.jodaWrites
import lila.study.Chapter
import scala.concurrent.ExecutionContext

final class JsonView(baseUrl: BaseUrl, markup: RelayMarkup, leaderboardApi: RelayLeaderboardApi)(implicit
    ec: ExecutionContext
) {

  import JsonView._
  import lila.study.JsonView.chapterMetadataWrites

  def apply(trs: RelayTour.WithRounds, withUrls: Boolean = false): JsObject =
    Json
      .obj(
        "tour" -> Json
          .obj(
            "id"          -> trs.tour.id,
            "name"        -> trs.tour.name,
            "slug"        -> trs.tour.slug,
            "description" -> trs.tour.description
          )
          .add("markup" -> trs.tour.markup.map(markup(trs.tour)))
          .add("url" -> withUrls.option(s"$baseUrl/broadcast/${trs.tour.slug}/${trs.tour.id}")),
        "rounds" -> trs.rounds.map { round =>
          if (withUrls) withUrl(round withTour trs.tour) else apply(round)
        }
      )

  def apply(round: RelayRound): JsObject =
    Json
      .obj(
        "id"   -> round.id,
        "name" -> round.name,
        "slug" -> round.slug
      )
      .add("finished" -> round.finished)
      .add("ongoing" -> (round.hasStarted && !round.finished))
      .add("startsAt" -> round.startsAt.orElse(round.startedAt))

  def withUrl(rt: RelayRound.WithTour): JsObject =
    apply(rt.round).add("url" -> s"$baseUrl${rt.path}".some)

  def withUrlAndGames(rt: RelayRound.WithTour, games: List[Chapter.Metadata]): JsObject =
    withUrl(rt) ++ Json.obj("games" -> games.map { g =>
      chapterMetadataWrites.writes(g) + ("url" -> JsString(s"$baseUrl${rt.path}/${g._id}"))
    })

  def sync(round: RelayRound) = syncWrites writes round.sync

  def makeData(
      trs: RelayTour.WithRounds,
      currentRoundId: RelayRound.Id,
      studyData: lila.study.JsonView.JsData,
      canContribute: Boolean
  ) = leaderboardApi(trs.tour) map { leaderboard =>
    JsData(
      relay = apply(trs)
        .add("sync" -> (canContribute ?? trs.rounds.find(_.id == currentRoundId).map(_.sync)))
        .add("leaderboard" -> leaderboard.map(_.players.map(RelayLeaderboard.playerWrites.writes))),
      study = studyData.study,
      analysis = studyData.analysis
    )
  }
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
        case url: RelayRound.Sync.UpstreamUrl => Json.obj("url" -> url.withRound.url)
        case RelayRound.Sync.UpstreamIds(ids) => Json.obj("ids" -> ids)
      }
  }
}
