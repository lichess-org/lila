package controllers

import chess.format.Fen
import play.api.libs.json.Json
import play.api.mvc.{ Request, Result, EssentialAction }

import lila.app.{ *, given }
import lila.core.net.IpAddress
import lila.common.HTTPRequest
import lila.game.{ AnonCookie, Pov }
import lila.memo.RateLimit

import lila.setup.Processor.HookResult
import lila.setup.ValidFen
import lila.core.socket.Sri
import lila.game.GameExt.perfType

final class Setup(
    env: Env,
    challengeC: => Challenge,
    apiC: => Api
) extends LilaController(env)
    with lila.web.TheftPrevention:

  private def forms     = env.setup.forms
  private def processor = env.setup.processor

  private[controllers] val PostRateLimit = RateLimit[IpAddress](
    5,
    1.minute,
    key = "setup.post",
    enforce = env.net.rateLimit.value,
    log = false
  )

  private[controllers] val AnonHookRateLimit = RateLimit.composite[IpAddress](
    key = "setup.hook.anon",
    enforce = env.net.rateLimit.value
  )(
    ("fast", 8, 1.minute),
    ("slow", 300, 1.day)
  )

  private[controllers] val BotAiRateLimit = RateLimit[UserId](50, 1.day, key = "setup.post.bot.ai")

  def ai = OpenBody:
    BotAiRateLimit(ctx.userId | UserId(""), rateLimited, cost = ctx.me.exists(_.isBot).so(1)):
      PostRateLimit(ctx.ip, rateLimited):
        forms.ai
          .bindFromRequest()
          .fold(
            doubleJsonFormError,
            config =>
              processor.ai(config).flatMap { pov =>
                negotiateApi(
                  html = redirectPov(pov),
                  api = _ => env.api.roundApi.player(pov, lila.core.data.Preload.none, none).map(Created(_))
                )
              }
          )

  def friend(userId: Option[UserStr]) =
    OpenBody: ctx ?=>
      PostRateLimit(ctx.ip, rateLimited):
        forms.friend
          .bindFromRequest()
          .fold(
            doubleJsonFormError,
            config =>
              for
                origUser <- ctx.user.soFu(env.user.perfsRepo.withPerf(_, config.perfType))
                destUser <- userId.so(env.user.api.enabledWithPerf(_, config.perfType))
                denied   <- destUser.so(u => env.challenge.granter.isDenied(u.user, config.perfType))
                result <- denied match
                  case Some(denied) =>
                    val message = lila.challenge.ChallengeDenied.translated(denied)
                    negotiate(
                      // 403 tells setupCtrl.ts to close the setup modal
                      forbiddenJson(message), // TODO test
                      BadRequest(jsonError(message))
                    )
                  case None =>
                    import lila.challenge.Challenge.*
                    (origUser, ctx.req.sid)
                      .match
                        case (Some(orig), _)                       => toRegistered(orig).some
                        case (_, Some(sid))                        => Challenger.Anonymous(sid).some
                        case _ if HTTPRequest.isLichobile(ctx.req) => Challenger.Open.some
                        case _                                     => none
                      .so: challenger =>
                        val timeControl = makeTimeControl(config.makeClock, config.makeDaysPerTurn)
                        val challenge = lila.challenge.Challenge.make(
                          variant = config.variant,
                          initialFen = config.fen,
                          timeControl = timeControl,
                          mode = config.mode,
                          color = config.color.name,
                          challenger = challenger,
                          destUser = destUser,
                          rematchOf = none
                        )
                        env.challenge.api
                          .create(challenge)
                          .flatMap:
                            if _ then
                              negotiate(
                                Redirect(routes.Round.watcher(challenge.id, "white")),
                                challengeC.showChallenge(challenge, justCreated = true)
                              )
                            else
                              negotiate(
                                Redirect(routes.Lobby.home),
                                BadRequest(jsonError("Challenge not created"))
                              )
              yield result
          )

  private def hookResponse(res: HookResult) = res match
    case HookResult.Created(id) =>
      JsonOk:
        Json.obj(
          "ok"   -> true,
          "hook" -> Json.obj("id" -> id)
        )
    case HookResult.Refused => BadRequest(jsonError("Game was not created"))

  def hook(sri: Sri) = OpenOrScopedBody(parse.anyContent)(_.Web.Mobile): ctx ?=>
    NoBot:
      NoPlaybanOrCurrent:
        forms.hook
          .bindFromRequest()
          .fold(
            doubleJsonFormError,
            userConfig =>
              PostRateLimit(req.ipAddress, rateLimited):
                AnonHookRateLimit(req.ipAddress, rateLimited, cost = ctx.isAnon.so(1)):
                  for
                    me <- ctx.user.soFu(env.user.api.withPerfs)
                    given Perf = me.fold(lila.rating.Perf.default)(_.perfs(userConfig.perfType))
                    blocking <- ctx.userId.so(env.relation.api.fetchBlocking)
                    res <- processor.hook(
                      userConfig.withinLimits,
                      sri,
                      req.sid,
                      lila.core.pool.Blocking(blocking)
                    )(using me)
                  yield hookResponse(res)
          )

  def like(sri: Sri, gameId: GameId) = Open:
    NoBot:
      PostRateLimit(ctx.ip, rateLimited):
        NoPlaybanOrCurrent:
          Found(env.game.gameRepo.game(gameId)): game =>
            for
              orig     <- ctx.user.soFu(env.user.api.withPerfs)
              blocking <- ctx.userId.so(env.relation.api.fetchBlocking)
              hookConfig = lila.setup.HookConfig.default(ctx.isAuth)
              hookConfigWithRating = get("rr")
                .fold(
                  hookConfig.withRatingRange(
                    orig.fold(lila.rating.Perf.default)(_.perfs(game.perfKey)).intRating.some,
                    get("deltaMin"),
                    get("deltaMax")
                  )
                )(hookConfig.withRatingRange)
                .updateFrom(game)
              allBlocking = lila.core.pool.Blocking(blocking ++ game.userIds)
              hookResult <- processor.hook(hookConfigWithRating, sri, ctx.req.sid, allBlocking)(using orig)
            yield hookResponse(hookResult)

  private val BoardApiHookConcurrencyLimitPerUserOrSri = lila.web.ConcurrencyLimit[Either[Sri, UserId]](
    name = "Board API hook Stream API concurrency per user",
    key = "boardApiHook.concurrency.limit.user",
    ttl = 10.minutes,
    maxConcurrency = 1
  )

  def boardApiHook = WithBoardApiHookAuthor { (author, reqSri) => ctx ?=>
    forms
      .boardApiHook:
        ctx.isMobileOauth || (ctx.isAnon && HTTPRequest.isLichessMobile(ctx.req))
      .bindFromRequest()
      .fold(
        doubleJsonFormError,
        config =>
          for
            me       <- ctx.me.so(env.user.api.withPerfs)
            blocking <- ctx.me.so(env.relation.api.fetchBlocking(_))
            uniqId = author.fold(_.value, u => s"sri:${u.id}")
            res <- config.fixColor
              .hook(reqSri | Sri(uniqId), me, sid = uniqId.some, lila.core.pool.Blocking(blocking))
              .match
                case Left(hook) =>
                  PostRateLimit(req.ipAddress, rateLimited):
                    BoardApiHookConcurrencyLimitPerUserOrSri(author.map(_.id))(
                      env.lobby.boardApiHookStream(hook.copy(boardApi = true))
                    )(jsOptToNdJson).toFuccess
                case Right(Some(seek)) =>
                  author match
                    case Left(_) =>
                      BadRequest(jsonError("Anonymous users cannot create seeks")).toFuccess
                    case Right(me) =>
                      env.setup.processor.createSeekIfAllowed(seek, me.id).map {
                        case HookResult.Refused =>
                          BadRequest(Json.obj("error" -> "Already playing too many games"))
                        case HookResult.Created(id) => Ok(Json.obj("id" -> id))
                      }
                case Right(None) => notFoundJson().toFuccess
          yield res
      )
  }

  def boardApiHookCancel = WithBoardApiHookAuthor { (_, reqSri) => _ ?=>
    reqSri.so: sri =>
      env.lobby.boardApiHookStream.cancel(sri)
      NoContent
  }

  private def WithBoardApiHookAuthor(
      f: (Either[Sri, lila.user.User], Option[Sri]) => BodyContext[?] ?=> Fu[Result]
  ): EssentialAction =
    AnonOrScopedBody(parse.anyContent)(_.Board.Play, _.Web.Mobile): ctx ?=>
      NoBot:
        val reqSri = getAs[Sri]("sri")
        ctx.me match
          case Some(u) => f(Right(u), reqSri)
          case None =>
            reqSri match
              case Some(sri) => f(Left(sri), reqSri)
              case None      => BadRequest(jsonError("Authentication required"))

  def filterForm = Open:
    Ok.page(views.setup.filter(forms.filter))

  def validateFen = Open:
    (get("fen").map(Fen.Full.clean): Option[Fen.Full]).flatMap(ValidFen(getBool("strict"))) match
      case None    => BadRequest
      case Some(v) => Ok.page(views.board.bits.miniSpan(v.fen.board, v.color))

  def apiAi = ScopedBody(_.Challenge.Write, _.Bot.Play, _.Board.Play, _.Web.Mobile) { ctx ?=> me ?=>
    BotAiRateLimit(me, rateLimited, cost = me.isBot.so(1)):
      PostRateLimit(req.ipAddress, rateLimited):
        forms.api.ai
          .bindFromRequest()
          .fold(
            doubleJsonFormError,
            config =>
              processor.apiAi(config).map { pov =>
                Created(env.game.jsonView.baseWithChessDenorm(pov.game, config.fen)).as(JSON)
              }
          )
  }

  private[controllers] def redirectPov(pov: Pov)(using ctx: Context) =
    val redir = Redirect(routes.Round.watcher(pov.gameId.value, "white"))
    if ctx.isAuth then redir
    else
      redir.withCookies(
        env.security.lilaCookie.cookie(
          AnonCookie.name,
          pov.playerId.value,
          maxAge = AnonCookie.maxAge.some,
          httpOnly = false.some
        )
      )
