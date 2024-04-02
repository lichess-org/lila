package controllers

import chess.format.pgn.{ PgnStr, Tag }
import play.api.data.Form
import play.api.libs.json.{ Json, OWrites }
import play.api.mvc.*
import views.*

import scala.annotation.nowarn

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.relay.{ RelayRound as RoundModel, RelayTour as TourModel }

final class RelayRound(
    env: Env,
    studyC: => Study,
    apiC: => Api
) extends LilaController(env):

  def form(tourId: TourModel.Id) = Auth { ctx ?=> _ ?=>
    NoLameOrBot:
      WithTourAndRoundsCanUpdate(tourId): trs =>
        Ok.page:
          html.relay.roundForm.create(env.relay.roundForm.create(trs), trs.tour)
  }

  def create(tourId: TourModel.Id) = AuthOrScopedBody(_.Study.Write) { ctx ?=> me ?=>
    NoLameOrBot:
      WithTourAndRoundsCanUpdate(tourId): trs =>
        val tour = trs.tour
        def whenRateLimited = negotiate(
          Redirect(routes.RelayTour.show(tour.slug, tour.id.value)),
          rateLimited
        )
        env.relay.roundForm
          .create(trs)
          .bindFromRequest()
          .fold(
            err =>
              negotiate(
                BadRequest.page(html.relay.roundForm.create(err, tour)),
                jsonFormError(err)
              ),
            setup =>
              rateLimitCreation(whenRateLimited):
                env.relay.api
                  .create(setup, tour)
                  .flatMap: rt =>
                    negotiate(
                      Redirect(routes.RelayRound.show(tour.slug, rt.relay.slug, rt.relay.id)),
                      JsonOk(env.relay.jsonView.myRound(rt))
                    )
          )
  }

  def edit(id: RelayRoundId) = Auth { ctx ?=> me ?=>
    FoundPage(env.relay.api.byIdAndContributor(id)): rt =>
      html.relay.roundForm.edit(rt, env.relay.roundForm.edit(rt.round))
  }

  def update(id: RelayRoundId) = AuthOrScopedBody(_.Study.Write) { ctx ?=> me ?=>
    env.relay.api
      .byIdAndContributor(id)
      .flatMapz { rt =>
        env.relay.roundForm
          .edit(rt.round)
          .bindFromRequest()
          .fold(
            err => fuccess(Left(rt -> err)),
            data =>
              env.relay.api
                .update(rt.round)(data.update)
                .dmap(_.withTour(rt.tour))
                .dmap(Right(_))
          )
          .dmap(some)
      }
      .orNotFound:
        _.fold(
          (old, err) =>
            negotiate(
              BadRequest.page(html.relay.roundForm.edit(old, err)),
              jsonFormError(err)
            ),
          rt => negotiate(Redirect(rt.path), JsonOk(env.relay.jsonView.withUrl(rt, withTour = true)))
        )
  }

  def reset(id: RelayRoundId) = Auth { ctx ?=> me ?=>
    Found(env.relay.api.byIdAndContributor(id)): rt =>
      env.relay.api.reset(rt.round).inject(Redirect(rt.path))
  }

  def show(ts: String, rs: String, id: RelayRoundId, embed: Option[UserStr]) =
    OpenOrScoped(_.Study.Read): ctx ?=>
      negotiate(
        html = WithRoundAndTour(ts, rs, id): rt =>
          val sc = env.study.preview
            .firstId(rt.round.studyId)
            .flatMap:
              // there might be no chapter after a round reset, let a new one be created
              case None              => env.study.api.byIdWithChapter(rt.round.studyId)
              case Some(firstChapId) => env.study.api.byIdWithChapterOrFallback(rt.round.studyId, firstChapId)
          sc.orNotFound { doShow(rt, _, embed) }
        ,
        json = doApiShow(id)
      )

  def apiShow(ts: String, rs: String, id: RelayRoundId) = AnonOrScoped(_.Study.Read):
    doApiShow(id)

  private def doApiShow(id: RelayRoundId)(using Context): Fu[Result] =
    Found(env.relay.api.byIdWithTour(id)): rt =>
      Found(env.study.studyRepo.byId(rt.round.studyId)): study =>
        studyC.CanView(study)(
          env.study.preview
            .jsonList(study.id)
            .map: previews =>
              JsonOk(env.relay.jsonView.withUrlAndPreviews(rt.withStudy(study), previews))
        )(studyC.privateUnauthorizedJson, studyC.privateForbiddenJson)

  def pgn(ts: String, rs: String, id: StudyId) = studyC.pgn(id)
  def apiPgn                                   = studyC.apiPgn

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

  def chapter(ts: String, rs: String, id: RelayRoundId, chapterId: StudyChapterId, embed: Option[UserStr]) =
    Open:
      WithRoundAndTour(ts, rs, id): rt =>
        env.study.api.byIdWithChapterOrFallback(rt.round.studyId, chapterId).orNotFound {
          doShow(rt, _, embed)
        }

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

  private def WithRoundAndTour(@nowarn ts: String, @nowarn rs: String, id: RelayRoundId)(
      f: RoundModel.WithTour => Fu[Result]
  )(using ctx: Context): Fu[Result] =
    Found(env.relay.api.byIdWithTour(id)): rt =>
      if !ctx.req.path.startsWith(rt.path) && HTTPRequest.isRedirectable(ctx.req)
      then Redirect(rt.path)
      else f(rt)

  private def WithTour(id: String)(
      f: TourModel => Fu[Result]
  )(using Context): Fu[Result] =
    Found(env.relay.api.tourById(TourModel.Id(id)))(f)

  private def WithTourAndRoundsCanUpdate(id: String)(
      f: TourModel.WithRounds => Fu[Result]
  )(using ctx: Context): Fu[Result] =
    WithTour(id): tour =>
      ctx.me
        .soUse { env.relay.api.canUpdate(tour) }
        .elseNotFound:
          env.relay.api.withRounds(tour).flatMap(f)

  private def doShow(rt: RoundModel.WithTour, oldSc: lila.study.Study.WithChapter, embed: Option[UserStr])(
      using ctx: Context
  ): Fu[Result] =
    studyC.CanView(oldSc.study)(
      for
        (sc, studyData) <- studyC.getJsonData(oldSc)
        rounds          <- env.relay.api.byTourOrdered(rt.tour)
        group           <- env.relay.api.withTours.get(rt.tour.id)
        isSubscribed <- ctx.me.soFu: me =>
          env.relay.api.isSubscribed(rt.tour.id, me.userId)
        pinnedStreamer <- rt.tour.pinnedStreamer.so(env.streamer.api.find)
        streamer       <- embed.so(env.streamer.api.find)
        stream         <- streamer.soFu(env.streamer.liveStreamApi.of)
        videoUrls =
          if embed.contains("fake") then
            lila.streamer.Stream
              .Urls(s"https://www.youtube.com/embed/zeo3AmLAuZc?autoplay=1&disablekb=1&color=white", "")
              .some
          else stream.flatMap(_.stream).map(_.urls(netDomain))
        crossSiteIsolation = videoUrls.isEmpty
        data = env.relay.jsonView.makeData(
          rt.tour.withRounds(rounds.map(_.round)),
          rt.round.id,
          studyData,
          group,
          ctx.userId.exists(sc.study.canContribute),
          isSubscribed,
          videoUrls.map(_.toPair),
          pinnedStreamer.map(s => (s.user.id, s.streamer.name.value, rt.tour.pinnedStreamerImage))
        )
        chat     <- NoCrawlers(studyC.chatOf(sc.study))
        sVersion <- NoCrawlers(env.study.version(sc.study.id))
        page <- renderPage:
          html.relay.show(rt.withStudy(sc.study), data, chat, sVersion, crossSiteIsolation)
        _ = if HTTPRequest.isHuman(req) then lila.mon.http.path(rt.tour.path).increment()
      yield if crossSiteIsolation then Ok(page).enforceCrossSiteIsolation else Ok(page)
    )(
      studyC.privateUnauthorizedFu(oldSc.study),
      studyC.privateForbiddenFu(oldSc.study)
    )

  private val CreateLimitPerUser = lila.memo.RateLimit[UserId](
    credits = 100 * 10,
    duration = 24.hour,
    key = "broadcast.round.user"
  )

  private val CreateLimitPerIP = lila.memo.RateLimit[lila.core.IpAddress](
    credits = 100 * 10,
    duration = 24.hour,
    key = "broadcast.round.ip"
  )

  private[controllers] def rateLimitCreation(fail: => Fu[Result])(
      create: => Fu[Result]
  )(using me: Me, req: RequestHeader): Fu[Result] =
    val cost =
      if isGranted(_.StudyAdmin) then 1
      else if isGranted(_.Relay) then 2
      else if me.hasTitle || me.isVerified then 5
      else 10
    CreateLimitPerUser(me, fail, cost = cost):
      CreateLimitPerIP(req.ipAddress, fail, cost = cost):
        create
