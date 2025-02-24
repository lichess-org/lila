package lila.relay

import play.api.libs.json.*
import scalalib.Json.paginatorWriteNoNbResults
import scalalib.paginator.Paginator

import lila.common.Json.{ *, given }
import lila.core.config.BaseUrl
import lila.memo.PicfitUrl
import lila.relay.RelayTour.{ WithLastRound, WithRounds }
import lila.study.ChapterPreview
import lila.core.fide.FideTC
import lila.core.socket.SocketVersion

final class JsonView(baseUrl: BaseUrl, markup: RelayMarkup, picfitUrl: PicfitUrl)(using Executor):

  import JsonView.{ Config, given }

  given Writes[RelayTour.Tier] = writeAs(_.v)

  given Writes[FideTC]           = writeAs(_.toString)
  given Writes[java.time.ZoneId] = writeAs(_.getId)

  given OWrites[RelayTour.Info] = Json.writes

  given Writes[RelayTour.Dates] = writeAs: ds =>
    List(ds.start.some, ds.end).flatten.map(Json.toJson)

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

  def fullTourWithRounds(trs: WithRounds, group: Option[RelayGroup.WithTours])(using Config): JsObject =
    Json
      .obj(
        "tour" -> fullTour(trs.tour),
        "rounds" -> trs.rounds.map: round =>
          withUrl(round.withTour(trs.tour), withTour = false)
      )
      .add("group" -> group)
      .add("defaultRoundId" -> RelayListing.defaultRoundToLink(trs).map(_.id))

  def tourWithAnyRound(t: RelayTour | WithLastRound | RelayCard)(using Config): JsObject = t match
    case tour: RelayTour => Json.obj("tour" -> fullTour(tour))
    case tr: WithLastRound =>
      Json
        .obj(
          "tour"  -> fullTour(tr.tour),
          "round" -> withUrl(tr.round.withTour(tr.tour), withTour = false)
        )
        .add("group" -> tr.group)
    case tr: RelayCard =>
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
      targetRound: Option[RelayRound.WithTour],
      socketVersion: Option[SocketVersion]
  )(using Option[Me]): JsObject =
    myRound(rt) ++ Json
      .obj("games" -> previews)
      .add("group" -> group)
      .add("targetRound" -> targetRound.map(withUrl(_, true)))
      .add("socketVersion" -> socketVersion)

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
        .add("note" -> trs.tour.note.ifTrue(canContribute))
        .add("pinned" -> pinned.map: p =>
          Json
            .obj("name" -> p.name)
            .add("redirect" -> p.upstream.map(_.urls.redirect))
            .add("text" -> p.text)),
      study = studyData.study,
      analysis = studyData.analysis,
      group = group.map(_.group.name)
    )

  def top(
      active: List[RelayCard],
      past: Paginator[WithLastRound]
  )(using Config) =
    Json.obj(
      "active"   -> active.map(tourWithAnyRound(_)),
      "upcoming" -> Json.arr(), // BC
      "past"     -> paginatorWriteNoNbResults.writes(past.map(tourWithAnyRound(_)))
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
      .add("finishedAt" -> r.finishedAt)
      .add("finished" -> r.isFinished) // BC
      .add("ongoing" -> (r.hasStarted && !r.isFinished))
      .add("startsAt" -> r.startsAtTime.orElse(r.startedAt))
      .add("startsAfterPrevious" -> r.startsAfterPrevious)

  def statsJson(stats: RelayStats.RoundStats) =
    Json.obj(
      "viewers" -> stats.viewers.map: (minute, crowd) =>
        Json.arr(minute * 60, crowd)
    )

  import RelayRound.Sync

  private given Writes[Sync.OnlyRound] = Writes(r => r.fold(JsString(_), JsNumber(_)))

  private given OWrites[Sync] = OWrites: s =>
    Json
      .obj(
        "ongoing" -> s.ongoing,
        "log"     -> s.log.events
      )
      .add("filter" -> s.onlyRound)
      .add("slices" -> s.slices.map(RelayGame.Slices.show))
      .add("delay" -> s.delay) ++
      s.upstream.so:
        case Sync.Upstream.Url(url)     => Json.obj("url" -> url)
        case Sync.Upstream.Urls(urls)   => Json.obj("urls" -> urls)
        case Sync.Upstream.Ids(ids)     => Json.obj("ids" -> ids)
        case Sync.Upstream.Users(users) => Json.obj("users" -> users)
