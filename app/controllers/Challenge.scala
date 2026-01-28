package controllers

import play.api.libs.json.Json
import play.api.mvc.Result

import lila.app.{ *, given }
import lila.challenge.{ Challenge as ChallengeModel, Direction }
import lila.core.id.ChallengeId
import lila.core.net.Bearer
import lila.game.AnonCookie
import lila.oauth.{ EndpointScopes, OAuthScope, OAuthServer }
import lila.setup.ApiConfig
import lila.memo.RateLimit.Limited

final class Challenge(env: Env) extends LilaController(env):
  def api = env.challenge.api

  def all = Auth { ctx ?=> me ?=>
    XhrOrRedirectHome:
      api.allFor(me).map(env.challenge.jsonView.apply).map(JsonOk)
  }

  def apiList = ScopedBody(_.Challenge.Read, _.Web.Mobile) { ctx ?=> me ?=>
    for all <- api.allFor(me, 300)
    yield JsonOk(env.challenge.jsonView.all(all))
  }

  def show(id: ChallengeId, @annotation.nowarn color: Option[Color]) = Open:
    showId(id)

  def apiShow(id: ChallengeId) = AnonOrScoped(_.Challenge.Read, _.Web.Mobile) { ctx ?=>
    Found(api.byId(id)): c =>
      val direction: Option[Direction] =
        if isMine(c) then Direction.Out.some
        else if isForMe(c) then Direction.In.some
        else none
      direction.so: dir =>
        for
          fullId <- c.accepted.so(env.round.proxyRepo.game(c.gameId).map2(c.fullIdOf(_, dir)))
          socketVersion <- ctx.isMobileOauth.so(env.challenge.version(c.id).dmap(some))
          json = env.challenge.jsonView.apiAndMobile(c, socketVersion, dir.some, fullId)
        yield JsonOk(json)
  }

  protected[controllers] def showId(id: ChallengeId)(using Context): Fu[Result] =
    Found(api.byId(id))(showChallenge(_))

  protected[controllers] def showChallenge(
      c: ChallengeModel,
      error: Option[String] = None,
      justCreated: Boolean = false
  )(using ctx: Context): Fu[Result] = for
    version <- env.challenge.version(c.id)
    mine = justCreated || isMine(c)
    direction: Option[Direction] =
      if mine then Direction.Out.some
      else if isForMe(c) then Direction.In.some
      else none
    json = env.challenge.jsonView.websiteAndLichobile(c, version, direction)
    res <- negotiate(
      html =
        val color = get("color").flatMap(Color.fromName)
        if mine then
          ctx.userId
            .so(env.game.gameRepo.recentChallengersOf(_, Max(10)))
            .flatMap(env.user.lightUserApi.asyncManyFallback)
            .flatMap: friends =>
              error match
                case Some(e) => BadRequest.page(views.challenge.mine(c, json, friends, e.some, color))
                case None => Ok.page(views.challenge.mine(c, json, friends, none, color))
        else
          Ok.async:
            for
              challenger <- c.challengerUserId.so(env.user.api.byIdWithPerf(_, c.perfType))
              relation <- (ctx.userId, c.challengerUserId).tupled.so(env.relation.api.fetchRelation.tupled)
            yield views.challenge.theirs(c, json, challenger, color, relation)
      ,
      json = Ok(json)
    ).flatMap(withChallengeAnonCookie(mine && c.challengerIsAnon, c, owner = true))
  yield env.security.lilaCookie.ensure(ctx.req)(res)

  private def isMine(challenge: ChallengeModel)(using Context) =
    challenge.challenger match
      case lila.challenge.Challenge.Challenger.Anonymous(secret) => ctx.req.sid.contains(secret)
      case lila.challenge.Challenge.Challenger.Registered(userId, _) => ctx.is(userId)
      case lila.challenge.Challenge.Challenger.Open => false

  private def isForMe(challenge: ChallengeModel)(using me: Option[Me]) =
    challenge.destUserId.forall(dest => me.exists(_.is(dest))) &&
      !challenge.challengerUserId.so(orig => me.exists(_.is(orig)))

  import cats.mtl.Handle.*
  def accept(id: ChallengeId, color: Option[Color]) = Open:
    Found(api.byId(id)): c =>
      isForMe(c).so:
        allow:
          api
            .accept(c, ctx.req.sid, color)
            .flatMap:
              _.fold("The Challenge has already been accepted".raise): pov =>
                negotiateApi(
                  html = Redirect(routes.Round.watcher(pov.gameId, color | Color.white)),
                  api = _ => env.api.roundApi.player(pov, scalalib.data.Preload.none, none).map { Ok(_) }
                )
            .flatMap(withChallengeAnonCookie(ctx.isAnon, c, owner = false))
        .rescue: err =>
          negotiate(
            Redirect(routes.Round.watcher(c.gameId, color | Color.white)),
            notFoundJson(err)
          )

  private def eitherBotLimitResponse(l: lila.bot.EitherBotLimit) = fuccess:
    l match
      case l: Limited => JsonLimited(l)
      case l: lila.bot.OpponentLimit =>
        import lila.bot.BotLimit.given
        BadRequest(Json.toJson(l))

  def apiAccept(id: ChallengeId, color: Option[Color]) =
    AnonOrScoped(_.Challenge.Write, _.Bot.Play, _.Board.Play, _.Web.Mobile) { ctx ?=>
      def tryRematch = ctx.useMe:
        env.bot.player
          .rematchAccept(id.into(GameId))
          .flatMap:
            case l: lila.bot.EitherBotLimit => eitherBotLimitResponse(l)
            case res: Boolean => if res then jsonOkResult else notFoundJson()
      api
        .byId(id)
        .flatMap:
          _.filter(isForMe) match
            case None => tryRematch
            case Some(c) if c.accepted => tryRematch
            case Some(c) =>
              ctx
                .useMe(c.challengerUserId.so(env.bot.limit.acceptLimitError))
                .map(eitherBotLimitResponse)
                .getOrElse:
                  allow:
                    api.accept(c, none, color).inject(jsonOkResult)
                  .rescue: err =>
                    fuccess(BadRequest(jsonError(err)))
    }

  private def withChallengeAnonCookie(cond: Boolean, c: ChallengeModel, owner: Boolean)(
      res: Result
  ): Fu[Result] =
    cond
      .so:
        env.game.gameRepo.game(c.id.into(GameId)).map2 { game =>
          env.security.lilaCookie.cookie(
            AnonCookie.name,
            game.player(if owner then c.finalColor else !c.finalColor).id.value,
            maxAge = AnonCookie.maxAge.some,
            httpOnly = false.some
          )
        }
      .map:
        _.foldLeft(res)(_.withCookies(_))

  def decline(id: ChallengeId) = AuthBody { ctx ?=> _ ?=>
    Found(api.byId(id)): c =>
      isForMe(c).so:
        api
          .decline(
            c,
            bindForm(env.challenge.forms.decline)(_ => ChallengeModel.DeclineReason.default, _.realReason)
          )
          .inject(NoContent)
  }
  def apiDecline(id: ChallengeId) = ScopedBody(_.Challenge.Write, _.Bot.Play, _.Board.Play, _.Web.Mobile):
    ctx ?=>
      me ?=>
        api
          .activeByIdFor(id, me)
          .flatMap:
            case None =>
              env.bot.player
                .rematchDecline(id.into(GameId))
                .flatMap:
                  if _ then jsonOkResult
                  else notFoundJson()
            case Some(c) =>
              bindForm(env.challenge.forms.decline)(
                jsonFormError,
                data => api.decline(c, data.realReason).inject(jsonOkResult)
              )

  def cancel(id: ChallengeId) =
    Open:
      Found(api.byId(id)): c =>
        if isMine(c)
        then api.cancel(c).inject(NoContent)
        else notFound

  def apiCancel(id: ChallengeId) = Scoped(_.Challenge.Write, _.Bot.Play, _.Board.Play, _.Web.Mobile): ctx ?=>
    me ?=>
      api
        .activeByIdBy(id, me)
        .flatMap:
          case Some(c) => api.cancel(c).inject(jsonOkResult)
          case None =>
            api
              .activeByIdFor(id, me)
              .flatMap:
                case Some(c) => api.decline(c, ChallengeModel.DeclineReason.default).inject(jsonOkResult)
                case None =>
                  import lila.core.round.{ Tell, RoundBus }
                  env.game.gameRepo
                    .game(id.into(GameId))
                    .dmap:
                      _.flatMap { Pov(_, me) }
                    .flatMapz: p =>
                      env.round.proxyRepo.upgradeIfPresent(p).dmap(some)
                    .flatMap:
                      case Some(pov) if pov.game.abortableByUser =>
                        lila.common.Bus.pub(Tell(pov.gameId, RoundBus.Abort(pov.playerId)))
                        jsonOkResult
                      case Some(pov) if pov.game.playable =>
                        Bearer.from(get("opponentToken")) match
                          case Some(bearer) =>
                            val required = OAuthScope.select(_.Challenge.Write).into(EndpointScopes)
                            allow:
                              for
                                access <- env.oAuth.server.auth(bearer, required, ctx.req.some)
                                _ <- raiseIf(!pov.opponent.isUser(access.me)):
                                  OAuthServer.AuthError("Not the opponent token")
                              yield
                                lila.common.Bus.pub(Tell(pov.gameId, RoundBus.AbortForce))
                                jsonOkResult
                            .rescue: err =>
                              BadRequest(jsonError(err.message))
                          case None if api.isOpenBy(id, me) =>
                            if pov.game.abortable then
                              lila.common.Bus.pub(Tell(pov.gameId, RoundBus.AbortForce))
                              jsonOkResult
                            else BadRequest(jsonError("The game can no longer be aborted"))
                          case None => BadRequest(jsonError("Missing opponentToken"))
                      case _ => notFoundJson()

  def apiStartClocks(id: GameId) = Anon:
    Found(env.round.proxyRepo.game(id)): game =>
      val accepted = OAuthScope.select(_.Challenge.Write).into(EndpointScopes)
      def startNow =
        env.round.roundApi.tell(game.id, lila.core.round.StartClock)
        jsonOkResult
      allow:
        for res <-
            if game.hasAi
            then
              getAs[Bearer]("token1")
                .traverse(env.oAuth.server.auth(_, accepted, req.some))
                .map:
                  _.exists(a => game.hasUserId(a.scoped.user.id)).option(startNow)
            else
              (getAs[Bearer]("token1"), getAs[Bearer]("token2"))
                .mapN(env.oAuth.server.authBoth(accepted, req))
                .so:
                  _.map: (u1, u2) =>
                    game.hasUserIds(u1.id, u2.id).option(startNow)
        yield res | notFoundJson()
      .rescue: err =>
        handleScopedFail(accepted, err)

  def toFriend(id: ChallengeId) = AuthBody { ctx ?=> _ ?=>
    Found(api.byId(id)): c =>
      if isMine(c) then
        bindForm(env.setup.forms.toFriend)(
          _ => NoContent,
          username =>
            limit.challenge(ctx.ip, rateLimited):
              def redir = Redirect(routes.Challenge.show(c.id))
              env.user.repo.byId(username).flatMap {
                case None => redir
                case Some(dest) if ctx.is(dest) => redir
                case Some(dest) =>
                  env.challenge.granter.isDenied(dest, c.perfType.key.some).flatMap {
                    case Some(denied) =>
                      showChallenge(c, lila.challenge.ChallengeDenied.translated(denied).some)
                    case None => api.setDestUser(c, dest).inject(redir)
                  }
              }
        )
      else notFound
  }

  def apiCreate(username: UserStr) =
    ScopedBody(_.Challenge.Write, _.Bot.Play, _.Board.Play, _.Web.Mobile) { ctx ?=> me ?=>
      bindForm(env.setup.forms.api.user)(
        doubleJsonFormError,
        config =>
          limit.challenge(req.ipAddress, rateLimited, cost = if me.isApiHog then 0 else 1):
            allow:
              for
                destUser <- env.user.repo.enabledById(username)
                destUser <- destUser.raiseIfNone(s"No such user: $username")
                _ <- raiseIf(me.is(destUser))("You cannot challenge yourself")
                _ <- raiseIf(destUser.isBot && !config.rules.isEmpty)("Rules not applicable for bots")
                isFriend <- env.relation.api.fetchFollows(destUser.id, me.userId)
                restricted = config.onlyIfOpponentFollowsMe && !isFriend
                _ <- raiseIf(restricted)(s"$username does not follow you")
                cost = if isFriend || me.isApiHog then 0 else if destUser.isBot then 1 else 5
                res <- limit.challengeUser(me, rateLimited, cost = cost):
                  env.bot.limit
                    .challengeLimitError(me.light, destUser.light)
                    .map(eitherBotLimitResponse)
                    .getOrElse:
                      for
                        challenge <- makeOauthChallenge(config, me, destUser)
                        denied <- env.challenge.granter.isDenied(destUser, config.perfKey.some)
                        _ <- raiseIfSome(denied.map(lila.challenge.ChallengeDenied.translated))(funit)
                        createNow <- env.challenge.api.delayedCreate(challenge)
                        createNow <- createNow.raiseIfNone("Challenge not created")
                        socket <- ctx.isMobileOauth.optionFu(env.challenge.version(challenge.id))
                        json = env.challenge.jsonView.apiAndMobile(
                          challenge,
                          socket,
                          lila.challenge.Direction.Out.some
                        )
                        res <-
                          if config.keepAliveStream then
                            val stream = env.challenge.keepAliveStream(challenge, json)(createNow)
                            jsOptToNdJson(ndJson.addKeepAlive(stream)).toFuccess
                          else createNow().inject(JsonOk(json))
                      yield res
              yield res
            .rescue: err =>
              BadRequest(jsonError(err))
      )
    }

  private def makeOauthChallenge(config: ApiConfig, orig: lila.user.User, dest: lila.user.User) =
    import lila.challenge.Challenge.*
    val timeControl = makeTimeControl(config.clock, config.days)
    env.user.perfsRepo
      .withPerf(orig -> dest, config.perfType, _.sec)
      .map: (orig, dest) =>
        lila.challenge.Challenge.make(
          variant = config.variant,
          initialFen = config.position,
          timeControl = timeControl,
          rated = config.rated,
          color = config.color.name,
          challenger = ChallengeModel.toRegistered(orig),
          destUser = dest.some,
          rematchOf = none,
          rules = config.rules
        )

  def openCreate = AnonOrScopedBody(parse.anyContent)(_.Challenge.Write, _.Web.Mobile): ctx ?=>
    bindForm(
      env.setup.forms.api.open(isAdmin = isGrantedOpt(_.ApiChallengeAdmin) || ctx.me.exists(_.isVerified))
    )(
      jsonFormError,
      config =>
        limit
          .challenge(req.ipAddress, rateLimited):
            env.challenge.api
              .createOpen(config)
              .map: challenge =>
                JsonOk:
                  val url = s"${env.net.baseUrl}/${challenge.id}"
                  env.challenge.jsonView.apiAndMobile(challenge, none, none) ++ Json.obj(
                    "urlWhite" -> s"$url?color=white",
                    "urlBlack" -> s"$url?color=black"
                  )
          .dmap(_.as(JSON))
    )

  def offerRematchForGame(gameId: GameId) = Auth { _ ?=> me ?=>
    NoBot:
      Found(env.game.gameRepo.game(gameId)): g =>
        g.opponentOf(me).flatMap(_.userId).so(env.user.repo.byId).orNotFound { opponent =>
          env.challenge.granter
            .isDenied(opponent, g.perfKey.some)
            .flatMap:
              case Some(d) => BadRequest(jsonError(lila.challenge.ChallengeDenied.translated(d)))
              case _ =>
                api
                  .offerRematchForGame(g, me)
                  .map:
                    if _ then jsonOkResult
                    else BadRequest(jsonError("Sorry, couldn't create the rematch."))
        }
  }
