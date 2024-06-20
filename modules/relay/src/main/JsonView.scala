package lila.relay

import play.api.libs.json.*
import scalalib.Json.paginatorWriteNoNbResults

import lila.common.Json.given
import lila.core.config.BaseUrl
import lila.memo.PicfitUrl
import lila.study.ChapterPreview
import lila.core.id.ImageId
import lila.relay.RelayTour.{ WithRounds, WithLastRound, ActiveWithSomeRounds }
import scalalib.paginator.Paginator
import geny.Generator.Action

final class JsonView(
    baseUrl: BaseUrl,
    markup: RelayMarkup,
    leaderboardApi: RelayLeaderboardApi,
    picfitUrl: PicfitUrl
)(using Executor):

  import JsonView.given

  given Writes[Option[RelayTour.Tier]] = Writes: t =>
    JsString(t.flatMap(RelayTour.Tier.keys.get) | "user")

  given OWrites[RelayTour] = OWrites: t =>
    Json
      .obj(
        "id"          -> t.id,
        "name"        -> t.name,
        "slug"        -> t.slug,
        "description" -> t.description,
        "createdAt"   -> t.createdAt,
        "url"         -> s"$baseUrl${t.path}"
      )
      .add("tier" -> t.tier)
      .add("image" -> t.image.map(id => RelayTour.thumbnail(picfitUrl, id, _.Size.Large)))

  given OWrites[RelayTour.IdName] = Json.writes[RelayTour.IdName]

  given OWrites[RelayGroup.WithTours] = OWrites: g =>
    Json.obj(
      "name"  -> g.group.name,
      "tours" -> g.withShorterTourNames.tours
    )

  def fullTour(tour: RelayTour): JsObject =
    Json
      .toJsObject(tour)
      .add("markup" -> tour.markup.map(markup(tour)))
      .add("teamTable" -> tour.teamTable)
      .add("leaderboard" -> tour.autoLeaderboard)

  def fullTourWithRounds(trs: WithRounds, group: Option[RelayGroup.WithTours]): JsObject =
    Json
      .obj(
        "tour" -> fullTour(trs.tour),
        "rounds" -> trs.rounds.map: round =>
          withUrl(round.withTour(trs.tour), withTour = false)
      )
      .add("group" -> group)

  def apply(t: RelayTour | WithLastRound | ActiveWithSomeRounds): JsObject = t match
    case tour: RelayTour => Json.obj("tour" -> fullTour(tour))
    case tr: WithLastRound =>
      Json
        .obj(
          "tour"      -> fullTour(tr.tour),
          "lastRound" -> withUrl(tr.round.withTour(tr.tour), withTour = false)
        )
        .add("group" -> tr.group)
    case tr: ActiveWithSomeRounds =>
      Json
        .obj(
          "tour"      -> fullTour(tr.tour),
          "lastRound" -> withUrl(tr)
        )
        .add("group" -> tr.group)

  def apply(round: RelayRound): JsObject = Json.toJsObject(round)

  def withUrl(rt: RelayRound.WithTour, withTour: Boolean): JsObject =
    apply(rt.round) ++ Json
      .obj("url" -> s"$baseUrl${rt.path}")
      .add("tour" -> withTour.option(rt.tour))

  def withUrl(tr: ActiveWithSomeRounds): JsObject =
    val linkRound = tr.link.withTour(tr.tour)
    apply(tr.display) ++ Json.obj("url" -> s"$baseUrl${linkRound.path}")

  def withUrlAndPreviews(
      rt: RelayRound.WithTourAndStudy,
      previews: ChapterPreview.AsJsons,
      group: Option[RelayGroup.WithTours]
  )(using Option[Me]): JsObject =
    myRound(rt) ++ Json.obj("games" -> previews).add("group" -> group)

  def sync(round: RelayRound) = Json.toJsObject(round.sync)

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
      group: Option[RelayGroup.WithTours],
      canContribute: Boolean,
      isSubscribed: Option[Boolean],
      videoUrls: Option[PairOf[String]],
      pinned: Option[(UserId, String, Option[ImageId])]
  ) =
    JsonView.JsData(
      relay = fullTourWithRounds(trs, group)
        .add("sync" -> (canContribute.so(trs.rounds.find(_.id == currentRoundId).map(_.sync))))
        .add("isSubscribed" -> isSubscribed)
        .add("videoUrls" -> videoUrls)
        .add("pinned" -> pinned.map: (id, name, image) =>
          Json
            .obj(
              "userId" -> id,
              "name"   -> name
            )
            .add("image" -> image.map(id => picfitUrl.thumbnail(id, 1200, 675)))),
      study = studyData.study,
      analysis = studyData.analysis,
      group = group.map(_.group.name)
    )

  def top(
      active: List[ActiveWithSomeRounds],
      upcoming: List[WithLastRound],
      past: Paginator[WithLastRound]
  ) =
    Json.obj(
      "active"   -> active.sortBy(t => -(~t.tour.tier)).map(apply(_)),
      "upcoming" -> upcoming.map(apply(_)),
      "past"     -> paginatorWriteNoNbResults.writes(past.map(apply(_)))
    )

object JsonView:

  case class JsData(relay: JsObject, study: JsObject, analysis: JsObject, group: Option[RelayGroup.Name])

  given OWrites[SyncLog.Event] = Json.writes

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

  given OWrites[RelayStats.RoundStats] = OWrites: r =>
    Json.obj(
      "round" -> r.round,
      "viewers" -> r.viewers.map: (minute, crowd) =>
        Json.arr(minute * 60, crowd)
    )

  import RelayRound.Sync
  private given OWrites[Sync] = OWrites: s =>
    Json
      .obj(
        "ongoing" -> s.ongoing,
        "log"     -> s.log.events
      )
      .add("delay" -> s.delay) ++
      s.upstream.so:
        case Sync.UpstreamUrl(url)        => Json.obj("url" -> url)
        case Sync.UpstreamLcc(url, round) => Json.obj("url" -> url, "round" -> round)
        case Sync.UpstreamUrls(urls)      => Json.obj("urls" -> urls.map(_.formUrl))
        case Sync.UpstreamIds(ids)        => Json.obj("ids" -> ids)
