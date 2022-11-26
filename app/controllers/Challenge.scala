package controllers

import cats.data.Validated
import play.api.libs.json.Json
import play.api.mvc.{ Request, Result }
import scala.annotation.nowarn
import scala.concurrent.duration.*
import views.html

import lila.api.Context
import lila.app.{ given, * }
import lila.challenge.{ Challenge as ChallengeModel }
import lila.challenge.Challenge.ID
import lila.common.{ Bearer, HTTPRequest, IpAddress, Template }
import lila.game.{ AnonCookie, Pov }
import lila.oauth.{ AccessToken, OAuthScope }
import lila.setup.ApiConfig
import lila.socket.SocketVersion
import lila.socket.SocketVersion.given
import lila.user.{ User as UserModel }
import play.api.mvc.RequestHeader

final class Challenge(
    env: Env,
    apiC: Api
) extends LilaController(env):

  def api = env.challenge.api

  def all =
    Auth { implicit ctx => me =>
      XhrOrRedirectHome {
        api allFor me.id map env.challenge.jsonView.apply map JsonOk
      }
    }

  def apiList =
    ScopedBody(_.Challenge.Read) { implicit req => me =>
      given play.api.i18n.Lang = reqLang
      api.allFor(me.id, 300) map { all =>
        JsonOk(
          Json.obj(
            "in"  -> all.in.map(env.challenge.jsonView.apply(lila.challenge.Direction.In.some)),
            "out" -> all.out.map(env.challenge.jsonView.apply(lila.challenge.Direction.Out.some))
          )
        )
      }
    }

  def show(id: String, _color: Option[String]) =
    Open { implicit ctx =>
      showId(id)
    }

  protected[controllers] def showId(id: String)(implicit ctx: Context): Fu[Result] =
    OptionFuResult(api byId id)(showChallenge(_))

  protected[controllers] def showChallenge(
      c: ChallengeModel,
      error: Option[String] = None,
      justCreated: Boolean = false
  )(using ctx: Context): Fu[Result] =
    env.challenge version c.id flatMap { version =>
      val mine = justCreated || isMine(c)
      import lila.challenge.Direction
      val direction: Option[Direction] =
        if (mine) Direction.Out.some
        else if (isForMe(c, ctx.me)) Direction.In.some
        else none
      val json = env.challenge.jsonView.show(c, version, direction)
      negotiate(
        html = {
          val color = get("color") flatMap chess.Color.fromName
          if (mine) fuccess {
            error match {
              case Some(e) => BadRequest(html.challenge.mine(c, json, e.some, color))
              case None    => Ok(html.challenge.mine(c, json, none, color))
            }
          }
          else
            (c.challengerUserId ?? env.user.repo.named) map { user =>
              Ok(html.challenge.theirs(c, json, user, color))
            }
        },
        api = _ => Ok(json).toFuccess
      ) flatMap withChallengeAnonCookie(mine && c.challengerIsAnon, c, owner = true)
    } map env.lilaCookie.ensure(ctx.req)

  private def isMine(challenge: ChallengeModel)(implicit ctx: Context) =
    challenge.challenger match
      case lila.challenge.Challenge.Challenger.Anonymous(secret) => HTTPRequest sid ctx.req contains secret
      case lila.challenge.Challenge.Challenger.Registered(userId, _) => ctx.userId contains userId
      case lila.challenge.Challenge.Challenger.Open                  => false

  private def isForMe(challenge: ChallengeModel, me: Option[UserModel]) =
    challenge.destUserId.fold(true)(dest => me.exists(_ is dest)) &&
      !challenge.challengerUserId.??(orig => me.exists(_ is orig))

  def accept(id: ID, color: Option[String]) =
    Open { implicit ctx =>
      OptionFuResult(api byId id) { c =>
        val cc = color flatMap chess.Color.fromName
        isForMe(c, ctx.me) ?? api
          .accept(c, ctx.me, HTTPRequest sid ctx.req, cc)
          .flatMap {
            case Validated.Valid(Some(pov)) =>
              negotiate(
                html = Redirect(routes.Round.watcher(pov.gameId, cc.fold("white")(_.name))).toFuccess,
                api = apiVersion => env.api.roundApi.player(pov, none, apiVersion) map { Ok(_) }
              ) flatMap withChallengeAnonCookie(ctx.isAnon, c, owner = false)
            case invalid =>
              negotiate(
                html = Redirect(routes.Round.watcher(c.id, cc.fold("white")(_.name))).toFuccess,
                api = _ =>
                  notFoundJson(invalid match {
                    case Validated.Invalid(err) => err
                    case _                      => "The challenge has already been accepted"
                  })
              )
          }
      }
    }

  def apiAccept(id: ID) =
    Scoped(_.Challenge.Write, _.Bot.Play, _.Board.Play) { _ => me =>
      def tryRematch =
        env.bot.player.rematchAccept(GameId(id), me) flatMap {
          case true => jsonOkResult.toFuccess
          case _    => notFoundJson()
        }
      api.byId(id) flatMap {
        _.filter(isForMe(_, me.some)) match
          case None                  => tryRematch
          case Some(c) if c.accepted => tryRematch
          case Some(c) =>
            api.accept(c, me.some, none) map {
              _.fold(err => BadRequest(jsonError(err)), _ => jsonOkResult)
            }
      }
    }

  private def withChallengeAnonCookie(cond: Boolean, c: ChallengeModel, owner: Boolean)(
      res: Result
  )(implicit ctx: Context): Fu[Result] =
    cond ?? {
      env.game.gameRepo.game(GameId(c.id)).map {
        _ map { game =>
          env.lilaCookie.cookie(
            AnonCookie.name,
            game.player(if (owner) c.finalColor else !c.finalColor).id.value,
            maxAge = AnonCookie.maxAge.some,
            httpOnly = false.some
          )
        }
      }
    } map { cookieOption =>
      cookieOption.fold(res) { res.withCookies(_) }
    }

  def decline(id: ID) =
    AuthBody { implicit ctx => _ =>
      OptionFuResult(api byId id) { c =>
        given play.api.mvc.Request[?] = ctx.body
        isForMe(c, ctx.me) ??
          api.decline(
            c,
            env.challenge.forms.decline
              .bindFromRequest()
              .fold(_ => ChallengeModel.DeclineReason.default, _.realReason)
          )
      }
    }
  def apiDecline(id: ID) =
    ScopedBody(_.Challenge.Write, _.Bot.Play, _.Board.Play) { implicit req => me =>
      given play.api.i18n.Lang = reqLang
      api.activeByIdFor(id, me) flatMap {
        case None =>
          env.bot.player.rematchDecline(GameId(id), me) flatMap {
            case true => jsonOkResult.toFuccess
            case _    => notFoundJson()
          }
        case Some(c) =>
          env.challenge.forms.decline
            .bindFromRequest()
            .fold(
              newJsonFormError,
              data => api.decline(c, data.realReason) inject jsonOkResult
            )
      }
    }

  def cancel(id: ID) =
    Open { implicit ctx =>
      OptionFuResult(api byId id) { c =>
        if (isMine(c)) api cancel c
        else notFound
      }
    }

  def apiCancel(id: ID) =
    Scoped(_.Challenge.Write, _.Bot.Play, _.Board.Play) { req => me =>
      api.activeByIdBy(id, me) flatMap {
        case Some(c) => api.cancel(c) inject jsonOkResult
        case None =>
          api.activeByIdFor(id, me) flatMap {
            case Some(c) => api.decline(c, ChallengeModel.DeclineReason.default) inject jsonOkResult
            case None =>
              import lila.hub.actorApi.map.Tell
              import lila.hub.actorApi.round.Abort
              import lila.round.actorApi.round.AbortForce
              env.game.gameRepo game GameId(id) dmap {
                _ flatMap { Pov.ofUserId(_, me.id) }
              } flatMap {
                _ ?? { p => env.round.proxyRepo.upgradeIfPresent(p) dmap some }
              } flatMap {
                case Some(pov) if pov.game.abortableByUser =>
                  lila.common.Bus.publish(Tell(id, Abort(pov.playerId)), "roundSocket")
                  jsonOkResult.toFuccess
                case Some(pov) if pov.game.playable =>
                  get("opponentToken", req).map(Bearer.apply) match
                    case None => BadRequest(jsonError("The game can no longer be aborted")).toFuccess
                    case Some(bearer) =>
                      env.oAuth.server.auth(bearer, List(OAuthScope.Challenge.Write), req.some) map {
                        case Right(OAuthScope.Scoped(op, _)) if pov.opponent.isUser(op) =>
                          lila.common.Bus.publish(Tell(id, AbortForce), "roundSocket")
                          jsonOkResult
                        case Right(_)  => BadRequest(jsonError("Not the opponent token"))
                        case Left(err) => BadRequest(jsonError(err.message))
                      }
                case _ => notFoundJson()
              }
          }
      }
    }

  def apiStartClocks(id: GameId) =
    Action.async { req =>
      import cats.implicits.*
      val scopes = List(OAuthScope.Challenge.Write)
      (get("token1", req) map Bearer.apply, get("token2", req) map Bearer.apply).mapN {
        env.oAuth.server.authBoth(scopes, req)
      } ?? {
        _ flatMap {
          case Left(e) => handleScopedFail(scopes, e)
          case Right((u1, u2)) =>
            env.game.gameRepo game id flatMap {
              _ ?? { g =>
                env.round.proxyRepo.upgradeIfPresent(g) dmap some dmap
                  (_.filter(_.hasUserIds(u1.id, u2.id)))
              }
            } map {
              _ ?? { game =>
                env.round.tellRound(game.id, lila.round.actorApi.round.StartClock)
                jsonOkResult
              }
            }
        }
      }
    }

  private val ChallengeIpRateLimit = lila.memo.RateLimit[IpAddress](
    500,
    10.minute,
    key = "challenge.create.ip"
  )

  private val BotChallengeIpRateLimit = lila.memo.RateLimit[IpAddress](
    400,
    1.day,
    key = "challenge.bot.create.ip"
  )

  private val ChallengeUserRateLimit = lila.memo.RateLimit.composite[lila.user.User.ID](
    key = "challenge.create.user"
  )(
    ("fast", 5 * 5, 1.minute),
    ("slow", 40 * 5, 1.day)
  )

  def toFriend(id: String) =
    AuthBody { implicit ctx => _ =>
      import play.api.data.*
      import play.api.data.Forms.*
      implicit val req: Request[?] = ctx.body
      OptionFuResult(api byId id) { c =>
        if (isMine(c))
          Form(
            single(
              "username" -> lila.user.UserForm.historicalUsernameField
            )
          ).bindFromRequest()
            .fold(
              _ => funit,
              username =>
                ChallengeIpRateLimit(ctx.ip) {
                  env.user.repo named username flatMap {
                    case None                       => Redirect(routes.Challenge.show(c.id)).toFuccess
                    case Some(dest) if ctx.is(dest) => Redirect(routes.Challenge.show(c.id)).toFuccess
                    case Some(dest) =>
                      env.challenge.granter.isDenied(ctx.me, dest, c.perfType.some) flatMap {
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

  def apiCreate(username: String) =
    ScopedBody(_.Challenge.Write, _.Bot.Play, _.Board.Play) { implicit req => me =>
      given play.api.i18n.Lang = reqLang
      !me.is(username) ?? env.setup.forms.api
        .user(me)
        .bindFromRequest()
        .fold(
          newJsonFormError,
          config =>
            ChallengeIpRateLimit(HTTPRequest ipAddress req, cost = if (me.isApiHog) 0 else 1) {
              env.user.repo enabledByName username flatMap {
                case None => JsonBadRequest(jsonError(s"No such user: $username")).toFuccess
                case Some(destUser) =>
                  val cost = if (me.isApiHog) 0 else if (destUser.isBot) 1 else 5
                  BotChallengeIpRateLimit(HTTPRequest ipAddress req, cost = if (me.isBot) 1 else 0) {
                    ChallengeUserRateLimit(me.id, cost = cost) {
                      val challenge = makeOauthChallenge(config, me, destUser)
                      config.acceptByToken match {
                        case Some(strToken) =>
                          apiChallengeAccept(destUser, challenge, strToken)(me, config.message)
                        case _ =>
                          env.challenge.granter.isDenied(me.some, destUser, config.perfType) flatMap {
                            case Some(denied) =>
                              JsonBadRequest(
                                jsonError(lila.challenge.ChallengeDenied.translated(denied))
                              ).toFuccess
                            case _ =>
                              env.challenge.api create challenge map {
                                case true =>
                                  val json = env.challenge.jsonView
                                    .show(challenge, SocketVersion(0), lila.challenge.Direction.Out.some)
                                  if (config.keepAliveStream)
                                    apiC.sourceToNdJsonOption(
                                      apiC.addKeepAlive(env.challenge.keepAliveStream(challenge, json))
                                    )
                                  else JsonOk(json)
                                case false =>
                                  JsonBadRequest(jsonError("Challenge not created"))
                              }
                          }
                      }
                    }(rateLimitedFu)
                  }(rateLimitedFu)
              }
            }(rateLimitedFu)
        )
    }

  private def makeOauthChallenge(config: ApiConfig, orig: UserModel, dest: UserModel) =
    import lila.challenge.Challenge.*
    val timeControl = TimeControl.make(config.clock, config.days)
    lila.challenge.Challenge
      .make(
        variant = config.variant,
        initialFen = config.position,
        timeControl = timeControl,
        mode = config.mode,
        color = config.color.name,
        challenger = ChallengeModel.toRegistered(config.variant, timeControl)(orig),
        destUser = dest.some,
        rematchOf = none,
        rules = config.rules
      )

  private def apiChallengeAccept(
      dest: UserModel,
      challenge: lila.challenge.Challenge,
      strToken: String
  )(managedBy: lila.user.User, message: Option[Template])(implicit req: RequestHeader) =
    env.oAuth.server.auth(
      Bearer(strToken),
      List(lila.oauth.OAuthScope.Challenge.Write),
      req.some
    ) flatMap {
      _.fold(
        err => BadRequest(jsonError(err.message)).toFuccess,
        scoped =>
          if (scoped.user is dest) env.challenge.api.oauthAccept(dest, challenge) flatMap {
            case Validated.Valid(g) =>
              env.challenge.msg.onApiPair(challenge)(managedBy, message) inject Ok(
                Json.obj(
                  "game" -> {
                    env.game.jsonView(g, challenge.initialFen) ++ Json.obj(
                      "url" -> s"${env.net.baseUrl}${routes.Round.watcher(g.id, "white")}"
                    )
                  }
                )
              )
            case Validated.Invalid(err) => BadRequest(jsonError(err)).toFuccess
          }
          else BadRequest(jsonError("dest and accept user don't match")).toFuccess
      )
    }

  def openCreate =
    Action.async { implicit req =>
      given play.api.i18n.Lang = reqLang
      env.setup.forms.api.open
        .bindFromRequest()
        .fold(
          err => BadRequest(apiFormError(err)).toFuccess,
          config =>
            ChallengeIpRateLimit(HTTPRequest ipAddress req) {
              import lila.challenge.Challenge.*
              val challenge = lila.challenge.Challenge.make(
                variant = config.variant,
                initialFen = config.position,
                timeControl = TimeControl.make(config.clock, config.days),
                mode = chess.Mode(config.rated),
                color = "random",
                challenger = Challenger.Open,
                destUser = none,
                rematchOf = none,
                name = config.name,
                openToUserIds = config.userIds,
                rules = config.rules
              )
              (env.challenge.api create challenge) map {
                case true =>
                  JsonOk(
                    env.challenge.jsonView.show(challenge, SocketVersion(0), none) ++ Json.obj(
                      "urlWhite" -> s"${env.net.baseUrl}/${challenge.id}?color=white",
                      "urlBlack" -> s"${env.net.baseUrl}/${challenge.id}?color=black"
                    )
                  )
                case false => BadRequest(jsonError("Challenge not created"))
              }
            }(rateLimitedFu).dmap(_ as JSON)
        )
    }

  def offerRematchForGame(gameId: GameId) =
    Auth { implicit ctx => me =>
      NoBot {
        OptionFuResult(env.game.gameRepo game gameId) { g =>
          Pov.opponentOfUserId(g, me.id).flatMap(_.userId) ?? env.user.repo.byId flatMap {
            _ ?? { opponent =>
              env.challenge.granter.isDenied(me.some, opponent, g.perfType) flatMap {
                case Some(d) => BadRequest(jsonError(lila.challenge.ChallengeDenied translated d)).toFuccess
                case _ =>
                  api.offerRematchForGame(g, me) map {
                    case true => jsonOkResult
                    case _    => BadRequest(jsonError("Sorry, couldn't create the rematch."))
                  }
              }
            }
          }
        }
      }
    }
