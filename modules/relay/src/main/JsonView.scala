package lila.relay

import play.api.libs.json.*
import scalalib.Json.paginatorWriteNoNbResults
import scalalib.paginator.Paginator

import lila.common.Json.given
import lila.core.config.BaseUrl
import lila.core.id.ImageId
import lila.memo.PicfitUrl
import lila.relay.RelayTour.{ ActiveWithSomeRounds, WithLastRound, WithRounds }
import lila.study.ChapterPreview

final class JsonView(
    baseUrl: BaseUrl,
    markup: RelayMarkup,
    leaderboardApi: RelayLeaderboardApi,
    picfitUrl: PicfitUrl
)(using Executor):

  import JsonView.{ Config, given }

  given Writes[Option[RelayTour.Tier]] = Writes: t =>
    JsString(t.flatMap(RelayTour.Tier.keys.get) | "user")

  given OWrites[RelayTour.Info] = Json.writes

  given Writes[RelayTour.Dates] = Writes: ds =>
    JsArray(List(ds.start.some, ds.end).flatten.map(Json.toJson))

  given OWrites[RelayTour] = OWrites: t =>
    Json
      .obj(
        "id"        -> t.id,
        "name"      -> t.name,
        "slug"      -> t.slug,
        "info"      -> t.info,
        "createdAt" -> t.createdAt,
        "url"       -> s"$baseUrl${t.path}"
      )
      .add("tier" -> t.tier)
      .add("dates" -> t.dates)
      .add("image" -> t.image.map(id => RelayTour.thumbnail(picfitUrl, id, _.Size.Large)))

  given OWrites[RelayTour.IdName] = Json.writes

  given OWrites[RelayGroup.WithTours] = OWrites: g =>
    Json.obj(
      "name"  -> g.group.name,
      "tours" -> g.withShorterTourNames.tours
    )

  def fullTour(tour: RelayTour)(using config: Config): JsObject =
    Json
      .toJsObject(tour)
      .add("description" -> tour.markup.map: md =>
        if config.html then markup(tour)(md).value
        else md.value)
      .add("teamTable" -> tour.teamTable)
      .add("leaderboard" -> tour.autoLeaderboard)

  def fullTourWithRounds(trs: WithRounds, group: Option[RelayGroup.WithTours])(using Config): JsObject =
    Json
      .obj(
        "tour" -> fullTour(trs.tour),
        "rounds" -> trs.rounds.map: round =>
          withUrl(round.withTour(trs.tour), withTour = false)
      )
      .add("group" -> group)

  def apply(t: RelayTour | WithLastRound | ActiveWithSomeRounds)(using Config): JsObject = t match
    case tour: RelayTour => Json.obj("tour" -> fullTour(tour))
    case tr: WithLastRound =>
      Json
        .obj(
          "tour"  -> fullTour(tr.tour),
          "round" -> withUrl(tr.round.withTour(tr.tour), withTour = false)
        )
        .add("group" -> tr.group)
    case tr: ActiveWithSomeRounds =>
      Json
        .obj(
          "tour"  -> fullTour(tr.tour),
          "round" -> apply(tr.display)
        )
        .add("roundToLink" -> (tr.link.id != tr.display.id).option(apply(tr.link)))
        .add("group" -> tr.group)

  def apply(round: RelayRound): JsObject = Json.toJsObject(round)

  def withUrl(rt: RelayRound.WithTour, withTour: Boolean): JsObject =
    apply(rt.round) ++ Json
      .obj("url" -> s"$baseUrl${rt.path}")
      .add("tour" -> withTour.option(rt.tour))

  def withUrlAndPreviews(
      rt: RelayRound.WithTourAndStudy,
      previews: ChapterPreview.AsJsons,
      group: Option[RelayGroup.WithTours],
      targetRound: Option[RelayRound.WithTour]
  )(using Option[Me]): JsObject =
    myRound(rt) ++ Json
      .obj("games" -> previews)
      .add("group" -> group)
      .add("targetRound" -> targetRound.map(withUrl(_, true)))

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
      pinned: Option[RelayPinnedStream]
  ) =
    JsonView.JsData(
      relay = fullTourWithRounds(trs, group)(using Config(html = true))
        .add("sync" -> (canContribute.so(trs.rounds.find(_.id == currentRoundId).map(_.sync))))
        .add("lcc", trs.rounds.find(_.id == currentRoundId).map(_.sync.upstream.exists(_.hasLcc)))
        .add("isSubscribed" -> isSubscribed)
        .add("videoUrls" -> videoUrls)
        .add("pinnedStream" -> pinned),
      study = studyData.study,
      analysis = studyData.analysis,
      group = group.map(_.group.name)
    )

  def top(
      active: List[ActiveWithSomeRounds],
      upcoming: List[WithLastRound],
      past: Paginator[WithLastRound]
  )(using Config) =
    Json.obj(
      "active"   -> active.sortBy(t => -(~t.tour.tier)).map(apply(_)),
      "upcoming" -> upcoming.map(apply(_)),
      "past"     -> paginatorWriteNoNbResults.writes(past.map(apply(_)))
    )

object JsonView:

  case class Config(html: Boolean)

  case class JsData(relay: JsObject, study: JsObject, analysis: JsObject, group: Option[RelayGroup.Name])

  given OWrites[RelayPinnedStream] = OWrites: s =>
    Json.obj("name" -> s.name)

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
      .add("startsAt" -> r.startsAtTime.orElse(r.startedAt))

  given OWrites[RelayStats.RoundStats] = OWrites: r =>
    Json.obj(
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
      .add("filter" -> s.onlyRound)
      .add("slices" -> s.slices.map(_.mkString(", ")))
      .add("delay" -> s.delay) ++
      s.upstream.so:
        case Sync.Upstream.Url(url)   => Json.obj("url" -> url)
        case Sync.Upstream.Urls(urls) => Json.obj("urls" -> urls)
        case Sync.Upstream.Ids(ids)   => Json.obj("ids" -> ids)
