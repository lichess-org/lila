package controllers

import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.i18n.Lang

import chess.format.Fen

import lila.app.{ given, * }
import lila.common.{ IpAddress, HTTPRequest }
import lila.game.{ AnonCookie, Pov }
import lila.rating.Glicko
import lila.setup.Processor.HookResult
import lila.setup.ValidFen
import lila.socket.Socket.Sri
import views.*
import play.api.mvc.Result

final class Setup(
    env: Env,
    challengeC: => Challenge,
    apiC: => Api
) extends LilaController(env)
    with TheftPrevention:

  private def forms     = env.setup.forms
  private def processor = env.setup.processor

  private[controllers] val PostRateLimit = lila.memo.RateLimit[IpAddress](
    5,
    1.minute,
    key = "setup.post",
    enforce = env.net.rateLimit.value,
    log = false
  )

  private[controllers] val AnonHookRateLimit = lila.memo.RateLimit.composite[IpAddress](
    key = "setup.hook.anon",
    enforce = env.net.rateLimit.value
  )(
    ("fast", 8, 1.minute),
    ("slow", 300, 1.day)
  )

  private[controllers] val BotAiRateLimit = lila.memo.RateLimit[UserId](
    50,
    1.day,
    key = "setup.post.bot.ai"
  )

  def ai = OpenBody:
    BotAiRateLimit(ctx.userId | UserId(""), rateLimitedFu, cost = ctx.me.exists(_.isBot) so 1):
      PostRateLimit(ctx.ip, rateLimitedFu):
        forms.ai
          .bindFromRequest()
          .fold(
            doubleJsonFormError,
            config =>
              processor.ai(config) flatMap { pov =>
                negotiateApi(
                  html = redirectPov(pov),
                  api = apiVersion =>
                    env.api.roundApi.player(pov, none, apiVersion) map { data =>
                      Created(data) as JSON
                    }
                )
              }
          )

  def friend(userId: Option[UserStr]) =
    OpenBody:
      PostRateLimit(ctx.ip, rateLimitedFu):
        forms.friend
          .bindFromRequest()
          .fold(
            doubleJsonFormError,
            config =>
              userId so env.user.repo.enabledById flatMap { destUser =>
                destUser so { env.challenge.granter.isDenied(ctx.me, _, config.perfType) } flatMap {
                  case Some(denied) =>
                    val message = lila.challenge.ChallengeDenied.translated(denied)
                    negotiate(
                      // 403 tells setupCtrl.ts to close the setup modal
                      Forbidden(jsonError(message)), // TODO test
                      BadRequest(jsonError(message))
                    )
                  case None =>
                    import lila.challenge.Challenge.*
                    val timeControl = TimeControl.make(config.makeClock, config.makeDaysPerTurn)
                    val challenge = lila.challenge.Challenge.make(
                      variant = config.variant,
                      initialFen = config.fen,
                      timeControl = timeControl,
                      mode = config.mode,
                      color = config.color.name,
                      challenger = (ctx.me, ctx.req.sid) match
                        case (Some(user), _) => toRegistered(config.variant, timeControl)(user)
                        case (_, Some(sid))  => Challenger.Anonymous(sid)
                        case _               => Challenger.Open
                      ,
                      destUser = destUser,
                      rematchOf = none
                    )
                    env.challenge.api create challenge flatMap {
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
                    }
                }
              }
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
              PostRateLimit(req.ipAddress, rateLimitedFu):
                AnonHookRateLimit(req.ipAddress, rateLimitedFu, cost = ctx.isAnon so 1):
                  (ctx.userId so env.relation.api.fetchBlocking) flatMap { blocking =>
                    processor.hook(
                      userConfig withinLimits ctx.me,
                      sri,
                      req.sid,
                      lila.pool.Blocking(blocking)
                    ) map hookResponse
                  }
          )

  def like(sri: Sri, gameId: GameId) = Open:
    NoBot:
      PostRateLimit(ctx.ip, rateLimitedFu):
        NoPlaybanOrCurrent:
          Found(env.game.gameRepo game gameId): game =>
            for
              blocking <- ctx.userId so env.relation.api.fetchBlocking
              hookConfig = lila.setup.HookConfig.default(ctx.isAuth)
              hookConfigWithRating = get("rr").fold(
                hookConfig.withRatingRange(
                  ctx.me.fold(Glicko.default.intRating.some)(_.perfs.ratingOf(game.perfKey)),
                  get("deltaMin"),
                  get("deltaMax")
                )
              )(hookConfig.withRatingRange) updateFrom game
              allBlocking = lila.pool.Blocking(blocking ++ game.userIds)
              hookResult <- processor.hook(hookConfigWithRating, sri, ctx.req.sid, allBlocking)
            yield hookResponse(hookResult)

  private val BoardApiHookConcurrencyLimitPerUserOrSri = lila.memo.ConcurrencyLimit[Either[Sri, UserId]](
    name = "Board API hook Stream API concurrency per user",
    key = "boardApiHook.concurrency.limit.user",
    ttl = 10.minutes,
    maxConcurrency = 1
  )
  def boardApiHook = AnonOrScopedBody(parse.anyContent)(_.Board.Play, _.Web.Mobile): ctx ?=>
    NoBot:
      val reqSri = getAs[Sri]("sri")
      val author: Either[Result, Either[Sri, lila.user.User]] = ctx.me match
        case Some(u) => Right(Right(u))
        case None =>
          reqSri match
            case Some(sri) => Right(Left(sri))
            case None      => Left(BadRequest(jsonError("Authentication required")))
      author match
        case Left(err) => err.toFuccess
        case Right(author) =>
          forms
            .boardApiHook:
              ctx.isMobileOauth || (ctx.isAnon && HTTPRequest.isLichessMobile(ctx.req))
            .bindFromRequest()
            .fold(
              doubleJsonFormError,
              config =>
                ctx.me.so(env.relation.api.fetchBlocking(_)).flatMap { blocking =>
                  val uniqId = author.fold(_.value, u => s"sri:${u.id}")
                  config.fixColor
                    .hook(reqSri | Sri(uniqId), ctx.me, sid = uniqId.some, lila.pool.Blocking(blocking)) match
                    case Left(hook) =>
                      PostRateLimit(req.ipAddress, rateLimitedFu):
                        BoardApiHookConcurrencyLimitPerUserOrSri(author.map(_.id))(
                          env.lobby.boardApiHookStream(hook.copy(boardApi = true))
                        )(apiC.sourceToNdJsonOption)
                    case Right(Some(seek)) =>
                      author match
                        case Left(_) => BadRequest(jsonError("Anonymous users cannot create seeks"))
                        case Right(me) =>
                          env.setup.processor.createSeekIfAllowed(seek)(using me) map {
                            case HookResult.Refused =>
                              BadRequest(Json.obj("error" -> "Already playing too many games"))
                            case HookResult.Created(id) => Ok(Json.obj("id" -> id))
                          }
                    case Right(None) => notFoundJson()

                }
            )

  def filterForm = Open:
    Ok.page(html.setup.filter(forms.filter))

  def validateFen = Open:
    (get("fen").map(Fen.Epd.clean): Option[Fen.Epd]) flatMap ValidFen(getBool("strict")) match
      case None    => BadRequest
      case Some(v) => Ok.page(html.board.bits.miniSpan(v.fen.board, v.color))

  def apiAi = ScopedBody(_.Challenge.Write, _.Bot.Play, _.Board.Play, _.Web.Mobile) { ctx ?=> me ?=>
    BotAiRateLimit(me, rateLimitedFu, cost = me.isBot so 1):
      PostRateLimit(req.ipAddress, rateLimitedFu):
        forms.api.ai
          .bindFromRequest()
          .fold(
            doubleJsonFormError,
            config =>
              processor.apiAi(config).map { pov =>
                Created(env.game.jsonView.baseWithChessDenorm(pov.game, config.fen)) as JSON
              }
          )
  }

  private[controllers] def redirectPov(pov: Pov)(using ctx: Context) =
    val redir = Redirect(routes.Round.watcher(pov.gameId.value, "white"))
    if ctx.isAuth then redir
    else
      redir withCookies env.lilaCookie.cookie(
        AnonCookie.name,
        pov.playerId.value,
        maxAge = AnonCookie.maxAge.some,
        httpOnly = false.some
      )
