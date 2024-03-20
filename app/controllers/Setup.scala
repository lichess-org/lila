package controllers

import play.api.libs.json.Json
import play.api.mvc.Results
import scala.concurrent.duration._

import shogi.format.forsyth.Sfen
import lila.api.Context
import lila.app._
import lila.common.{ HTTPRequest, IpAddress }
import lila.game.{ AnonCookie, Pov }
import lila.setup.Processor.HookResult
import lila.setup.ValidSfen
import lila.socket.Socket.Sri
import views._

final class Setup(
    env: Env,
    challengeC: => Challenge,
    apiC: => Api
) extends LilaController(env)
    with TheftPrevention {

  private def forms     = env.setup.forms
  private def processor = env.setup.processor

  private[controllers] val PostRateLimit = new lila.memo.RateLimit[IpAddress](
    5,
    1.minute,
    key = "setup.post",
    enforce = env.net.rateLimit.value
  )

  private[controllers] val BotAiRateLimit = new lila.memo.RateLimit[lila.user.User.ID](
    50,
    1.day,
    key = "setup.post.bot.ai"
  )

  def aiForm =
    Open { implicit ctx =>
      if (HTTPRequest isXhr ctx.req) {
        fuccess(
          forms.aiFilled(get("sfen").map(Sfen.clean), get("variant").flatMap(shogi.variant.Variant.apply))
        ) map { form =>
          val variant = form("variant").value.flatMap(shogi.variant.Variant.apply) | shogi.variant.Standard
          html.setup.forms.ai(
            form,
            form("sfen").value map Sfen.clean flatMap ValidSfen(getBool("strict"), variant)
          )
        }
      } else Redirect(s"${routes.Lobby.home}#ai").fuccess
    }

  def ai = OpenBody { implicit ctx =>
    BotAiRateLimit(~ctx.userId, cost = ctx.me.exists(_.isBot) ?? 1) {
      PostRateLimit(HTTPRequest lastRemoteAddress ctx.req) {
        implicit val req = ctx.body
        forms.ai
          .bindFromRequest()
          .fold(
            err =>
              negotiate(
                html = keyPages.home(Results.BadRequest),
                api = _ => jsonFormError(err)
              ),
            config =>
              processor.ai(config)(ctx) flatMap { pov =>
                negotiate(
                  html = fuccess(redirectPov(pov)),
                  api = apiVersion =>
                    if (getBool("redirect"))
                      fuccess(Ok(redirectPovJson(pov)))
                    else
                      env.api.roundApi.player(pov, none, apiVersion) map { data =>
                        Created(data) as JSON
                      }
                )
              }
          )
      }(rateLimitedFu)
    }(rateLimitedFu)
  }

  private def redirectPovJson(pov: Pov) =
    Json
      .obj(
        "id"  -> pov.fullId,
        "url" -> s"/${pov.fullId}"
      )
      .add("cookie" -> lila.game.AnonCookie.json(pov))

  def friendForm(userId: Option[String]) =
    Open { implicit ctx =>
      if (HTTPRequest isXhr ctx.req)
        fuccess(
          forms.friendFilled(get("sfen").map(Sfen.clean), get("variant").flatMap(shogi.variant.Variant.apply))
        ) flatMap { form =>
          val variant   = form("variant").value.flatMap(shogi.variant.Variant.apply) | shogi.variant.Standard
          val validSfen = form("sfen").value map Sfen.clean flatMap ValidSfen(false, variant)
          userId ?? env.user.repo.named flatMap {
            case None => Ok(html.setup.forms.friend(form, none, none, validSfen)).fuccess
            case Some(user) =>
              env.challenge.granter(ctx.me, user, none) map {
                case Some(denied) => BadRequest(lila.challenge.ChallengeDenied.translated(denied))
                case None         => Ok(html.setup.forms.friend(form, user.some, none, validSfen))
              }
          }
        }
      else
        fuccess {
          Redirect(s"${routes.Lobby.home}#friend")
        }
    }

  def friend(userId: Option[String]) =
    OpenBody { implicit ctx =>
      implicit val req = ctx.body
      PostRateLimit(HTTPRequest lastRemoteAddress ctx.req) {
        forms
          .friend(ctx)
          .bindFromRequest()
          .fold(
            err =>
              negotiate(
                html = keyPages.home(Results.BadRequest),
                api = _ => jsonFormError(err)
              ),
            config =>
              userId ?? env.user.repo.enabledById flatMap { destUser =>
                destUser ?? { env.challenge.granter(ctx.me, _, config.perfType) } flatMap {
                  case Some(denied) =>
                    val message = lila.challenge.ChallengeDenied.translated(denied)
                    negotiate(
                      html = BadRequest(html.site.message.challengeDenied(message)).fuccess,
                      api = _ => BadRequest(jsonError(message)).fuccess
                    )
                  case None =>
                    import lila.challenge.Challenge._
                    val timeControl = config.makeClock map {
                      TimeControl.Clock.apply
                    } orElse config.makeDaysPerTurn.map {
                      TimeControl.Correspondence.apply
                    } getOrElse TimeControl.Unlimited
                    val challenge = lila.challenge.Challenge.make(
                      variant = config.variant,
                      initialSfen = config.sfen,
                      timeControl = timeControl,
                      mode = config.mode,
                      color = config.color.name,
                      challenger = (ctx.me, HTTPRequest sid req) match {
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
                          html = fuccess(Redirect(routes.Round.watcher(challenge.id, "sente"))),
                          api = _ => challengeC showChallenge challenge
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

  def hookForm =
    Open { implicit ctx =>
      NoBot {
        if (HTTPRequest isXhr ctx.req) NoPlaybanOrCurrent {
          fuccess(forms.hookFilled(timeModeString = get("time"))) map { html.setup.forms.hook(_) }
        }
        else
          fuccess {
            Redirect(s"${routes.Lobby.home}#hook")
          }
      }
    }

  private def hookResponse(res: HookResult) =
    res match {
      case HookResult.Created(id) =>
        Ok(
          Json.obj(
            "ok"   -> true,
            "hook" -> Json.obj("id" -> id)
          )
        ) as JSON
      case HookResult.Refused => BadRequest(jsonError("Game was not created"))
    }

  def hook(sri: String) =
    OpenBody { implicit ctx =>
      NoBot {
        implicit val req = ctx.body
        PostRateLimit(HTTPRequest lastRemoteAddress ctx.req) {
          NoPlaybanOrCurrent {
            forms
              .hook(ctx)
              .bindFromRequest()
              .fold(
                jsonFormError,
                userConfig =>
                  (ctx.userId ?? env.relation.api.fetchBlocking) flatMap { blocking =>
                    processor.hook(
                      userConfig withinLimits ctx.me,
                      Sri(sri),
                      HTTPRequest sid req,
                      blocking
                    ) map hookResponse
                  }
              )
          }
        }(rateLimitedFu)
      }
    }

  def like(sri: String, gameId: String) =
    Open { implicit ctx =>
      NoBot {
        PostRateLimit(HTTPRequest lastRemoteAddress ctx.req) {
          NoPlaybanOrCurrent {
            env.game.gameRepo game gameId flatMap {
              _ ?? { game =>
                for {
                  blocking <- ctx.userId ?? env.relation.api.fetchBlocking
                  hookConfig    = lila.setup.HookConfig.default withRatingRange get("rr") updateFrom game
                  sameOpponents = game.userIds
                  hookResult <-
                    processor
                      .hook(
                        hookConfig,
                        Sri(sri),
                        HTTPRequest sid ctx.req,
                        blocking ++ sameOpponents
                      )
                } yield hookResponse(hookResult)
              }
            }
          }
        }(rateLimitedFu)
      }
    }

  private val BoardApiHookConcurrencyLimitPerUser = new lila.memo.ConcurrencyLimit[String](
    name = "Board API hook Stream API concurrency per user",
    key = "boardApiHook.concurrency.limit.user",
    ttl = 10.minutes,
    maxConcurrency = 1
  )
  def boardApiHook =
    ScopedBody(_.Board.Play) { implicit req => me =>
      implicit val lang = reqLang
      if (me.isBot) notForBotAccounts.fuccess
      else
        forms.boardApiHook
          .bindFromRequest()
          .fold(
            newJsonFormError,
            config =>
              env.relation.api.fetchBlocking(me.id) flatMap { blocking =>
                val uniqId = s"sri:${me.id}"
                config.hook(Sri(uniqId), me.some, sid = uniqId.some, blocking) match {
                  case Left(hook) =>
                    PostRateLimit(HTTPRequest lastRemoteAddress req) {
                      BoardApiHookConcurrencyLimitPerUser(me.id)(
                        env.lobby.boardApiHookStream(hook.copy(boardApi = true))
                      )(apiC.sourceToNdJsonOption).fuccess
                    }(rateLimitedFu)
                  case _ => BadRequest(jsonError("Invalid board API seek")).fuccess
                }
              }
          )
    }

  def filterForm =
    Open { implicit ctx =>
      fuccess(html.setup.filter(forms.filter))
    }

  def validateSfen =
    Open { implicit ctx =>
      (for {
        variant <- get("variant").flatMap(_.toIntOption) flatMap shogi.variant.Variant.apply
        sfen    <- get("sfen") map Sfen.clean orElse variant.initialSfen.some
        valid   <- ValidSfen.apply(getBool("strict"), variant)(sfen)
      } yield valid) match {
        case None    => BadRequest.fuccess
        case Some(v) => Ok(html.game.bits.miniBoard(v.sfen, v.situation.color, v.situation.variant)).fuccess
      }
    }

  def apiAi =
    ScopedBody(_.Challenge.Write, _.Bot.Play, _.Board.Play) { implicit req => me =>
      implicit val lang = reqLang
      BotAiRateLimit(me.id, cost = me.isBot ?? 1) {
        PostRateLimit(HTTPRequest lastRemoteAddress req) {
          forms.api.ai
            .bindFromRequest()
            .fold(
              jsonFormError,
              config =>
                processor.apiAi(config, me) map { pov =>
                  Created(env.game.jsonView(pov.game)) as JSON
                }
            )
        }(rateLimitedFu)
      }(rateLimitedFu)
    }

  private[controllers] def redirectPov(pov: Pov)(implicit ctx: Context) = {
    val redir = Redirect(routes.Round.watcher(pov.gameId, "sente"))
    if (ctx.isAuth) redir
    else
      redir withCookies env.lilaCookie.cookie(
        AnonCookie.name,
        pov.playerId,
        maxAge = AnonCookie.maxAge.some,
        httpOnly = false.some
      )
  }
}
