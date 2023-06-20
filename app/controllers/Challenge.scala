package controllers

import cats.data.Validated
import play.api.libs.json.Json
import play.api.mvc.{ RequestHeader, Result }
import views.html

import lila.app.{ given, * }
import lila.challenge.{ Challenge as ChallengeModel }
import lila.challenge.Challenge.{ Id as ChallengeId }
import lila.common.{ Bearer, IpAddress, Template }
import lila.game.{ AnonCookie, Pov }
import lila.oauth.{ OAuthScope, EndpointScopes }
import lila.setup.ApiConfig
import lila.socket.SocketVersion
import lila.user.{ Me, User as UserModel }

final class Challenge(
    env: Env,
    apiC: Api
) extends LilaController(env):

  def api = env.challenge.api

  def all = Auth { ctx ?=> me ?=>
    XhrOrRedirectHome:
      api allFor me map env.challenge.jsonView.apply map JsonOk
  }

  def apiList = ScopedBody(_.Challenge.Read) { ctx ?=> me ?=>
    api.allFor(me, 300).map { all =>
      JsonOk:
        Json.obj(
          "in"  -> all.in.map(env.challenge.jsonView.apply(lila.challenge.Direction.In.some)),
          "out" -> all.out.map(env.challenge.jsonView.apply(lila.challenge.Direction.Out.some))
        )
    }
  }

  def show(id: ChallengeId, _color: Option[String]) = Open:
    showId(id)

  protected[controllers] def showId(id: ChallengeId)(using WebContext): Fu[Result] =
    OptionFuResult(api byId id)(showChallenge(_))

  protected[controllers] def showChallenge(
      c: ChallengeModel,
      error: Option[String] = None,
      justCreated: Boolean = false
  )(using ctx: WebContext): Fu[Result] =
    env.challenge version c.id flatMap { version =>
      val mine = justCreated || isMine(c)
      import lila.challenge.Direction
      val direction: Option[Direction] =
        if mine then Direction.Out.some
        else if isForMe(c) then Direction.In.some
        else none
      val json = env.challenge.jsonView.show(c, version, direction)
      negotiate(
        html = {
          val color = get("color") flatMap chess.Color.fromName
          if mine then
            error match
              case Some(e) => BadRequest(html.challenge.mine(c, json, e.some, color))
              case None    => Ok(html.challenge.mine(c, json, none, color))
          else
            (c.challengerUserId so env.user.repo.byId) map { user =>
              Ok(html.challenge.theirs(c, json, user, color))
            }
        },
        api = _ => Ok(json)
      ) flatMap withChallengeAnonCookie(mine && c.challengerIsAnon, c, owner = true)
    } map env.lilaCookie.ensure(ctx.req)

  private def isMine(challenge: ChallengeModel)(using WebContext) =
    challenge.challenger match
      case lila.challenge.Challenge.Challenger.Anonymous(secret)     => ctx.req.sid contains secret
      case lila.challenge.Challenge.Challenger.Registered(userId, _) => ctx.userId contains userId
      case lila.challenge.Challenge.Challenger.Open                  => false

  private def isForMe(challenge: ChallengeModel)(using me: Option[Me]) =
    challenge.destUserId.fold(true)(dest => me.exists(_ is dest)) &&
      !challenge.challengerUserId.so(orig => me.exists(_ is orig))

  def accept(id: ChallengeId, color: Option[String]) = Open:
    OptionFuResult(api byId id): c =>
      val cc = color flatMap chess.Color.fromName
      isForMe(c) so api
        .accept(c, ctx.req.sid, cc)
        .flatMap {
          case Validated.Valid(Some(pov)) =>
            negotiate(
              html = Redirect(routes.Round.watcher(pov.gameId, cc.fold("white")(_.name))),
              api = apiVersion => env.api.roundApi.player(pov, none, apiVersion) map { Ok(_) }
            ) flatMap withChallengeAnonCookie(ctx.isAnon, c, owner = false)
          case invalid =>
            negotiate(
              html = Redirect(routes.Round.watcher(c.id.value, cc.fold("white")(_.name))),
              api = _ =>
                notFoundJson(invalid match
                  case Validated.Invalid(err) => err
                  case _                      => "The challenge has already been accepted"
                )
            )
        }

  def apiAccept(id: ChallengeId) =
    Scoped(_.Challenge.Write, _.Bot.Play, _.Board.Play) { _ ?=> me ?=>
      def tryRematch =
        env.bot.player.rematchAccept(id into GameId) flatMap {
          if _ then jsonOkResult
          else notFoundJson()
        }
      api.byId(id) flatMap {
        _.filter(isForMe) match
          case None                  => tryRematch
          case Some(c) if c.accepted => tryRematch
          case Some(c) =>
            api.accept(c, none) map {
              _.fold(err => BadRequest(jsonError(err)), _ => jsonOkResult)
            }
      }
    }

  private def withChallengeAnonCookie(cond: Boolean, c: ChallengeModel, owner: Boolean)(
      res: Result
  )(using WebContext): Fu[Result] =
    cond so {
      env.game.gameRepo.game(c.id into GameId).map {
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
      cookieOption.foldLeft(res)(_ withCookies _)
    }

  def decline(id: ChallengeId) = AuthBody { ctx ?=> _ ?=>
    OptionFuResult(api byId id): c =>
      isForMe(c) so
        api.decline(
          c,
          env.challenge.forms.decline
            .bindFromRequest()
            .fold(_ => ChallengeModel.DeclineReason.default, _.realReason)
        )
  }
  def apiDecline(id: ChallengeId) = ScopedBody(_.Challenge.Write, _.Bot.Play, _.Board.Play) { ctx ?=> me ?=>
    api.activeByIdFor(id, me) flatMap {
      case None =>
        env.bot.player.rematchDecline(id into GameId) flatMap {
          if _ then jsonOkResult
          else notFoundJson()
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

  def cancel(id: ChallengeId) =
    Open:
      OptionFuResult(api byId id): c =>
        if isMine(c) then api cancel c else notFound

  def apiCancel(id: ChallengeId) = Scoped(_.Challenge.Write, _.Bot.Play, _.Board.Play) { ctx ?=> me ?=>
    api.activeByIdBy(id, me) flatMap {
      case Some(c) => api.cancel(c) inject jsonOkResult
      case None =>
        api.activeByIdFor(id, me) flatMap {
          case Some(c) => api.decline(c, ChallengeModel.DeclineReason.default) inject jsonOkResult
          case None =>
            import lila.hub.actorApi.map.Tell
            import lila.hub.actorApi.round.Abort
            import lila.round.actorApi.round.AbortForce
            env.game.gameRepo game id.into(GameId) dmap {
              _ flatMap { Pov(_, me) }
            } flatMapz { p =>
              env.round.proxyRepo.upgradeIfPresent(p) dmap some
            } flatMap {
              case Some(pov) if pov.game.abortableByUser =>
                lila.common.Bus.publish(Tell(id.value, Abort(pov.playerId)), "roundSocket")
                jsonOkResult
              case Some(pov) if pov.game.playable =>
                Bearer.from(get("opponentToken")) match
                  case Some(bearer) =>
                    val required = OAuthScope.select(_.Challenge.Write) into EndpointScopes
                    env.oAuth.server.auth(bearer, required, ctx.req.some) map {
                      case Right(OAuthScope.Scoped(op, _)) if pov.opponent.isUser(op) =>
                        lila.common.Bus.publish(Tell(id.value, AbortForce), "roundSocket")
                        jsonOkResult
                      case Right(_)  => BadRequest(jsonError("Not the opponent token"))
                      case Left(err) => BadRequest(jsonError(err.message))
                    }
                  case None if api.isOpenBy(id, me) =>
                    if pov.game.abortable then
                      lila.common.Bus.publish(Tell(id.value, AbortForce), "roundSocket")
                      jsonOkResult
                    else BadRequest(jsonError("The game can no longer be aborted"))
                  case None => BadRequest(jsonError("Missing opponentToken"))
              case _ => notFoundJson()
            }
        }
    }
  }

  def apiStartClocks(id: GameId) = Anon:
    import cats.syntax.all.*
    val accepted = OAuthScope.select(_.Challenge.Write) into EndpointScopes
    (Bearer from get("token1"), Bearer from get("token2"))
      .mapN:
        env.oAuth.server.authBoth(accepted, req)
      .so:
        _.flatMap:
          case Left(e) => handleScopedFail(accepted, e)
          case Right((u1, u2)) =>
            env.game.gameRepo game id flatMapz { g =>
              env.round.proxyRepo.upgradeIfPresent(g) dmap some dmap
                (_.filter(_.hasUserIds(u1.id, u2.id)))
            } mapz { game =>
              env.round.tellRound(game.id, lila.round.actorApi.round.StartClock)
              jsonOkResult
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

  private val ChallengeUserRateLimit = lila.memo.RateLimit.composite[UserId](
    key = "challenge.create.user"
  )(
    ("fast", 5 * 5, 1.minute),
    ("slow", 40 * 5, 1.day)
  )

  def toFriend(id: ChallengeId) = AuthBody { ctx ?=> _ ?=>
    import play.api.data.*
    import play.api.data.Forms.*
    OptionFuResult(api byId id): c =>
      if isMine(c) then
        Form(single("username" -> lila.user.UserForm.historicalUsernameField))
          .bindFromRequest()
          .fold(
            _ => funit,
            username =>
              ChallengeIpRateLimit(ctx.ip, rateLimitedFu):
                env.user.repo byId username flatMap {
                  case None                       => Redirect(routes.Challenge.show(c.id))
                  case Some(dest) if ctx.is(dest) => Redirect(routes.Challenge.show(c.id))
                  case Some(dest) =>
                    env.challenge.granter.isDenied(ctx.me, dest, c.perfType.some) flatMap {
                      case Some(denied) =>
                        showChallenge(c, lila.challenge.ChallengeDenied.translated(denied).some)
                      case None => api.setDestUser(c, dest) inject Redirect(routes.Challenge.show(c.id))
                    }
                }
          )
      else notFound
  }

  def apiCreate(username: UserStr) =
    ScopedBody(_.Challenge.Write, _.Bot.Play, _.Board.Play, _.Web.Mobile) { ctx ?=> me ?=>
      !me.is(username) so env.setup.forms.api.user
        .bindFromRequest()
        .fold(
          newJsonFormError,
          config =>
            ChallengeIpRateLimit(req.ipAddress, rateLimitedFu, cost = if me.isApiHog then 0 else 1):
              env.user.repo enabledById username flatMap {
                case None => JsonBadRequest(jsonError(s"No such user: $username"))
                case Some(destUser) =>
                  val cost = if me.isApiHog then 0 else if destUser.isBot then 1 else 5
                  BotChallengeIpRateLimit(req.ipAddress, rateLimitedFu, cost = if me.isBot then 1 else 0):
                    ChallengeUserRateLimit(me, rateLimitedFu, cost = cost):
                      val challenge = makeOauthChallenge(config, me, destUser)
                      config.acceptByToken match
                        case Some(strToken) =>
                          apiChallengeAccept(destUser, challenge, strToken)(me, config.message)
                        case _ =>
                          env.challenge.granter
                            .isDenied(me.some, destUser, config.perfType)
                            .flatMap:
                              case Some(denied) =>
                                JsonBadRequest:
                                  jsonError(lila.challenge.ChallengeDenied.translated(denied))
                              case _ =>
                                env.challenge.api create challenge map {
                                  if _ then
                                    val json = env.challenge.jsonView
                                      .show(challenge, SocketVersion(0), lila.challenge.Direction.Out.some)
                                    if (config.keepAliveStream)
                                      apiC.sourceToNdJsonOption(
                                        apiC.addKeepAlive(env.challenge.keepAliveStream(challenge, json))
                                      )
                                    else JsonOk(json)
                                  else JsonBadRequest(jsonError("Challenge not created"))
                                }
              }
        )
    }

  private def makeOauthChallenge(config: ApiConfig, orig: UserModel, dest: UserModel) =
    import lila.challenge.Challenge.*
    val timeControl = TimeControl.make(config.clock, config.days)
    lila.challenge.Challenge.make(
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
  )(managedBy: lila.user.User, message: Option[Template])(using req: RequestHeader): Fu[Result] =
    val accepted = OAuthScope.select(_.Challenge.Write) into EndpointScopes
    env.oAuth.server
      .auth(Bearer(strToken), accepted, req.some)
      .flatMap:
        _.fold(
          err => BadRequest(jsonError(err.message)),
          scoped =>
            if scoped.me is dest
            then
              env.challenge.api.oauthAccept(dest, challenge) flatMap {
                case Validated.Valid(g) =>
                  env.challenge.msg.onApiPair(challenge)(managedBy, message) inject Ok:
                    Json.obj:
                      "game" -> {
                        env.game.jsonView.baseWithChessDenorm(g, challenge.initialFen) ++ Json.obj(
                          "url" -> s"${env.net.baseUrl}${routes.Round.watcher(g.id, "white")}"
                        )
                      }
                case Validated.Invalid(err) => BadRequest(jsonError(err))
              }
            else BadRequest(jsonError("dest and accept user don't match"))
        )

  def openCreate = AnonOrScopedBody(parse.anyContent)(_.Challenge.Write): ctx ?=>
    env.setup.forms.api.open
      .bindFromRequest()
      .fold(
        err => BadRequest(apiFormError(err)),
        config =>
          ChallengeIpRateLimit(req.ipAddress, rateLimitedFu):
            import lila.challenge.Challenge.*
            env.challenge.api
              .createOpen(config)
              .map: challenge =>
                JsonOk:
                  env.challenge.jsonView.show(challenge, SocketVersion(0), none) ++ Json.obj(
                    "urlWhite" -> s"${env.net.baseUrl}/${challenge.id}?color=white",
                    "urlBlack" -> s"${env.net.baseUrl}/${challenge.id}?color=black"
                  )
          .dmap(_ as JSON)
      )

  def offerRematchForGame(gameId: GameId) = Auth { _ ?=> me ?=>
    NoBot:
      OptionFuResult(env.game.gameRepo game gameId): g =>
        Pov.opponentOf(g, me).flatMap(_.userId) so env.user.repo.byId flatMapz { opponent =>
          env.challenge.granter.isDenied(me.some, opponent, g.perfType) flatMap {
            case Some(d) => BadRequest(jsonError(lila.challenge.ChallengeDenied translated d))
            case _ =>
              api.offerRematchForGame(g, me) map {
                if _ then jsonOkResult
                else BadRequest(jsonError("Sorry, couldn't create the rematch."))
              }
          }
        }
  }
