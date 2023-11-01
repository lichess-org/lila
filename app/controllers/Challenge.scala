package controllers

import play.api.libs.json.Json
import play.api.mvc.{ RequestHeader, Result }
import views.html

import lila.app.{ given, * }
import lila.challenge.{ Challenge as ChallengeModel }
import lila.challenge.Challenge.{ Id as ChallengeId }
import lila.common.{ Bearer, IpAddress, Template, Preload }
import lila.game.{ AnonCookie, Pov }
import lila.oauth.{ OAuthScope, EndpointScopes }
import lila.setup.ApiConfig
import lila.socket.SocketVersion
import lila.user.{ User as UserModel }

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

  protected[controllers] def showId(id: ChallengeId)(using Context): Fu[Result] =
    Found(api byId id)(showChallenge(_))

  protected[controllers] def showChallenge(
      c: ChallengeModel,
      error: Option[String] = None,
      justCreated: Boolean = false
  )(using ctx: Context): Fu[Result] =
    env.challenge version c.id flatMap { version =>
      val mine = justCreated || isMine(c)
      import lila.challenge.Direction
      val direction: Option[Direction] =
        if mine then Direction.Out.some
        else if isForMe(c) then Direction.In.some
        else none
      val json = env.challenge.jsonView.show(c, version, direction)
      negotiate(
        html =
          val color = get("color") flatMap chess.Color.fromName
          if mine then
            error match
              case Some(e) => BadRequest.page(html.challenge.mine(c, json, e.some, color))
              case None    => Ok.page(html.challenge.mine(c, json, none, color))
          else
            Ok.pageAsync:
              c.challengerUserId.so(env.user.api.withPerf(_, c.perfType)).map {
                html.challenge.theirs(c, json, _, color)
              }
        ,
        json = Ok(json)
      ) flatMap withChallengeAnonCookie(mine && c.challengerIsAnon, c, owner = true)
    } map env.lilaCookie.ensure(ctx.req)

  private def isMine(challenge: ChallengeModel)(using Context) =
    challenge.challenger match
      case lila.challenge.Challenge.Challenger.Anonymous(secret)     => ctx.req.sid contains secret
      case lila.challenge.Challenge.Challenger.Registered(userId, _) => ctx.userId contains userId
      case lila.challenge.Challenge.Challenger.Open                  => false

  private def isForMe(challenge: ChallengeModel)(using me: Option[Me]) =
    challenge.destUserId.forall(dest => me.exists(_ is dest)) &&
      !challenge.challengerUserId.so(orig => me.exists(_ is orig))

  def accept(id: ChallengeId, color: Option[String]) = Open:
    Found(api byId id): c =>
      val cc = color flatMap chess.Color.fromName
      isForMe(c) so api
        .accept(c, ctx.req.sid, cc)
        .flatMap:
          case Right(Some(pov)) =>
            negotiateApi(
              html = Redirect(routes.Round.watcher(pov.gameId, cc.fold("white")(_.name))),
              api = _ => env.api.roundApi.player(pov, Preload.none, none) map { Ok(_) }
            ) flatMap withChallengeAnonCookie(ctx.isAnon, c, owner = false)
          case invalid =>
            negotiate(
              Redirect(routes.Round.watcher(c.id.value, cc.fold("white")(_.name))),
              notFoundJson(invalid match
                case Left(err) => err
                case _         => "The challenge has already been accepted"
              )
            )

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
  )(using Context): Fu[Result] =
    cond so {
      env.game.gameRepo.game(c.id into GameId).map {
        _ map { game =>
          env.lilaCookie.cookie(
            AnonCookie.name,
            game.player(if owner then c.finalColor else !c.finalColor).id.value,
            maxAge = AnonCookie.maxAge.some,
            httpOnly = false.some
          )
        }
      }
    } map { cookieOption =>
      cookieOption.foldLeft(res)(_ withCookies _)
    }

  def decline(id: ChallengeId) = AuthBody { ctx ?=> _ ?=>
    Found(api byId id): c =>
      isForMe(c).so:
        api.decline(
          c,
          env.challenge.forms.decline
            .bindFromRequest()
            .fold(_ => ChallengeModel.DeclineReason.default, _.realReason)
        ) inject NoContent
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
            jsonFormError,
            data => api.decline(c, data.realReason) inject jsonOkResult
          )
    }
  }

  def cancel(id: ChallengeId) =
    Open:
      Found(api byId id): c =>
        if isMine(c)
        then api cancel c inject NoContent
        else notFound

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
                      case Right(access) if pov.opponent.isUser(access.user) =>
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
            } orNotFound { game =>
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
    Found(api byId id): c =>
      if isMine(c) then
        Form(single("username" -> lila.user.UserForm.historicalUsernameField))
          .bindFromRequest()
          .fold(
            _ => NoContent,
            username =>
              ChallengeIpRateLimit(ctx.ip, rateLimited):
                env.user.repo byId username flatMap {
                  case None                       => Redirect(routes.Challenge.show(c.id))
                  case Some(dest) if ctx.is(dest) => Redirect(routes.Challenge.show(c.id))
                  case Some(dest) =>
                    env.challenge.granter.isDenied(dest, c.perfType) flatMap {
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
          doubleJsonFormError,
          config =>
            ChallengeIpRateLimit(req.ipAddress, rateLimited, cost = if me.isApiHog then 0 else 1):
              env.user.repo enabledById username flatMap {
                case None => JsonBadRequest(jsonError(s"No such user: $username"))
                case Some(destUser) =>
                  val cost = if me.isApiHog then 0 else if destUser.isBot then 1 else 5
                  BotChallengeIpRateLimit(req.ipAddress, rateLimited, cost = if me.isBot then 1 else 0):
                    ChallengeUserRateLimit(me, rateLimited, cost = cost):
                      for
                        challenge <- makeOauthChallenge(config, me, destUser)
                        grant     <- env.challenge.granter.isDenied(destUser, config.perfType)
                        res <- grant match
                          case Some(denied) =>
                            fuccess:
                              JsonBadRequest:
                                jsonError(lila.challenge.ChallengeDenied.translated(denied))
                          case _ =>
                            env.challenge.api create challenge map {
                              if _ then
                                val json = env.challenge.jsonView
                                  .show(challenge, SocketVersion(0), lila.challenge.Direction.Out.some)
                                if config.keepAliveStream then
                                  apiC.sourceToNdJsonOption:
                                    apiC.addKeepAlive(env.challenge.keepAliveStream(challenge, json))
                                else JsonOk(json)
                              else JsonBadRequest(jsonError("Challenge not created"))
                            }
                      yield res
              }
        )
    }

  private def makeOauthChallenge(config: ApiConfig, orig: UserModel, dest: UserModel) =
    import lila.challenge.Challenge.*
    val timeControl = TimeControl.make(config.clock, config.days)
    env.user.perfsRepo
      .withPerf(orig -> dest, config.perfType, _.sec)
      .map: (orig, dest) =>
        lila.challenge.Challenge.make(
          variant = config.variant,
          initialFen = config.position,
          timeControl = timeControl,
          mode = config.mode,
          color = config.color.name,
          challenger = ChallengeModel.toRegistered(orig),
          destUser = dest.some,
          rematchOf = none,
          rules = config.rules
        )

  def openCreate = AnonOrScopedBody(parse.anyContent)(_.Challenge.Write): ctx ?=>
    env.setup.forms.api
      .open(isAdmin = isGrantedOpt(_.ApiChallengeAdmin) || ctx.me.exists(_.isVerified))
      .bindFromRequest()
      .fold(
        jsonFormError,
        config =>
          ChallengeIpRateLimit(req.ipAddress, rateLimited):
            import lila.challenge.Challenge.*
            env.challenge.api
              .createOpen(config)
              .map: challenge =>
                JsonOk:
                  val url = s"${env.net.baseUrl}/${challenge.id}"
                  env.challenge.jsonView.show(challenge, SocketVersion(0), none) ++ Json.obj(
                    "urlWhite" -> s"$url?color=white",
                    "urlBlack" -> s"$url?color=black"
                  )
          .dmap(_ as JSON)
      )

  def offerRematchForGame(gameId: GameId) = Auth { _ ?=> me ?=>
    NoBot:
      Found(env.game.gameRepo game gameId): g =>
        g.opponentOf(me).flatMap(_.userId) so env.user.repo.byId orNotFound { opponent =>
          env.challenge.granter.isDenied(opponent, g.perfType) flatMap {
            case Some(d) => BadRequest(jsonError(lila.challenge.ChallengeDenied translated d))
            case _ =>
              api.offerRematchForGame(g, me) map {
                if _ then jsonOkResult
                else BadRequest(jsonError("Sorry, couldn't create the rematch."))
              }
          }
        }
  }
