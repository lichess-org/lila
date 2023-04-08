package controllers

import play.api.libs.json.Json

import chess.format.Fen
import lila.api.Context
import lila.app.{ given, * }
import lila.common.IpAddress
import lila.game.{ AnonCookie, Pov }
import lila.rating.Glicko
import lila.setup.Processor.HookResult
import lila.setup.ValidFen
import lila.socket.Socket.Sri
import views.*

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

  def ai = OpenBody { implicit ctx =>
    BotAiRateLimit(ctx.userId | UserId(""), cost = ctx.me.exists(_.isBot) ?? 1) {
      PostRateLimit(ctx.ip) {
        given play.api.mvc.Request[?] = ctx.body
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
      }(rateLimitedFu)
    }(rateLimitedFu)
  }

  def friend(userId: Option[UserStr]) =
    OpenBody { implicit ctx =>
      given play.api.mvc.Request[?] = ctx.body
      PostRateLimit(ctx.ip) {
        forms
          .friend(ctx)
          .bindFromRequest()
          .fold(
            jsonFormError,
            config =>
              userId ?? env.user.repo.enabledById flatMap { destUser =>
                destUser ?? { env.challenge.granter.isDenied(ctx.me, _, config.perfType) } flatMap {
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
                    (env.challenge.api create challenge) flatMap {
                      case true =>
                        negotiate(
                          html = fuccess(Redirect(routes.Round.watcher(challenge.id, "white"))),
                          api = _ => challengeC.showChallenge(challenge, justCreated = true)
                        )
                      case false =>
                        negotiate(
                          html = fuccess(Redirect(routes.Lobby.home)),
                          api = _ => fuccess(BadRequest(jsonError("Challenge not created")))
                        )
                    }
                }
              }
          )
      }(rateLimitedFu)
    }

  private def hookResponse(res: HookResult) =
    res match
      case HookResult.Created(id) =>
        JsonOk(
          Json.obj(
            "ok"   -> true,
            "hook" -> Json.obj("id" -> id)
          )
        )
      case HookResult.Refused => BadRequest(jsonError("Game was not created"))

  def hook(sri: String) =
    OpenBody { implicit ctx =>
      NoBot {
        given play.api.mvc.Request[?] = ctx.body
        NoPlaybanOrCurrent {
          forms.hook
            .bindFromRequest()
            .fold(
              jsonFormError,
              userConfig =>
                PostRateLimit(ctx.ip) {
                  AnonHookRateLimit(ctx.ip, cost = ctx.isAnon ?? 1) {
                    (ctx.userId ?? env.relation.api.fetchBlocking) flatMap { blocking =>
                      processor.hook(
                        userConfig withinLimits ctx.me,
                        Sri(sri),
                        ctx.req.sid,
                        lila.pool.Blocking(blocking)
                      ) map hookResponse
                    }
                  }(rateLimitedFu)
                }(rateLimitedFu)
            )
        }
      }
    }

  def like(sri: String, gameId: GameId) =
    Open { implicit ctx =>
      NoBot {
        PostRateLimit(ctx.ip) {
          NoPlaybanOrCurrent {
            env.game.gameRepo game gameId flatMapz { game =>
              for
                blocking <- ctx.userId ?? env.relation.api.fetchBlocking
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
                      hookConfigWithRating,
                      Sri(sri),
                      ctx.req.sid,
                      lila.pool.Blocking(blocking ++ sameOpponents)
                    )
              yield hookResponse(hookResult)
            }
          }
        }(rateLimitedFu)
      }
    }

  private val BoardApiHookConcurrencyLimitPerUser = lila.memo.ConcurrencyLimit[UserId](
    name = "Board API hook Stream API concurrency per user",
    key = "boardApiHook.concurrency.limit.user",
    ttl = 10.minutes,
    maxConcurrency = 1
  )
  def boardApiHook =
    ScopedBody(_.Board.Play) { implicit req => me =>
      given play.api.i18n.Lang = reqLang
      if (me.isBot) notForBotAccounts.toFuccess
      else
        forms.boardApiHook
          .bindFromRequest()
          .fold(
            newJsonFormError,
            config =>
              env.relation.api.fetchBlocking(me.id) flatMap { blocking =>
                val uniqId = s"sri:${me.id}"
                config.fixColor
                  .hook(Sri(uniqId), me.some, sid = uniqId.some, lila.pool.Blocking(blocking)) match {
                  case Left(hook) =>
                    PostRateLimit(req.ipAddress) {
                      BoardApiHookConcurrencyLimitPerUser(me.id)(
                        env.lobby.boardApiHookStream(hook.copy(boardApi = true))
                      )(apiC.sourceToNdJsonOption).toFuccess
                    }(rateLimitedFu)
                  case Right(Some(seek)) =>
                    env.setup.processor.createSeekIfAllowed(seek, me.id) map {
                      case HookResult.Refused =>
                        BadRequest(Json.obj("error" -> "Already playing too many games"))
                      case HookResult.Created(id) => Ok(Json.obj("id" -> id))
                    }
                  case Right(None) => notFoundJson()
                }
              }
          )
    }

  def filterForm =
    Open { implicit ctx =>
      fuccess(html.setup.filter(forms.filter))
    }

  def validateFen =
    Open { implicit ctx =>
      (get("fen").map(Fen.Epd.clean): Option[Fen.Epd]) flatMap ValidFen(getBool("strict")) match
        case None    => BadRequest.toFuccess
        case Some(v) => Ok(html.board.bits.miniSpan(v.fen.board, v.color)).toFuccess
    }

  def apiAi =
    ScopedBody(_.Challenge.Write, _.Bot.Play, _.Board.Play) { implicit req => me =>
      given play.api.i18n.Lang = reqLang
      BotAiRateLimit(me.id, cost = me.isBot ?? 1) {
        PostRateLimit(req.ipAddress) {
          forms.api.ai
            .bindFromRequest()
            .fold(
              jsonFormError,
              config =>
                processor.apiAi(config, me) map { pov =>
                  Created(env.game.jsonView.base(pov.game, config.fen)) as JSON
                }
            )
        }(rateLimitedFu)
      }(rateLimitedFu)
    }

  private[controllers] def redirectPov(pov: Pov)(implicit ctx: Context) =
    val redir = Redirect(routes.Round.watcher(pov.gameId.value, "white"))
    if (ctx.isAuth) redir
    else
      redir withCookies env.lilaCookie.cookie(
        AnonCookie.name,
        pov.playerId.value,
        maxAge = AnonCookie.maxAge.some,
        httpOnly = false.some
      )
