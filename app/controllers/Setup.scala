package controllers

import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.i18n.Lang

import chess.format.Fen
import lila.api.context.*
import lila.app.{ given, * }
import lila.common.IpAddress
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
            jsonFormError,
            config =>
              processor.ai(config) flatMap { pov =>
                negotiate(
                  html = fuccess(redirectPov(pov)),
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
            jsonFormError,
            config =>
              userId so env.user.repo.enabledById flatMap { destUser =>
                destUser so { env.challenge.granter.isDenied(ctx.me, _, config.perfType) } flatMap {
                  case Some(denied) =>
                    val message = lila.challenge.ChallengeDenied.translated(denied)
                    negotiate(
                      html = Forbidden(jsonError(message)).toFuccess,
                      // 403 tells setupCtrl.ts to close the setup modal
                      api = _ => BadRequest(jsonError(message)).toFuccess
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
                      challenger = (ctx.me, ctx.req.sid) match {
                        case (Some(user), _) => toRegistered(config.variant, timeControl)(user)
                        case (_, Some(sid))  => Challenger.Anonymous(sid)
                        case _               => Challenger.Open
                      },
                      destUser = destUser,
                      rematchOf = none
                    )
                    env.challenge.api create challenge flatMap {
                      if _ then
                        negotiate(
                          html = fuccess(Redirect(routes.Round.watcher(challenge.id, "white"))),
                          api = _ => challengeC.showChallenge(challenge, justCreated = true)
                        )
                      else
                        negotiate(
                          html = fuccess(Redirect(routes.Lobby.home)),
                          api = _ => fuccess(BadRequest(jsonError("Challenge not created")))
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

  def hook(sri: Sri) = OpenOrScopedBody(parse.anyContent)(Seq(_.Web.Mobile)): ctx ?=>
    NoBot:
      NoPlaybanOrCurrent(ctx.me):
        forms
          .hook(ctx.me)
          .bindFromRequest()
          .fold(
            jsonFormError,
            userConfig =>
              PostRateLimit(req.ipAddress, rateLimitedFu):
                AnonHookRateLimit(req.ipAddress, rateLimitedFu, cost = ctx.isAnon so 1):
                  (ctx.userId so env.relation.api.fetchBlocking) flatMap { blocking =>
                    processor.hook(
                      ctx.me,
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
          env.game.gameRepo game gameId flatMapz { game =>
            for
              blocking <- ctx.userId so env.relation.api.fetchBlocking
              hookConfig = lila.setup.HookConfig.default(ctx.isAuth)
              hookConfigWithRating = get("rr").fold(
                hookConfig.withRatingRange(
                  ctx.me.fold(Glicko.default.intRating.some)(_.perfs.ratingOf(game.perfKey)),
                  get("deltaMin"),
                  get("deltaMax")
                )
              )(rr => hookConfig withRatingRange rr) updateFrom game
              sameOpponents = game.userIds
              hookResult <-
                processor
                  .hook(
                    ctx.me,
                    hookConfigWithRating,
                    sri,
                    ctx.req.sid,
                    lila.pool.Blocking(blocking ++ sameOpponents)
                  )
            yield hookResponse(hookResult)
          }

  private val BoardApiHookConcurrencyLimitPerUserOrSri = lila.memo.ConcurrencyLimit[Either[Sri, UserId]](
    name = "Board API hook Stream API concurrency per user",
    key = "boardApiHook.concurrency.limit.user",
    ttl = 10.minutes,
    maxConcurrency = 1
  )
  def boardApiHook = AnonOrScopedBody(parse.anyContent)(_.Board.Play, _.Web.Mobile) { ctx ?=> me =>
    val author: Either[Result, Either[Sri, lila.user.User]] = me match
      case Some(u) if u.isBot => Left(notForBotAccounts)
      case Some(u)            => Right(Right(u))
      case None =>
        getAs[Sri]("sri") match
          case Some(sri) => Right(Left(sri))
          case None      => Left(BadRequest(jsonError("Authentication required")))
    author match
      case Left(err) => err.toFuccess
      case Right(author) =>
        forms
          .boardApiHook(ctx.isMobile)
          .bindFromRequest()
          .fold(
            newJsonFormError,
            config =>
              me.map(_.id).so(env.relation.api.fetchBlocking) flatMap { blocking =>
                val uniqId = author.fold(_.value, u => s"sri:${u.id}")
                config.fixColor
                  .hook(Sri(uniqId), me, sid = uniqId.some, lila.pool.Blocking(blocking)) match
                  case Left(hook) =>
                    PostRateLimit(req.ipAddress, rateLimitedFu):
                      BoardApiHookConcurrencyLimitPerUserOrSri(author.map(_.id))(
                        env.lobby.boardApiHookStream(hook.copy(boardApi = true))
                      )(apiC.sourceToNdJsonOption).toFuccess
                  case Right(Some(seek)) =>
                    author match
                      case Left(_) => BadRequest(jsonError("Anonymous users cannot create seeks")).toFuccess
                      case Right(u) =>
                        env.setup.processor.createSeekIfAllowed(seek, u.id) map {
                          case HookResult.Refused =>
                            BadRequest(Json.obj("error" -> "Already playing too many games"))
                          case HookResult.Created(id) => Ok(Json.obj("id" -> id))
                        }
                  case Right(None) => notFoundJson()

              }
          )
  }

  def filterForm = Open:
    fuccess(html.setup.filter(forms.filter))

  def validateFen = Open:
    (get("fen").map(Fen.Epd.clean): Option[Fen.Epd]) flatMap ValidFen(getBool("strict")) match
      case None    => BadRequest.toFuccess
      case Some(v) => Ok(html.board.bits.miniSpan(v.fen.board, v.color)).toFuccess

  def apiAi = ScopedBody(_.Challenge.Write, _.Bot.Play, _.Board.Play) { ctx ?=> me =>
    BotAiRateLimit(me.id, rateLimitedFu, cost = me.isBot so 1):
      PostRateLimit(req.ipAddress, rateLimitedFu):
        forms.api.ai
          .bindFromRequest()
          .fold(
            jsonFormError,
            config =>
              processor.apiAi(config, me) map { pov =>
                Created(env.game.jsonView.base(pov.game, config.fen)) as JSON
              }
          )
  }

  private[controllers] def redirectPov(pov: Pov)(using ctx: WebContext) =
    val redir = Redirect(routes.Round.watcher(pov.gameId.value, "white"))
    if ctx.isAuth then redir
    else
      redir withCookies env.lilaCookie.cookie(
        AnonCookie.name,
        pov.playerId.value,
        maxAge = AnonCookie.maxAge.some,
        httpOnly = false.some
      )
