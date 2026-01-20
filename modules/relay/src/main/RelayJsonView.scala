package lila.relay

import play.api.libs.json.*
import scalalib.Json.paginatorWriteNoNbResults
import scalalib.paginator.Paginator

import lila.common.Json.{ *, given }
import lila.core.config.RouteUrl
import lila.memo.PicfitUrl
import lila.relay.RelayTour.{ WithLastRound, WithRounds }
import lila.study.ChapterPreview
import lila.study.Settings
import lila.core.socket.SocketVersion
import lila.core.LightUser.GetterSync
import lila.core.i18n.Translate
import lila.core.fide.PhotosJson

final class RelayJsonView(
    routeUrl: RouteUrl,
    picfitUrl: PicfitUrl,
    lightUserSync: GetterSync,
    markdown: RelayMarkdown
):

  import RelayJsonView.{ Config, given }

  given Writes[RelayTour.Tier] = writeAs(_.v)

  given Writes[chess.FideTC] = writeAs(_.toString)
  given Writes[java.time.ZoneId] = writeAs(_.getId)

  given OWrites[RelayTour.Info] = Json.writes

  given Writes[RelayTour.Dates] = writeAs: ds =>
    List(ds.start.some, ds.end).flatten.map(Json.toJson)

  given OWrites[RelayTour] = OWrites: t =>
    Json
      .obj(
        "id" -> t.id,
        "name" -> t.name,
        "slug" -> t.slug,
        "info" -> t.info,
        "createdAt" -> t.createdAt,
        "url" -> routeUrl(t.call)
      )
      .add("tier" -> t.tier)
      .add("dates" -> t.dates)
      .add("image" -> t.image.map(id => RelayTour.thumbnail(picfitUrl, id, _.Size.Large)))

  given OWrites[RelayTour.TourPreview] = Json.writes

  given OWrites[RelayGroup.WithTours] = OWrites: g =>
    Json.obj(
      "id" -> g.group.id,
      "slug" -> g.group.name.toSlug,
      "name" -> g.group.name,
      "tours" -> g.withShorterTourNames.tours
    )

  def fullTour(tour: RelayTour)(using config: Config): JsObject =
    Json
      .toJsObject(tour)
      .add("description" -> {
        if config.html then markdown.of(tour).map(_.value) else tour.markup.map(_.value)
      })
      .add("teamTable" -> tour.teamTable)
      .add("showTeamScores" -> tour.showTeamScores)
      .add("communityOwner" -> tour.communityOwner.map(lightUserSync))

  def fullTourWithRounds(trs: WithRounds, group: Option[RelayGroup.WithTours])(using
      Config,
      Translate
  ): JsObject =
    Json
      .obj(
        "tour" -> fullTour(trs.tour),
        "rounds" -> trs.rounds.map: round =>
          withUrl(round.withTour(trs.tour), withTour = false)
      )
      .add("group" -> group)
      .add("defaultRoundId" -> RelayDefaults.defaultRoundToLink(trs).map(_.id))

  def tourWithAnyRound(t: RelayTour | WithLastRound | RelayCard)(using Config, Translate): JsObject = t match
    case tour: RelayTour => Json.obj("tour" -> fullTour(tour))
    case tr: WithLastRound =>
      Json
        .obj(
          "tour" -> fullTour(tr.tour),
          "round" -> withUrl(tr.round.withTour(tr.tour), withTour = false)
        )
        .add("group" -> tr.group)
    case tr: RelayCard =>
      Json
        .obj(
          "tour" -> fullTour(tr.tour),
          "round" -> withUrl(tr.display.withTour(tr.tour), withTour = false)
        )
        .add("roundToLink" -> (tr.link.id != tr.display.id).option(apply(tr.link)))
        .add("group" -> tr.group)

  def apply(round: RelayRound)(using Translate): JsObject = Json.toJsObject(round)

  def withUrl(rt: RelayRound.WithTour, withTour: Boolean)(using Translate): JsObject =
    apply(rt.round) ++ Json
      .obj("url" -> routeUrl(rt.call))
      .add("tour" -> withTour.option(rt.tour))

  def withUrlAndPreviews(
      rt: RelayRound.WithTourAndStudy,
      previews: ChapterPreview.AsJsons,
      group: Option[RelayGroup.WithTours],
      targetRound: Option[RelayRound.WithTour],
      isSubscribed: Option[Boolean],
      socketVersion: Option[SocketVersion],
      photos: PhotosJson
  )(using Option[Me])(using Translate): JsObject =
    myRound(rt) ++ Json
      .obj("games" -> previews, "photos" -> photos)
      .add("group" -> group)
      .add("targetRound" -> targetRound.map(withUrl(_, true)))
      .add("isSubscribed", isSubscribed)
      .add("socketVersion" -> socketVersion)

  def sync(round: RelayRound) = Json.toJsObject(round.sync)

  def myRound(r: RelayRound.WithTourAndStudy)(using me: Option[Me])(using Translate) =

    def allowed(selection: Settings => Settings.UserSelection): Boolean =
      Settings.UserSelection.allows(selection(r.study.settings), r.study, me.map(_.userId))

    val cheatable = r.relay.sync.isInternalWithoutDelay && !r.relay.isFinished

    Json.obj(
      "round" -> apply(r.relay)
        .add("url" -> routeUrl(r.call).some)
        .add("delay" -> r.relay.sync.delay),
      "tour" -> fullTour(r.tour)(using Config(html = false)),
      "study" -> Json.obj(
        "writeable" -> me.exists(r.study.canContribute),
        "features" -> Json.obj(
          "chat" -> allowed(_.chat),
          "computer" -> (!cheatable && allowed(_.computer)),
          "explorer" -> (!cheatable && allowed(_.explorer))
        )
      )
    )

  def makeData(
      trs: RelayTour.WithRounds,
      currentRoundId: RelayRoundId,
      studyData: lila.study.JsonView.JsData,
      group: Option[RelayGroup.WithTours],
      canContribute: Boolean,
      isSubscribed: Option[Boolean],
      videoUrls: Option[PairOf[String]],
      pinned: Option[RelayPinnedStream],
      delayedUntil: Option[Instant],
      photos: PhotosJson
  )(using Translate) =
    RelayJsonView.JsData(
      relay = fullTourWithRounds(trs, group)(using Config(html = true))
        .add("sync" -> canContribute.so(trs.rounds.find(_.id == currentRoundId).map(_.sync)))
        .add("lcc", trs.rounds.find(_.id == currentRoundId).map(_.sync.upstream.exists(_.hasLcc)))
        .add("isSubscribed" -> isSubscribed)
        .add("videoUrls" -> videoUrls)
        .add("note" -> canContribute.so(trs.tour.note))
        .add("delayedUntil" -> delayedUntil)
        .add("photos" -> photos.some)
        .add("pinned" -> pinned.map: p =>
          Json
            .obj("name" -> p.name)
            .add("redirect" -> p.upstream.map(_.urls.redirect))
            .add("text" -> p.text)),
      study = studyData.study,
      analysis = studyData.analysis,
      group = group.map(_.group.name)
    )

  def home(h: RelayHome)(using Config, Translate) = top(h.ongoing ::: h.recent, h.past)

  def top(active: List[RelayCard | WithLastRound], tours: Paginator[WithLastRound])(using Config, Translate) =
    Json.obj(
      "active" -> active.map(tourWithAnyRound),
      "upcoming" -> Json.arr(), // BC
      "past" -> paginatorWriteNoNbResults.writes(tours.map(tourWithAnyRound))
    )

  def search(tours: Paginator[WithLastRound])(using Config, Translate) =
    paginatorWriteNoNbResults.writes(tours.map(tourWithAnyRound(_)))

object RelayJsonView:

  case class Config(html: Boolean)

  case class JsData(
      relay: JsObject,
      study: JsObject,
      analysis: JsObject,
      group: Option[RelayGroup.Name]
  )

  given OWrites[RelayPinnedStream] = OWrites: s =>
    Json.obj("name" -> s.name)

  given OWrites[SyncLog.Event] = Json.writes

  given OWrites[RelayRound.CustomScoring] = Json.writes

  given (using Translate): OWrites[RelayRound] = OWrites: r =>
    Json
      .obj(
        "id" -> r.id,
        "name" -> r.transName,
        "slug" -> r.slug,
        "createdAt" -> r.createdAt,
        "rated" -> r.rated
      )
      .add("finishedAt" -> r.finishedAt)
      .add("finished" -> r.isFinished) // BC
      .add("ongoing" -> (r.hasStarted && !r.isFinished))
      .add("startsAt" -> r.startsAtTime.orElse(r.startedAt))
      .add("startsAfterPrevious" -> r.startsAfterPrevious)
      .add("customScoring" -> r.customScoring)

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
        "log" -> s.log.events
      )
      .add("filter" -> s.onlyRound)
      .add("slices" -> s.slices.map(RelayGame.Slices.show))
      .add("delay" -> s.delay) ++
      s.upstream.so:
        case Sync.Upstream.Url(url) => Json.obj("url" -> url)
        case Sync.Upstream.Urls(urls) => Json.obj("urls" -> urls)
        case Sync.Upstream.Ids(ids) => Json.obj("ids" -> ids)
        case Sync.Upstream.Users(users) => Json.obj("users" -> users)

  private given OWrites[chess.format.pgn.Tags] = OWrites: tags =>
    Json.obj(tags.value.map(t => (t.name.name, t.value))*)

  given OWrites[RelayPush.Results] = OWrites: results =>
    Json.obj:
      "games" -> results.map:
        _.fold(
          fail => Json.obj("tags" -> fail.tags, "error" -> fail.error),
          pass => Json.obj("tags" -> pass.tags, "moves" -> pass.moves)
        )
