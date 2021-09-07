package controllers

import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.Results
import scala.concurrent.duration._

import chess.format.FEN
import lila.api.{ BodyContext, Context }
import lila.app._
import lila.common.{ HTTPRequest, IpAddress }
import lila.game.{ AnonCookie, Pov }
import lila.setup.Processor.HookResult
import lila.setup.ValidFen
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
    enforce = env.net.rateLimit.value,
    log = false
  )

  def aiForm =
    Open { implicit ctx =>
      if (HTTPRequest isXhr ctx.req) {
        fuccess(forms aiFilled get("fen").map(FEN.clean)) map { form =>
          html.setup.forms.ai(
            form,
            env.fishnet.aiPerfApi.intRatings,
            form("fen").value map FEN.clean flatMap ValidFen(getBool("strict"))
          )
        }
      } else Redirect(s"${routes.Lobby.home}#ai").fuccess
    }

  def ai =
    process(_ => forms.ai) { config => implicit ctx =>
      processor ai config
    }

  def friendForm(userId: Option[String]) =
    Open { implicit ctx =>
      if (HTTPRequest isXhr ctx.req)
        fuccess(forms friendFilled get("fen").map(FEN.clean)) flatMap { form =>
          val validFen = form("fen").value map FEN.clean flatMap ValidFen(strict = false)
          userId ?? env.user.repo.named flatMap {
            case None => Ok(html.setup.forms.friend(form, none, none, validFen)).fuccess
            case Some(user) =>
              env.challenge.granter(ctx.me, user, none) map {
                case Some(denied) => BadRequest(lila.challenge.ChallengeDenied.translated(denied))
                case None         => Ok(html.setup.forms.friend(form, user.some, none, validFen))
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
      PostRateLimit(ctx.ip) {
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
                      initialFen = config.fen,
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

  def hookForm =
    Open { implicit ctx =>
      NoBot {
        if (HTTPRequest isXhr ctx.req) NoPlaybanOrCurrent {
          val timeMode = get("time")
          fuccess(forms.hookFilled(timeModeString = timeMode)) map {
            html.setup.forms.hook(_, timeMode.isDefined)
          }
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
        JsonOk(
          Json.obj(
            "ok"   -> true,
            "hook" -> Json.obj("id" -> id)
          )
        )
      case HookResult.Refused => BadRequest(jsonError("Game was not created"))
    }

  def hook(sri: String) =
    OpenBody { implicit ctx =>
      NoBot {
        implicit val req = ctx.body
        PostRateLimit(ctx.ip) {
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
        PostRateLimit(ctx.ip) {
          NoPlaybanOrCurrent {
            env.game.gameRepo game gameId flatMap {
              _ ?? { game =>
                for {
                  blocking <- ctx.userId ?? env.relation.api.fetchBlocking
                  hookConfig = lila.setup.HookConfig.default(ctx.isAuth) withRatingRange get(
                    "rr"
                  ) updateFrom game
                  sameOpponents = game.userIds
                  hookResult <-
                    processor
                      .hook(hookConfig, Sri(sri), HTTPRequest sid ctx.req, blocking ++ sameOpponents)
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
                config.fixColor.hook(Sri(uniqId), me.some, sid = uniqId.some, blocking) match {
                  case Left(hook) =>
                    PostRateLimit(HTTPRequest ipAddress req) {
                      BoardApiHookConcurrencyLimitPerUser(me.id)(
                        env.lobby.boardApiHookStream(hook.copy(boardApi = true))
                      )(apiC.sourceToNdJsonOption).fuccess
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
      get("fen") map FEN.clean flatMap ValidFen(getBool("strict")) match {
        case None    => BadRequest.fuccess
        case Some(v) => Ok(html.board.bits.miniSpan(v.fen, v.color)).fuccess
      }
    }

  def apiAi =
    ScopedBody(_.Challenge.Write, _.Bot.Play, _.Board.Play) { implicit req => me =>
      implicit val lang = reqLang
      PostRateLimit(HTTPRequest ipAddress req) {
        forms.api.ai
          .bindFromRequest()
          .fold(
            jsonFormError,
            config =>
              processor.apiAi(config, me) map { pov =>
                Created(env.game.jsonView(pov.game, config.fen)) as JSON
              }
          )
      }(rateLimitedFu)
    }

  private def process[A](form: Context => Form[A])(op: A => BodyContext[_] => Fu[Pov]) =
    OpenBody { implicit ctx =>
      PostRateLimit(ctx.ip) {
        implicit val req = ctx.body
        form(ctx)
          .bindFromRequest()
          .fold(
            err =>
              negotiate(
                html = keyPages.home(Results.BadRequest),
                api = _ => jsonFormError(err)
              ),
            config =>
              op(config)(ctx) flatMap { pov =>
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
    }

  private[controllers] def redirectPov(pov: Pov)(implicit ctx: Context) = {
    val redir = Redirect(routes.Round.watcher(pov.gameId, "white"))
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
