package controllers

import play.api.libs.json.Json
import play.api.mvc.Result
import scala.annotation.nowarn
import scala.concurrent.duration._

import lila.api.Context
import lila.app._
import lila.challenge.{ Challenge => ChallengeModel }
import lila.common.{ HTTPRequest, IpAddress }
import lila.game.{ AnonCookie, Pov }
import lila.socket.Socket.SocketVersion
import lila.user.{ User => UserModel }
import views.html

final class Challenge(
    env: Env
) extends LilaController(env) {

  def api = env.challenge.api

  def all =
    Auth { implicit ctx => me =>
      XhrOrRedirectHome {
        api allFor me.id map { all =>
          Ok(env.challenge.jsonView(all)) as JSON
        }
      }
    }

  def show(id: String, @nowarn("cat=unused") _color: Option[String]) =
    Open { implicit ctx =>
      showId(id)
    }

  protected[controllers] def showId(id: String)(implicit
      ctx: Context
  ): Fu[Result] =
    OptionFuResult(api byId id)(showChallenge(_))

  protected[controllers] def showChallenge(
      c: ChallengeModel,
      error: Option[String] = None
  )(implicit
      ctx: Context
  ): Fu[Result] =
    env.challenge version c.id flatMap { version =>
      val mine = isMine(c)
      import lila.challenge.Direction
      val direction: Option[Direction] =
        if (mine) Direction.Out.some
        else if (isForMe(c)) Direction.In.some
        else none
      val json = env.challenge.jsonView.show(c, version, direction)
      negotiate(
        html =
          if (mine) fuccess {
            error match {
              case Some(e) => BadRequest(html.challenge.mine(c, json, e.some))
              case None    => Ok(html.challenge.mine(c, json, none))
            }
          }
          else
            (c.challengerUserId ?? env.user.repo.named) map { user =>
              Ok(html.challenge.theirs(c, json, user, get("color") flatMap chess.Color.apply))
            },
        api = _ => Ok(json).fuccess
      ) flatMap withChallengeAnonCookie(mine && c.challengerIsAnon, c, true)
    } dmap env.lilaCookie.ensure(ctx.req)

  private def isMine(challenge: ChallengeModel)(implicit ctx: Context) =
    challenge.challenger match {
      case lila.challenge.Challenge.Challenger.Anonymous(secret)     => HTTPRequest sid ctx.req contains secret
      case lila.challenge.Challenge.Challenger.Registered(userId, _) => ctx.userId contains userId
      case lila.challenge.Challenge.Challenger.Open                  => false
    }

  private def isForMe(challenge: ChallengeModel)(implicit ctx: Context) =
    challenge.destUserId.fold(true)(ctx.userId.contains)

  def accept(id: String, color: Option[String]) =
    Open { implicit ctx =>
      OptionFuResult(api byId id) { c =>
        val cc = color flatMap chess.Color.apply
        isForMe(c) ?? api
          .accept(c, ctx.me, HTTPRequest sid ctx.req, cc)
          .flatMap {
            case Some(pov) =>
              negotiate(
                html = Redirect(routes.Round.watcher(pov.gameId, cc.fold("white")(_.name))).fuccess,
                api = apiVersion => env.api.roundApi.player(pov, none, apiVersion) map { Ok(_) }
              ) flatMap withChallengeAnonCookie(ctx.isAnon, c, false)
            case None =>
              negotiate(
                html = Redirect(routes.Round.watcher(c.id, cc.fold("white")(_.name))).fuccess,
                api = _ => notFoundJson("Someone else accepted the challenge")
              )
          }
      }
    }
  def apiAccept(id: String) =
    Scoped(_.Challenge.Write, _.Bot.Play, _.Board.Play) { _ => me =>
      api.onlineByIdFor(id, me) flatMap {
        _ ?? { api.accept(_, me.some, none) }
      } flatMap { res =>
        if (res.isDefined) jsonOkResult.fuccess
        else
          env.bot.player.rematchAccept(id, me) flatMap {
            case true => jsonOkResult.fuccess
            case _    => notFoundJson()
          }
      }
    }

  private def withChallengeAnonCookie(cond: Boolean, c: ChallengeModel, owner: Boolean)(
      res: Result
  )(implicit ctx: Context): Fu[Result] =
    cond ?? {
      env.game.gameRepo.game(c.id).map {
        _ map { game =>
          env.lilaCookie.cookie(
            AnonCookie.name,
            game.player(if (owner) c.finalColor else !c.finalColor).id,
            maxAge = AnonCookie.maxAge.some,
            httpOnly = false.some
          )
        }
      }
    } map { cookieOption =>
      cookieOption.fold(res) { res.withCookies(_) }
    }

  def decline(id: String) =
    Auth { implicit ctx => _ =>
      OptionFuResult(api byId id) { c =>
        if (isForMe(c)) api decline c
        else notFound
      }
    }
  def apiDecline(id: String) =
    Scoped(_.Challenge.Write, _.Bot.Play, _.Board.Play) { _ => me =>
      api.activeByIdFor(id, me) flatMap {
        case None =>
          env.bot.player.rematchDecline(id, me) flatMap {
            case true => jsonOkResult.fuccess
            case _    => notFoundJson()
          }
        case Some(c) => api.decline(c) inject jsonOkResult
      }
    }

  def cancel(id: String) =
    Open { implicit ctx =>
      OptionFuResult(api byId id) { c =>
        if (isMine(c)) api cancel c
        else notFound
      }
    }

  private val ChallengeIpRateLimit = new lila.memo.RateLimit[IpAddress](
    100,
    10.minute,
    key = "challenge.create.ip",
    enforce = env.net.rateLimit.value
  )

  private val ChallengeUserRateLimit = lila.memo.RateLimit.composite[lila.user.User.ID](
    key = "challenge.create.user",
    enforce = env.net.rateLimit.value
  )(
    ("fast", 5, 1.minute),
    ("slow", 30, 1.day)
  )

  def toFriend(id: String) =
    AuthBody { implicit ctx => _ =>
      import play.api.data._
      import play.api.data.Forms._
      implicit def req = ctx.body
      OptionFuResult(api byId id) { c =>
        if (isMine(c))
          Form(
            single(
              "username" -> lila.user.DataForm.historicalUsernameField
            )
          ).bindFromRequest()
            .fold(
              _ => funit,
              username =>
                ChallengeIpRateLimit(HTTPRequest lastRemoteAddress req) {
                  env.user.repo named username flatMap {
                    case None => Redirect(routes.Challenge.show(c.id)).fuccess
                    case Some(dest) =>
                      env.challenge.granter(ctx.me, dest, c.perfType.some) flatMap {
                        case Some(denied) =>
                          showChallenge(c, lila.challenge.ChallengeDenied.translated(denied).some)
                        case None => api.setDestUser(c, dest) inject Redirect(routes.Challenge.show(c.id))
                      }
                  }
                }(rateLimitedFu)
            )
        else notFound
      }
    }

  def apiCreate(userId: String) =
    ScopedBody(_.Challenge.Write, _.Bot.Play, _.Board.Play) { implicit req => me =>
      implicit val lang = reqLang
      env.setup.forms.api.user
        .bindFromRequest()
        .fold(
          err => BadRequest(apiFormError(err)).fuccess,
          config => {
            val cost = if (me.isApiHog) 0 else 1
            ChallengeIpRateLimit(HTTPRequest lastRemoteAddress req, cost = cost) {
              ChallengeUserRateLimit(me.id, cost = cost) {
                env.user.repo enabledById userId.toLowerCase flatMap {
                  destUser =>
                    import lila.challenge.Challenge._
                    val timeControl = config.clock map {
                      TimeControl.Clock.apply
                    } orElse config.days.map {
                      TimeControl.Correspondence.apply
                    } getOrElse TimeControl.Unlimited
                    val challenge = lila.challenge.Challenge.make(
                      variant = config.variant,
                      initialFen = config.position,
                      timeControl = timeControl,
                      mode = config.mode,
                      color = config.color.name,
                      challenger = ChallengeModel.toRegistered(config.variant, timeControl)(me),
                      destUser = destUser,
                      rematchOf = none
                    )
                    (destUser, config.acceptByToken) match {
                      case (Some(dest), Some(strToken)) => apiChallengeAccept(dest, challenge, strToken)
                      case _ =>
                        destUser ?? { env.challenge.granter(me.some, _, config.perfType) } flatMap {
                          case Some(denied) =>
                            BadRequest(jsonError(lila.challenge.ChallengeDenied.translated(denied))).fuccess
                          case _ =>
                            (env.challenge.api create challenge) map {
                              case true =>
                                JsonOk(
                                  env.challenge.jsonView
                                    .show(challenge, SocketVersion(0), lila.challenge.Direction.Out.some)
                                )
                              case false =>
                                BadRequest(jsonError("Challenge not created"))
                            }
                        } map (_ as JSON)
                    }
                }
              }(rateLimitedFu)
            }(rateLimitedFu)
          }
        )
    }

  private def apiChallengeAccept(
      dest: UserModel,
      challenge: lila.challenge.Challenge,
      strToken: String
  ) =
    env.security.api.oauthScoped(
      lila.oauth.AccessToken.Id(strToken),
      List(lila.oauth.OAuthScope.Challenge.Write)
    ) flatMap {
      _.fold(
        err => BadRequest(jsonError(err.message)).fuccess,
        scoped =>
          if (scoped.user is dest)
            env.challenge.api.oauthAccept(dest, challenge) map {
              case None => BadRequest(jsonError("Couldn't create game"))
              case Some(g) =>
                Ok(
                  Json.obj(
                    "game" -> env.game.jsonView(g, challenge.initialFen)
                  )
                )
            }
          else BadRequest(jsonError("dest and accept user don't match")).fuccess
      )
    }

  def openCreate =
    Action.async { implicit req =>
      implicit val lang = reqLang
      env.setup.forms.api.open
        .bindFromRequest()
        .fold(
          err => BadRequest(apiFormError(err)).fuccess,
          config =>
            ChallengeIpRateLimit(HTTPRequest lastRemoteAddress req) {
              import lila.challenge.Challenge._
              val challenge = lila.challenge.Challenge
                .make(
                  variant = config.variant,
                  initialFen = config.position,
                  timeControl = config.clock.fold[TimeControl](TimeControl.Unlimited) {
                    TimeControl.Clock.apply
                  },
                  mode = chess.Mode.Casual,
                  color = "random",
                  challenger = Challenger.Open,
                  destUser = none,
                  rematchOf = none
                )
              (env.challenge.api create challenge) map {
                case true =>
                  JsonOk(
                    env.challenge.jsonView.show(challenge, SocketVersion(0), none) ++ Json.obj(
                      "urlWhite" -> s"${env.net.baseUrl}/${challenge.id}?color=white",
                      "urlBlack" -> s"${env.net.baseUrl}/${challenge.id}?color=black"
                    )
                  )
                case false =>
                  BadRequest(jsonError("Challenge not created"))
              }
            }(rateLimitedFu) dmap (_ as JSON)
        )
    }

  def rematchOf(gameId: String) =
    Auth { implicit ctx => me =>
      OptionFuResult(env.game.gameRepo game gameId) { g =>
        Pov.opponentOfUserId(g, me.id).flatMap(_.userId) ?? env.user.repo.byId flatMap {
          _ ?? { opponent =>
            env.challenge.granter(me.some, opponent, g.perfType) flatMap {
              case Some(d) =>
                BadRequest(jsonError {
                  lila.challenge.ChallengeDenied translated d
                }).fuccess
              case _ =>
                api.sendRematchOf(g, me) map {
                  case true => Ok
                  case _    => BadRequest(jsonError("Sorry, couldn't create the rematch."))
                }
            }
          }
        }
      }
    }
}
