package lila.relay

import play.api.libs.json.*

import lila.common.config.BaseUrl
import lila.common.Json.given
import lila.study.Chapter

final class JsonView(baseUrl: BaseUrl, markup: RelayMarkup, leaderboardApi: RelayLeaderboardApi)(using
    Executor
):

  import JsonView.given
  import lila.study.JsonView.given

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
          .add("url" -> withUrls.option(s"$baseUrl/broadcast/${trs.tour.slug}/${trs.tour.id}"))
          .add("official" -> trs.tour.official),
        "rounds" -> trs.rounds.map { round =>
          if withUrls then withUrl(round withTour trs.tour) else apply(round)
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
      Json.toJsObject(g) + ("url" -> JsString(s"$baseUrl${rt.path}/${g._id}"))
    })

  def sync(round: RelayRound) = Json toJsObject round.sync

  def makeData(
      trs: RelayTour.WithRounds,
      currentRoundId: RelayRoundId,
      studyData: lila.study.JsonView.JsData,
      canContribute: Boolean
  ) = leaderboardApi(trs.tour) map { leaderboard =>
    JsonView.JsData(
      relay = apply(trs)
        .add("sync" -> (canContribute so trs.rounds.find(_.id == currentRoundId).map(_.sync)))
        .add("leaderboard" -> leaderboard.map(_.players)),
      study = studyData.study,
      analysis = studyData.analysis
    )
  }

object JsonView:

  case class JsData(relay: JsObject, study: JsObject, analysis: JsObject)

  given OWrites[SyncLog.Event] = Json.writes

  private given OWrites[RelayRound.Sync] = OWrites { s =>
    Json
      .obj(
        "ongoing" -> s.ongoing,
        "log"     -> s.log.events
      )
      .add("delay" -> s.delay) ++
      s.upstream.so {
        case url: RelayRound.Sync.UpstreamUrl => Json.obj("url" -> url.withRound.url)
        case RelayRound.Sync.UpstreamIds(ids) => Json.obj("ids" -> ids)
      }
  }
