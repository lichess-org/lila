package lila.relay

import play.api.libs.json.*

import lila.common.config.BaseUrl
import lila.common.Json.given
import lila.study.Chapter
import lila.user.Me

final class JsonView(baseUrl: BaseUrl, markup: RelayMarkup, leaderboardApi: RelayLeaderboardApi)(using
    Executor
):

  import JsonView.given
  import lila.study.JsonView.given

  given OWrites[RelayTour] = OWrites: t =>
    Json
      .obj(
        "id"          -> t.id,
        "name"        -> t.name,
        "slug"        -> t.slug,
        "description" -> t.description,
        "createdAt"   -> t.createdAt
      )
      .add("official" -> t.official)

  given OWrites[RelayRound] = OWrites: r =>
    Json
      .obj(
        "id"        -> r.id,
        "name"      -> r.name,
        "slug"      -> r.slug,
        "createdAt" -> r.createdAt
      )
      .add("finished" -> r.finished)
      .add("ongoing" -> (r.hasStarted && !r.finished))
      .add("startsAt" -> r.startsAt.orElse(r.startedAt))

  def apply(trs: RelayTour.WithRounds, withUrls: Boolean = false): JsObject =
    Json
      .obj(
        "tour" -> Json
          .toJsObject(trs.tour)
          .add("markup" -> trs.tour.markup.map(markup(trs.tour)))
          .add("url" -> withUrls.option(s"$baseUrl${trs.tour.path}")),
        "rounds" -> trs.rounds.map: round =>
          if withUrls then withUrl(round withTour trs.tour) else apply(round)
      )

  def apply(round: RelayRound): JsObject = Json.toJsObject(round)

  def withUrl(rt: RelayRound.WithTour): JsObject =
    apply(rt.round) ++ Json.obj(
      "tour" -> rt.tour,
      "url"  -> s"$baseUrl${rt.path}"
    )

  def withUrlAndGames(rt: RelayRound.WithTourAndStudy, games: List[Chapter.Metadata])(using
      Option[Me]
  ): JsObject =
    myRound(rt) ++
      Json.obj("games" -> games.map { g =>
        Json.toJsObject(g) + ("url" -> JsString(s"$baseUrl${rt.path}/${g._id}"))
      })

  def sync(round: RelayRound) = Json toJsObject round.sync

  def myRound(r: RelayRound.WithTourAndStudy)(using me: Option[Me]) = Json
    .obj(
      "round" -> apply(r.relay)
        .add("url" -> s"$baseUrl${r.path}".some)
        .add("delay" -> r.relay.sync.delay),
      "tour"  -> r.tour,
      "study" -> Json.obj("writeable" -> me.exists(r.study.canContribute))
    )

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
