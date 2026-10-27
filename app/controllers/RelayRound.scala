package controllers

import chess.format.pgn.{ PgnStr, Tag }
import play.api.mvc.*
import org.apache.pekko.stream.scaladsl.Source

import scala.annotation.nowarn

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.core.id.{ RelayGroupId, RelayRoundId, RelayTourId }
import lila.relay.ui.FormNavigation
import lila.relay.{ RelayRound as RoundModel, RelayTour as TourModel, RelayVideoEmbed as VideoEmbed }
import lila.study.Study as StudyModel

final class RelayRound(env: Env, studyC: => Study) extends LilaController(env):

  import env.relay.{ jsonView, api, playerApi, roundForm, pgnStream }

  def form(tourId: RelayTourId) = Auth { ctx ?=> _ ?=>
    NoLameOrBot:
      WithNavigationCanUpdate(tourId): nav =>
        Ok.page:
          views.relay.form.round
            .create(roundForm.create(nav.tourWithRounds), nav)
  }

  def create(tourId: RelayTourId) = AuthOrScopedBody(_.Study.Write) { ctx ?=> me ?=>
    NoLameOrBot:
      WithNavigationCanUpdate(tourId): nav =>
        def whenRateLimited = negotiate(Redirect(nav.tour.call), rateLimited)
        bindForm(roundForm.create(nav.tourWithRounds))(
          err =>
            negotiate(
              BadRequest.page(views.relay.form.round.create(err, nav)),
              jsonFormError(err)
            ),
          setup =>
            limit.relay.roundCreate(me.userId -> req.ipAddress, () => whenRateLimited): () =>
              api
                .create(setup, nav.tour)
                .flatMap: rt =>
                  negotiate(
                    Redirect(routes.RelayRound.edit(rt.relay.id)).flashSuccess,
                    Ok(jsonView.myRound(rt))
                  )
        )
  }

  private def accessDenied(id: RelayRoundId)(using Context) =
    negotiate(
      Found(api.byId(id)): r =>
        Forbidden.page(views.relay.form.noAccess(r)),
      forbiddenJson()
    )

  def edit(id: RelayRoundId) = Auth { ctx ?=> me ?=>
    api
      .byIdAndContributor(id)
      .flatMap:
        case None => accessDenied(id)
        case Some(rt) =>
          api
            .formNavigation(rt)
            .flatMap: (round, nav) =>
              Ok.page(views.relay.form.round.edit(round, roundForm.edit(nav.tour, round), nav))
  }

  def update(id: RelayRoundId) = AuthOrScopedBody(_.Study.Write) { ctx ?=> me ?=>
    api
      .byIdAndContributor(id)
      .flatMap:
        case None => accessDenied(id)
        case Some(rt) =>
          api
            .formNavigation(rt)
            .flatMap: (round, nav) =>
              bindForm(roundForm.edit(nav.tour, round))(
                err => fuccess(Left((round, nav, err))),
                data =>
                  api
                    .formUpdate(round, data, nav.tour)
                    .dmap(_.withTour(nav.tour))
                    .dmap(Right(_))
              )
            .flatMap:
              case Left((round, nav, err)) =>
                negotiate(
                  BadRequest.page(views.relay.form.round.edit(round, err, nav)),
                  jsonFormError(err)
                )
              case Right(_) =>
                negotiate(
                  Redirect(routes.RelayRound.edit(id)).flashSuccess,
                  doApiShow(id)
                )
  }

  def reset(id: RelayRoundId) = AuthOrScoped(_.Study.Write) { ctx ?=> me ?=>
    Found(api.byIdAndContributor(id)): rt =>
      api.reset(rt.round) >> negotiate(Redirect(rt.path), jsonOkResult)
  }

  def show(ts: String, rs: String, id: RelayRoundId) =
    OpenOrScoped(_.Study.Read): ctx ?=>
      negotiate(
        html = WithRoundAndTour(ts, rs, id): rt =>
          val sc = env.study.preview
            .firstId(rt.round.studyId)
            .flatMap:
              // there might be no chapter after a round reset, let a new one be created
              case None => env.study.api.byIdWithChapter(rt.round.studyId)
              case Some(firstChapId) => env.study.api.byIdWithChapterOrFallback(rt.round.studyId, firstChapId)
          sc.orNotFound: study =>
            env.relay.videoEmbed.withCookie:
              doShow(rt, study, _)
        ,
        json = doApiShow(id)
      )

  def apiShow(@nowarn ts: String, @nowarn rs: String, id: RelayRoundId) =
    AnonOrScoped(_.Study.Read, _.Web.Mobile):
      doApiShow(id)

  def embedShow(@nowarn ts: String, @nowarn rs: String, id: RelayRoundId): EssentialAction =
    Anon:
      InEmbedContext:
        FoundEmbed(api.byIdWithTour(id))(embedShow(_, none))

  def embedChapter(
      @nowarn ts: String,
      @nowarn rs: String,
      id: RelayRoundId,
      chapterId: StudyChapterId
  ): EssentialAction =
    Anon:
      InEmbedContext:
        FoundEmbed(api.byIdWithTour(id))(embedShow(_, chapterId.some))

  def embedShow(rt: RoundModel.WithTour, chapterId: Option[StudyChapterId])(using
      EmbedContext
  ): Fu[Result] =
    val studyAndChapter = chapterId match
      case Some(cid) => env.study.api.byIdWithChapterOrFallback(rt.round.studyId, cid)
      case None =>
        env.study.preview
          .firstId(rt.round.studyId)
          .flatMapz(env.study.api.byIdWithChapterOrFallback(rt.round.studyId, _))
    FoundEmbed(studyAndChapter): sc =>
      studyC.CanView(sc.study)(
        for
          (sc, studyData) <- studyC.getJsonData(sc, withChapters = true)
          rounds <- api.byTourOrdered(rt.tour)
          group <- api.withTours.get(rt.tour.id)
          photos <- playerApi.photosJson(rt.tour.id)
          data = jsonView.makeData(
            rt.tour.withRounds(rounds.map(_.round)),
            rt.round.id,
            studyData,
            group,
            canContribute = false,
            isSubscribed = none,
            videoUrls = none,
            pinned = none,
            delayedUntil = none,
            photos = photos
          )
          sVersion <- NoCrawlers(env.study.version(sc.study.id))
          embed <- views.relay.embed(rt.withStudy(sc.study), data, sVersion)
          _ = env.relay.stats.viewers.hit(rt)
        yield Ok(embed).enforceCrossSiteIsolation
      )(
        studyC.privateUnauthorizedFu(sc.study),
        studyC.privateForbiddenFu(sc.study)
      )

  private def doApiShow(id: RelayRoundId)(using Context): Fu[Result] =
    limit.relay.apiGet(rateLimited):
      Found(api.byIdWithTour(id)): rt =>
        Found(env.study.studyRepo.byId(rt.round.studyId)): study =>
          studyC.CanView(study)(
            for
              group <- api.withTours.get(rt.tour.id)
              previews <- env.study.preview.jsonList.withoutInitialEmpty(study.id)
              targetRound <- api.officialTarget(rt.round)
              isSubscribed <- ctx.userId.traverse(api.isSubscribed(rt.tour.id, _))
              sVersion <- HTTPRequest.isLichessMobile.optionFu(env.study.version(study.id))
              photos <- playerApi.photosJson(rt.tour.id)
              _ = env.relay.stats.viewers.hit(rt)
              json = jsonView.withUrlAndPreviews(
                rt.withStudy(study),
                previews,
                group,
                targetRound,
                isSubscribed,
                sVersion,
                photos
              )
            yield Ok(json).headerCache(jsonView.responseCache(rt, study)).varyAccept
          )(studyC.privateUnauthorizedJson, studyC.privateForbiddenJson)

  def pgn(ts: String, rs: String, id: RelayRoundId) = Open:
    pgnWithFlags(ts, rs, id)

  def apiPgn(id: RelayRoundId) = AnonOrScoped(_.Study.Read): ctx ?=>
    pgnStream.parseExportDate(id) match
      case Some(since) if isGrantedOpt(_.StudyAdmin) =>
        Ok.chunked(pgnStream.exportFullMonth(since))
      case _ => pgnWithFlags("-", "-", id)

  private def pgnWithFlags(ts: String, rs: String, id: RelayRoundId)(using Context): Fu[Result] =
    studyC.pgnWithFlags(
      id.studyId,
      _.copy(
        updateTags = _ + Tag("GameURL", routeUrl(routes.RelayRound.show(ts, rs, id))),
        comments = false,
        variations = false
      )
    )

  def apiMyRounds = Scoped(_.Study.Read) { ctx ?=> _ ?=>
    val source = api.myRounds(MaxPerSecond(20), getIntAs[Max]("nb")).map(jsonView.myRound)
    limit.GlobalConcurrencyLimitPerIP.download(ctx.ip)(source)(jsToNdJson)
  }

  def apiSpotlightRounds = SecuredScoped(_.StudyAdmin) { ctx ?=> _ ?=>
    val source = api
      .spotlightRounds(MaxPerSecond(120), getIntAs[Max]("nb"), getTimestamp("since"), getTimestamp("until"))
      .map(jsonView.withSpotlight)
    limit.GlobalConcurrencyLimitPerIP.download(ctx.ip)(source)(jsToNdJson)
  }

  def streamRound(id: RelayRoundId) = AnonOrScoped(): ctx ?=>
    Found(api.byIdWithStudy(id)): rs =>
      studyC.CanView(rs.study) {
        pgnStreamResponse(pgnStream.streamRoundGames(rs))
      }(Unauthorized, Forbidden)

  def streamTour(id: RelayTourId) = AnonOrScoped(): ctx ?=>
    Found(api.tourById(id)): tour =>
      pgnStreamResponse(pgnStream.streamTourGames(tour))

  def streamGroup(id: RelayGroupId) = AnonOrScoped(): ctx ?=>
    Found(api.groupById(id)): group =>
      pgnStreamResponse(pgnStream.streamGroupGames(group))

  private def pgnStreamResponse(source: => Source[PgnStr, ?])(using Context) =
    limit.relay.stream()(source): limited =>
      Ok.chunked[PgnStr](limited.keepAlive(60.seconds, () => PgnStr(" "))).noProxyBuffer

  def chapter(ts: String, rs: String, id: RelayRoundId, chapterId: StudyChapterId) =
    Open:
      WithRoundAndTour(ts, rs, id, chapterId.some): rt =>
        Found(env.study.api.byIdWithChapterOrFallback(rt.round.studyId, chapterId)): study =>
          env.relay.videoEmbed.withCookie:
            doShow(rt, study, _)

  def push(id: RelayRoundId) = ScopedBody(parse.tolerantText)(Seq(_.Study.Write)) { ctx ?=> me ?=>
    limit.relay.push(me.userId, () => rateLimited): () =>
      Found(api.byIdWithTourAndStudy(id)): rt =>
        if !rt.study.canContribute(me) then forbiddenJson()
        else
          import lila.relay.RelayJsonView.given
          env.relay
            .push(rt.withTour, PgnStr(ctx.body.body))
            .map(JsonOk)
  }

  def teamsView(id: RelayRoundId) = Open:
    Found(api.byIdWithTourAndStudy(id)): rt =>
      studyC.CanView(rt.study) {
        rt.tour.teamTable.so:
          env.relay.teamTable.tableJson(rt.relay.id).map(JsonOk)
      }(Unauthorized, Forbidden)

  def stats(id: RelayRoundId) = Open:
    Found(api.byIdWithTour(id)): rt =>
      env.relay.stats.getJson(rt).map(Ok(_))

  private def WithRoundAndTour(
      @nowarn ts: String,
      @nowarn rs: String,
      id: RelayRoundId,
      chapterId: Option[StudyChapterId] = None
  )(
      f: RoundModel.WithTour => Fu[Result]
  )(using ctx: Context): Fu[Result] =
    Found(api.byIdWithTour(id)): rt =>
      if !ctx.req.path.startsWith(rt.path) && HTTPRequest.isRedirectable
      then Redirect(chapterId.fold(rt.call)(rt.call))
      else f(rt)

  private def WithTour(id: RelayTourId)(
      f: TourModel => Fu[Result]
  )(using Context): Fu[Result] =
    Found(api.tourById(id))(f)

  private def WithNavigationCanUpdate(id: RelayTourId)(
      f: FormNavigation => Fu[Result]
  )(using ctx: Context): Fu[Result] =
    WithTour(id): tour =>
      ctx
        .useMe(api.canUpdate(tour))
        .elseNotFound:
          api.formNavigation(tour).flatMap(f)

  private def doShow(rt: RoundModel.WithTour, oldSc: StudyModel.WithChapter, embed: VideoEmbed)(using
      ctx: Context
  ): Fu[Result] =
    studyC.CanView(oldSc.study)(
      for
        (sc, studyData) <- studyC.getJsonData(oldSc, withChapters = true)
        rounds <- api.byTourOrdered(rt.tour)
        group <- api.withTours.get(rt.tour.id)
        isSubscribed <- ctx.userId.traverse(api.isSubscribed(rt.tour.id, _))
        delayedUntil <- env.relay.delayedUntil(rt.round)
        videoUrls <- embed match
          case VideoEmbed.Stream(userId) =>
            env.streamer.api
              .find(userId)
              .flatMapz(s => env.streamer.liveApi.of(s).dmap(some))
              .map:
                _.flatMap(_.stream).map(_.urls.toPair(netDomain))
          case VideoEmbed.PinnedStream =>
            fuccess:
              rt.tour.pinnedStream
                .ifFalse(rt.round.isFinished)
                .flatMap(_.upstream)
                .map(_.urls.toPair(netDomain))
          case _ => fuccess(none)
        crossSiteIsolation = videoUrls.isEmpty || (
          rt.tour.pinnedStream.isDefined && crossOriginPolicy.supportsCredentiallessIFrames(ctx.req)
        )
        photos <- playerApi.photosJson(rt.tour.id)
        data = jsonView.makeData(
          rt.tour.withRounds(rounds.map(_.round)),
          rt.round.id,
          studyData,
          group,
          ctx.userId.exists(sc.study.canContribute),
          isSubscribed,
          videoUrls,
          rt.tour.pinnedStream,
          delayedUntil,
          photos
        )
        chat <- NoCrawlers(studyC.chatOf(sc.study))
        sVersion <- NoCrawlers(env.study.version(sc.study.id))
        page <- renderPage:
          views.relay.show(rt.withStudy(sc.study), data, chat, sVersion, crossSiteIsolation)
      yield
        env.relay.stats.viewers.hit(rt)
        if crossSiteIsolation then Ok(page).enforceCrossSiteIsolation
        else Ok(page).withHeaders(crossOriginPolicy.unsafe*)
    )(
      studyC.privateUnauthorizedFu(oldSc.study),
      studyC.privateForbiddenFu(oldSc.study)
    )
