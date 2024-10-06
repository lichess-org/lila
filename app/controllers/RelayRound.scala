package controllers

import chess.format.pgn.{ PgnStr, Tag }
import play.api.libs.json.{ Json, OWrites }
import play.api.mvc.*

import scala.annotation.nowarn

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.core.id.{ RelayRoundId, RelayTourId }
import lila.relay.ui.FormNavigation
import lila.relay.{ RelayRound as RoundModel, RelayTour as TourModel, RelayVideoEmbed as VideoEmbed }

final class RelayRound(
    env: Env,
    studyC: => Study,
    apiC: => Api
) extends LilaController(env):

  def form(tourId: RelayTourId) = Auth { ctx ?=> _ ?=>
    NoLameOrBot:
      WithNavigationCanUpdate(tourId): nav =>
        Ok.page:
          views.relay.form.round
            .create(env.relay.roundForm.create(nav.tourWithRounds), nav)
  }

  def create(tourId: RelayTourId) = AuthOrScopedBody(_.Study.Write) { ctx ?=> me ?=>
    NoLameOrBot:
      WithNavigationCanUpdate(tourId): nav =>
        def whenRateLimited = negotiate(
          Redirect(routes.RelayTour.show(nav.tour.slug, nav.tour.id)),
          rateLimited
        )
        bindForm(env.relay.roundForm.create(nav.tourWithRounds))(
          err =>
            negotiate(
              BadRequest.page(views.relay.form.round.create(err, nav)),
              jsonFormError(err)
            ),
          setup =>
            rateLimitCreation(whenRateLimited):
              env.relay.api
                .create(setup, nav.tour)
                .flatMap: rt =>
                  negotiate(
                    Redirect(routes.RelayRound.edit(rt.relay.id)).flashSuccess,
                    JsonOk(env.relay.jsonView.myRound(rt))
                  )
        )
  }

  def edit(id: RelayRoundId) = Auth { ctx ?=> me ?=>
    env.relay.api
      .byIdAndContributor(id)
      .flatMap:
        case None =>
          Found(env.relay.api.formNavigation(id)): (_, nav) =>
            Forbidden.page(views.relay.form.noAccess(nav))
        case Some(rt) =>
          env.relay.api
            .formNavigation(rt)
            .flatMap: (round, nav) =>
              Ok.page(views.relay.form.round.edit(round, env.relay.roundForm.edit(nav.tour, round), nav))
  }

  def update(id: RelayRoundId) = AuthOrScopedBody(_.Study.Write) { ctx ?=> me ?=>
    given play.api.Mode = env.mode
    env.relay.api
      .formNavigation(id)
      .flatMapz: (round, nav) =>
        bindForm(env.relay.roundForm.edit(nav.tour, round))(
          err => fuccess(Left((round, nav) -> err)),
          data =>
            env.relay.api
              .update(round)(data.update(nav.tour.official))
              .dmap(_.withTour(nav.tour))
              .dmap(Right(_))
        ).dmap(some)
      .orNotFound:
        _.fold(
          { case ((round, nav), err) =>
            negotiate(
              BadRequest.page(views.relay.form.round.edit(round, err, nav)),
              jsonFormError(err)
            )
          },
          rt =>
            negotiate(
              Redirect(routes.RelayRound.edit(id)).flashSuccess,
              JsonOk(env.relay.jsonView.withUrl(rt, withTour = true))
            )
        )
  }

  def reset(id: RelayRoundId) = AuthOrScoped(_.Study.Write) { ctx ?=> me ?=>
    Found(env.relay.api.byIdAndContributor(id)): rt =>
      env.relay.api.reset(rt.round) >> negotiate(Redirect(rt.path), jsonOkResult)
  }

  def show(ts: String, rs: String, id: RelayRoundId) =
    OpenOrScoped(_.Study.Read): ctx ?=>
      negotiate(
        html = WithRoundAndTour(ts, rs, id): rt =>
          val sc = env.study.preview
            .firstId(rt.round.studyId)
            .flatMap:
              // there might be no chapter after a round reset, let a new one be created
              case None              => env.study.api.byIdWithChapter(rt.round.studyId)
              case Some(firstChapId) => env.study.api.byIdWithChapterOrFallback(rt.round.studyId, firstChapId)
          sc.orNotFound: study =>
            env.relay.videoEmbed.withCookie:
              doShow(rt, study, _)
        ,
        json = doApiShow(id)
      )

  def apiShow(ts: String, rs: String, id: RelayRoundId) = AnonOrScoped(_.Study.Read, _.Web.Mobile):
    doApiShow(id)

  def embedShow(ts: String, rs: String, id: RelayRoundId): EssentialAction =
    Anon:
      InEmbedContext:
        FoundEmbed(env.relay.api.byIdWithTour(id))(embedShow)

  def embedShow(rt: RoundModel.WithTour)(using EmbedContext): Fu[Result] =
    env.study.preview
      .firstId(rt.round.studyId)
      .flatMapz(env.study.api.byIdWithChapterOrFallback(rt.round.studyId, _))
      .orNotFoundEmbed: oldSc =>
        studyC.CanView(oldSc.study)(
          for
            (sc, studyData) <- studyC.getJsonData(oldSc, withChapters = true)
            rounds          <- env.relay.api.byTourOrdered(rt.tour)
            group           <- env.relay.api.withTours.get(rt.tour.id)
            data = env.relay.jsonView.makeData(
              rt.tour.withRounds(rounds.map(_.round)),
              rt.round.id,
              studyData,
              group,
              canContribute = false,
              isSubscribed = none,
              videoUrls = none,
              pinned = none
            )
            sVersion <- NoCrawlers(env.study.version(sc.study.id))
            embed    <- views.relay.embed(rt.withStudy(sc.study), data, sVersion)
          yield Ok(embed).enforceCrossSiteIsolation
        )(
          studyC.privateUnauthorizedFu(oldSc.study),
          studyC.privateForbiddenFu(oldSc.study)
        )

  private def doApiShow(id: RelayRoundId)(using Context): Fu[Result] =
    Found(env.relay.api.byIdWithTour(id))(doApiShow)

  def doApiShow(rt: RoundModel.WithTour)(using Context): Fu[Result] =
    Found(env.study.studyRepo.byId(rt.round.studyId)): study =>
      studyC.CanView(study)(
        for
          group       <- env.relay.api.withTours.get(rt.tour.id)
          previews    <- env.study.preview.jsonList.withoutInitialEmpty(study.id)
          targetRound <- env.relay.api.officialTarget(rt.round)
        yield JsonOk(
          env.relay.jsonView.withUrlAndPreviews(rt.withStudy(study), previews, group, targetRound)
        )
      )(studyC.privateUnauthorizedJson, studyC.privateForbiddenJson)

  def pgn(ts: String, rs: String, id: RelayRoundId) = Open:
    pgnWithFlags(ts, rs, id)

  def apiPgn(id: RelayRoundId) = AnonOrScoped(_.Study.Read): ctx ?=>
    pgnWithFlags("-", "-", id)

  private def pgnWithFlags(ts: String, rs: String, id: RelayRoundId)(using Context): Fu[Result] =
    studyC.pgnWithFlags(
      id.into(StudyId),
      _.copy(
        site = s"${env.net.baseUrl}${routes.RelayRound.show(ts, rs, id)}".some,
        comments = false,
        variations = false
      )
    )

  def apiMyRounds = Scoped(_.Study.Read) { ctx ?=> _ ?=>
    val source = env.relay.api.myRounds(MaxPerSecond(20), getIntAs[Max]("nb")).map(env.relay.jsonView.myRound)
    apiC.GlobalConcurrencyLimitPerIP.download(ctx.ip)(source)(jsToNdJson)
  }

  def stream(id: RelayRoundId) = AnonOrScoped(): ctx ?=>
    Found(env.relay.api.byIdWithStudy(id)): rs =>
      studyC.CanView(rs.study) {
        apiC.GlobalConcurrencyLimitPerIP
          .events(req.ipAddress)(env.relay.pgnStream.streamRoundGames(rs)): source =>
            noProxyBuffer(Ok.chunked[PgnStr](source.keepAlive(60.seconds, () => PgnStr(" "))))
      }(Unauthorized, Forbidden)

  def chapter(ts: String, rs: String, id: RelayRoundId, chapterId: StudyChapterId) =
    Open:
      WithRoundAndTour(ts, rs, id, chapterId.some): rt =>
        Found(env.study.api.byIdWithChapterOrFallback(rt.round.studyId, chapterId)): study =>
          env.relay.videoEmbed.withCookie:
            doShow(rt, study, _)

  def push(id: RelayRoundId) = ScopedBody(parse.tolerantText)(Seq(_.Study.Write)) { ctx ?=> me ?=>
    Found(env.relay.api.byIdWithTourAndStudy(id)): rt =>
      if !rt.study.canContribute(me) then forbiddenJson()
      else
        given OWrites[List[Tag]] = OWrites(tags => Json.obj(tags.map(t => (t.name.name, t.value))*))
        env.relay
          .push(rt.withTour, PgnStr(ctx.body.body))
          .map: results =>
            JsonOk:
              Json.obj:
                "games" -> results.map:
                  _.fold(
                    fail => Json.obj("tags" -> fail.tags.value, "error" -> fail.error),
                    pass => Json.obj("tags" -> pass.tags.value, "moves" -> pass.moves)
                  )
  }

  def teamsView(id: RelayRoundId) = Open:
    Found(env.relay.api.byIdWithTourAndStudy(id)): rt =>
      studyC.CanView(rt.study) {
        rt.tour.teamTable.so:
          env.relay.teamTable.tableJson(rt.relay).map(JsonStrOk)
      }(Unauthorized, Forbidden)

  def stats(id: RelayRoundId) = Open:
    env.relay.stats
      .get(id)
      .map: stats =>
        import lila.relay.JsonView.given
        JsonOk(stats)

  private def WithRoundAndTour(
      @nowarn ts: String,
      @nowarn rs: String,
      id: RelayRoundId,
      chapterId: Option[StudyChapterId] = None
  )(
      f: RoundModel.WithTour => Fu[Result]
  )(using ctx: Context): Fu[Result] =
    Found(env.relay.api.byIdWithTour(id)): rt =>
      if !ctx.req.path.startsWith(rt.path) && HTTPRequest.isRedirectable(ctx.req)
      then Redirect(chapterId.fold(rt.path)(rt.path))
      else f(rt)

  private def WithTour(id: RelayTourId)(
      f: TourModel => Fu[Result]
  )(using Context): Fu[Result] =
    Found(env.relay.api.tourById(id))(f)

  private def WithNavigationCanUpdate(id: RelayTourId)(
      f: FormNavigation => Fu[Result]
  )(using ctx: Context): Fu[Result] =
    WithTour(id): tour =>
      ctx.me
        .soUse { env.relay.api.canUpdate(tour) }
        .elseNotFound:
          env.relay.api.formNavigation(tour).flatMap(f)

  private def doShow(rt: RoundModel.WithTour, oldSc: lila.study.Study.WithChapter, embed: VideoEmbed)(using
      ctx: Context
  ): Fu[Result] =
    studyC.CanView(oldSc.study)(
      for
        (sc, studyData) <- studyC.getJsonData(oldSc, withChapters = true)
        rounds          <- env.relay.api.byTourOrdered(rt.tour)
        group           <- env.relay.api.withTours.get(rt.tour.id)
        isSubscribed <- ctx.me.soFu: me =>
          env.relay.api.isSubscribed(rt.tour.id, me.userId)
        videoUrls <- embed match
          case VideoEmbed.Auto =>
            fuccess:
              rt.tour.pinnedStream
                .ifFalse(rt.round.finished)
                .flatMap(_.upstream)
                .map(_.urls(netDomain).toPair)
          case VideoEmbed.No => fuccess(none)
          case VideoEmbed.Stream(userId) =>
            env.streamer.api
              .find(userId)
              .flatMapz(s => env.streamer.liveStreamApi.of(s).dmap(some))
              .map:
                _.flatMap(_.stream).map(_.urls(netDomain).toPair)
        crossSiteIsolation = videoUrls.isEmpty
        data = env.relay.jsonView.makeData(
          rt.tour.withRounds(rounds.map(_.round)),
          rt.round.id,
          studyData,
          group,
          ctx.userId.exists(sc.study.canContribute),
          isSubscribed,
          videoUrls,
          rt.tour.pinnedStream
        )
        chat     <- NoCrawlers(studyC.chatOf(sc.study))
        sVersion <- NoCrawlers(env.study.version(sc.study.id))
        page <- renderPage:
          views.relay.show(rt.withStudy(sc.study), data, chat, sVersion, crossSiteIsolation)
        _ = if HTTPRequest.isHuman(req) then lila.mon.http.path(rt.tour.path).increment()
      yield
        if crossSiteIsolation then Ok(page).enforceCrossSiteIsolation
        else Ok(page).withHeaders(crossOriginPolicy.unsafe*)
    )(
      studyC.privateUnauthorizedFu(oldSc.study),
      studyC.privateForbiddenFu(oldSc.study)
    )

  private[controllers] def rateLimitCreation(fail: => Fu[Result])(
      create: => Fu[Result]
  )(using me: Me, req: RequestHeader): Fu[Result] =
    val cost =
      if isGranted(_.StudyAdmin) then 1
      else if isGranted(_.Relay) then 2
      else if me.hasTitle || me.isVerified then 5
      else 10
    limit.relay(me.userId -> req.ipAddress, fail, cost)(create)
