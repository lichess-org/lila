package controllers

import play.api.data.Form
import play.api.mvc.*

import lila.api.WebContext
import lila.app.{ given, * }

// import lila.common.config.MaxPerSecond
import lila.relay.{ RelayRound as RoundModel, RelayRoundForm, RelayTour as TourModel }
import lila.user.{ User as UserModel }
import views.*
import chess.format.pgn.PgnStr
import scala.annotation.nowarn

final class RelayRound(
    env: Env,
    studyC: => Study,
    apiC: => Api
) extends LilaController(env):

  def form(tourId: String) = Auth { ctx ?=> _ =>
    NoLameOrBot:
      WithTourAndRoundsCanUpdate(tourId): trs =>
        html.relay.roundForm.create(env.relay.roundForm.create(trs), trs.tour)
  }

  def create(tourId: String) =
    AuthOrScopedBody(_.Study.Write)(
      auth = ctx ?=>
        me =>
          NoLameOrBot:
            WithTourAndRoundsCanUpdate(tourId): trs =>
              val tour = trs.tour
              env.relay.roundForm
                .create(trs)
                .bindFromRequest()
                .fold(
                  err => BadRequest(html.relay.roundForm.create(err, tour)),
                  setup =>
                    rateLimitCreation(
                      me,
                      ctx.req,
                      Redirect(routes.RelayTour.redirectOrApiTour(tour.slug, tour.id.value))
                    ):
                      env.relay.api.create(setup, me, tour) map { round =>
                        Redirect(routes.RelayRound.show(tour.slug, round.slug, round.id.value))
                      }
                )
      ,
      scoped = ctx ?=>
        me =>
          NoLameOrBot(me):
            env.relay.api tourById TourModel.Id(tourId) flatMapz { tour =>
              env.relay.api.withRounds(tour) flatMap { trs =>
                env.relay.roundForm
                  .create(trs)
                  .bindFromRequest()
                  .fold(
                    err => BadRequest(apiFormError(err)),
                    setup =>
                      rateLimitCreation(me, ctx.req, rateLimited):
                        JsonOk:
                          env.relay.api.create(setup, me, tour) map { round =>
                            env.relay.jsonView.withUrl(round withTour tour)
                          }
                  )
              }
            }
    )

  def edit(id: RelayRoundId) = Auth { ctx ?=> me =>
    OptionFuResult(env.relay.api.byIdAndContributor(id, me)): rt =>
      html.relay.roundForm.edit(rt, env.relay.roundForm.edit(rt.round))
  }

  def update(id: RelayRoundId) =
    AuthOrScopedBody(_.Study.Write)(
      auth = ctx ?=>
        me =>
          doUpdate(id, me).flatMapz: res =>
            res.fold(
              (old, err) => BadRequest(html.relay.roundForm.edit(old, err)),
              rt => Redirect(rt.path)
            ),
      scoped = ctx ?=>
        me =>
          doUpdate(id, me) map {
            case None => NotFound(jsonError("No such broadcast"))
            case Some(res) =>
              res.fold(
                (_, err) => BadRequest(apiFormError(err)),
                rt => JsonOk(env.relay.jsonView.withUrl(rt))
              )
          }
    )

  private def doUpdate(id: RelayRoundId, me: UserModel)(using
      req: Request[?]
  ): Fu[Option[Either[(RoundModel.WithTour, Form[RelayRoundForm.Data]), RoundModel.WithTour]]] =
    env.relay.api.byIdAndContributor(id, me) flatMapz { rt =>
      env.relay.roundForm
        .edit(rt.round)
        .bindFromRequest()
        .fold(
          err => fuccess(Left(rt -> err)),
          data =>
            env.relay.api.update(rt.round) { data.update(_, me) }.dmap(_ withTour rt.tour) dmap Right.apply
        ) dmap some
    }

  def reset(id: RelayRoundId) = Auth { ctx ?=> me =>
    OptionFuResult(env.relay.api.byIdAndContributor(id, me)) { rt =>
      env.relay.api.reset(rt.round, me) inject Redirect(rt.path)
    }
  }

  def show(ts: String, rs: String, id: RelayRoundId) =
    OpenOrScoped(_.Study.Read)(
      open = ctx ?=>
        pageHit
        WithRoundAndTour(ts, rs, id): rt =>
          val sc =
            if rt.round.sync.ongoing then
              env.study.chapterRepo relaysAndTagsByStudyId rt.round.studyId flatMap { chapters =>
                chapters.find(_.looksAlive) orElse chapters.headOption match {
                  case Some(chapter) => env.study.api.byIdWithChapterOrFallback(rt.round.studyId, chapter.id)
                  case None          => env.study.api byIdWithChapter rt.round.studyId
                }
              }
            else env.study.api byIdWithChapter rt.round.studyId
          sc flatMapz { doShow(rt, _) }
      ,
      scoped = _ ?=>
        env.relay.api.byIdWithTour(id) flatMapz { rt =>
          env.study.chapterRepo orderedMetadataByStudy rt.round.studyId map { games =>
            JsonOk(env.relay.jsonView.withUrlAndGames(rt, games))
          }
        }
    )

  def pgn(ts: String, rs: String, id: StudyId) = studyC.pgn(id)
  def apiPgn(id: StudyId)                      = studyC.apiPgn(id)

  def stream(id: RelayRoundId) = AnonOrScoped(): ctx ?=>
    env.relay.api.byIdWithStudy(id) flatMapz { rt =>
      studyC.CanView(rt.study, ctx.me) {
        apiC.GlobalConcurrencyLimitPerIP
          .events(req.ipAddress)(env.relay.pgnStream.streamRoundGames(rt)): source =>
            noProxyBuffer(Ok.chunked[PgnStr](source.keepAlive(60.seconds, () => PgnStr(" "))))
      }(Unauthorized, Forbidden)
    }

  def chapter(ts: String, rs: String, id: RelayRoundId, chapterId: StudyChapterId) = Open:
    WithRoundAndTour(ts, rs, id): rt =>
      env.study.api.byIdWithChapterOrFallback(rt.round.studyId, chapterId) flatMapz { doShow(rt, _) }

  def push(id: RelayRoundId) = ScopedBody(parse.tolerantText)(Seq(_.Study.Write)) { ctx ?=> me =>
    env.relay.api
      .byIdAndContributor(id, me)
      .flatMap:
        case None     => notFoundJson()
        case Some(rt) => env.relay.push(rt, PgnStr(ctx.body.body)) inject jsonOkResult
  }

  private def WithRoundAndTour(@nowarn ts: String, @nowarn rs: String, id: RelayRoundId)(
      f: RoundModel.WithTour => Fu[Result]
  )(using ctx: WebContext): Fu[Result] =
    OptionFuResult(env.relay.api byIdWithTour id): rt =>
      if !ctx.req.path.startsWith(rt.path)
      then Redirect(rt.path)
      else f(rt)

  private def WithTour(id: String)(
      f: TourModel => Fu[Result]
  )(using WebContext): Fu[Result] =
    OptionFuResult(env.relay.api tourById TourModel.Id(id))(f)

  private def WithTourAndRoundsCanUpdate(id: String)(
      f: TourModel.WithRounds => Fu[Result]
  )(using ctx: WebContext): Fu[Result] =
    WithTour(id) { tour =>
      ctx.me.so { env.relay.api.canUpdate(_, tour) } flatMapz {
        env.relay.api withRounds tour flatMap f
      }
    }

  private def doShow(rt: RoundModel.WithTour, oldSc: lila.study.Study.WithChapter)(using
      ctx: WebContext
  ): Fu[Result] =
    studyC.CanView(oldSc.study, ctx.me) {
      for
        (sc, studyData) <- studyC.getJsonData(oldSc)
        rounds          <- env.relay.api.byTourOrdered(rt.tour)
        data <- env.relay.jsonView.makeData(
          rt.tour withRounds rounds.map(_.round),
          rt.round.id,
          studyData,
          ctx.userId exists sc.study.canContribute
        )
        chat      <- studyC.chatOf(sc.study)
        sVersion  <- env.study.version(sc.study.id)
        streamers <- studyC.streamersOf(sc.study)
      yield Ok(
        html.relay.show(rt withStudy sc.study, data, chat, sVersion, streamers)
      ).enableSharedArrayBuffer
    }(studyC.privateUnauthorizedFu(oldSc.study), studyC.privateForbiddenFu(oldSc.study))

  private val CreateLimitPerUser = lila.memo.RateLimit[UserId](
    credits = 100 * 10,
    duration = 24.hour,
    key = "broadcast.round.user"
  )

  private val CreateLimitPerIP = lila.memo.RateLimit[lila.common.IpAddress](
    credits = 100 * 10,
    duration = 24.hour,
    key = "broadcast.round.ip"
  )

  private[controllers] def rateLimitCreation(
      me: UserModel,
      req: RequestHeader,
      fail: => Fu[Result]
  )(create: => Fu[Result]): Fu[Result] =
    val cost =
      if isGranted(_.Relay, me) then 2
      else if me.hasTitle || me.isVerified then 5
      else 10
    CreateLimitPerUser(me.id, fail, cost = cost):
      CreateLimitPerIP(req.ipAddress, fail, cost = cost):
        create
