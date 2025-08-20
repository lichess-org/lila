package controllers

import chess.format.Fen
import play.api.libs.json.Json
import play.api.mvc.{ EssentialAction, Result }

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.core.socket.Sri
import lila.game.AnonCookie
import lila.setup.Processor.HookResult
import lila.setup.ValidFen

final class Setup(
    env: Env,
    challengeC: => Challenge
) extends LilaController(env)
    with lila.web.TheftPrevention:

  import env.setup.{ forms, processor }

  def ai = OpenBody:
    limit.setupBotAi(ctx.userId | UserId(""), rateLimited, cost = ctx.me.exists(_.isBot).so(1)):
      limit.setupPost(ctx.ip, rateLimited):
        bindForm(forms.ai)(
          doubleJsonFormError,
          config =>
            processor.ai(config).flatMap { pov =>
              negotiateApi(
                html = redirectPov(pov),
                api = _ => env.api.roundApi.player(pov, scalalib.data.Preload.none, none).map(Created(_))
              )
            }
        )

  def friend(userId: Option[UserStr]) =
    OpenBody: ctx ?=>
      limit.setupPost(ctx.ip, rateLimited):
        bindForm(forms.friend)(
          doubleJsonFormError,
          config =>
            for
              origUser <- ctx.user.traverse(env.user.perfsRepo.withPerf(_, config.perfType))
              destUser <- userId.so(env.user.api.enabledWithPerf(_, config.perfType))
              denied <- destUser.so(u => env.challenge.granter.isDenied(u.user, config.perfKey.some))
              result <- denied match
                case Some(denied) =>
                  val message = lila.challenge.ChallengeDenied.translated(denied)
                  negotiate(
                    // 403 tells setupCtrl.ts to close the setup modal
                    forbiddenJson(message), // TODO test
                    JsonBadRequest(message)
                  )
                case None =>
                  import lila.challenge.Challenge.*
                  (origUser, ctx.req.sid)
                    .match
                      case (Some(orig), _) => toRegistered(orig).some
                      case (_, Some(sid)) => Challenger.Anonymous(sid).some
                      case _ if HTTPRequest.isLichobile(ctx.req) => Challenger.Open.some
                      case _ => none
                    .so: challenger =>
                      val timeControl = makeTimeControl(config.makeClock, config.makeDaysPerTurn)
                      val challenge = lila.challenge.Challenge.make(
                        variant = config.variant,
                        initialFen = config.fen,
                        timeControl = timeControl,
                        rated = config.rated,
                        color = config.color.name,
                        challenger = challenger,
                        destUser = destUser,
                        rematchOf = none
                      )
                      env.challenge.api
                        .create(challenge)
                        .flatMap:
                          if _ then
                            negotiate(
                              Redirect(routes.Round.watcher(challenge.gameId, Color.white)),
                              challengeC.showChallenge(challenge, justCreated = true)
                            )
                          else
                            negotiate(
                              Redirect(routes.Lobby.home),
                              JsonBadRequest("Challenge not created")
                            )
            yield result
        )

  private def hookResponse(res: HookResult) = res match
    case HookResult.Created(id) =>
      JsonOk:
        Json.obj(
          "ok" -> true,
          "hook" -> Json.obj("id" -> id)
        )
    case HookResult.Refused => JsonBadRequest(("Game was not created"))

  def hook(sri: Sri) = OpenOrScopedBody(parse.anyContent)(_.Web.Mobile): ctx ?=>
    NoBot:
      NoPlaybanOrCurrent:
        bindForm(forms.hook)(
          doubleJsonFormError,
          userConfig =>
            limit.setupPost(req.ipAddress, rateLimited):
              limit.setupAnonHook(req.ipAddress, rateLimited, cost = ctx.isAnon.so(1)):
                for
                  me <- ctx.user.traverse(env.user.api.withPerfs)
                  given Perf = me.fold(lila.rating.Perf.default)(_.perfs(userConfig.perfType))
                  blocking <- ctx.userId.so(env.relation.api.fetchBlocking)
                  res <- processor.hook(
                    userConfig.withinLimits,
                    sri,
                    req.sid,
                    lila.core.pool.Blocking(blocking)
                  )(using me)
                yield hookResponse(res)
        )

  def like(sri: Sri, gameId: GameId) = Open:
    NoBot:
      limit.setupPost(ctx.ip, rateLimited):
        NoPlaybanOrCurrent:
          Found(env.game.gameRepo.game(gameId)): game =>
            for
              orig <- ctx.user.traverse(env.user.api.withPerfs)
              blocking <- ctx.userId.so(env.relation.api.fetchBlocking)
              hookConfig = lila.setup.HookConfig.default(ctx.isAuth)
              hookConfigWithRating = get("rr")
                .fold(
                  hookConfig.withRatingRange(
                    orig.fold(lila.rating.Perf.default)(_.perfs(game.perfKey)).intRating.some,
                    get("deltaMin"),
                    get("deltaMax")
                  )
                )(hookConfig.withRatingRange)
                .updateFrom(game)
              allBlocking = lila.core.pool.Blocking(blocking ++ game.userIds)
              hookResult <- processor.hook(hookConfigWithRating, sri, ctx.req.sid, allBlocking)(using orig)
            yield hookResponse(hookResult)

  def boardApiHook = WithBoardApiHookAuthor { (author, reqSri) => ctx ?=>
    forms
      .boardApiHook:
        ctx.isMobileOauth || (ctx.isAnon && HTTPRequest.isLichessMobile(ctx.req))
      .bindFromRequest()
      .fold(
        doubleJsonFormError,
        config =>
          for
            me <- ctx.me.so(env.user.api.withPerfs)
            blocking <- ctx.me.so(env.relation.api.fetchBlocking(_))
            sri = orUserSri(author)
            ua = HTTPRequest.userAgent(req).fold("?")(_.value)
            _ = lila.mon.lobby.hook
              .apiCreate(ua = ua.split(' ').take(2).mkString(" "), color = config.color.name)
              .increment()
            forcedColor <- env.lobby.boardApiHookStream.mustPlayAsColor(config.color)
            res <- forcedColor.match
              case Some(forced) => fuccess(JsonBadRequest(s"You must also play some games as $forced"))
              case None =>
                config
                  .hook(reqSri | sri, me, sid = sri.value.some, lila.core.pool.Blocking(blocking))
                  .match
                    case Left(hook) =>
                      limit.setupPost(req.ipAddress, rateLimited):
                        limit
                          .boardApiConcurrency(author.map(_.id))(
                            env.lobby.boardApiHookStream(hook.copy(boardApi = true))
                          )(jsOptToNdJson)
                          .toFuccess
                    case Right(Some(seek)) =>
                      author match
                        case Left(_) => JsonBadRequest("Anonymous users cannot create seeks").toFuccess
                        case Right(me) =>
                          env.setup.processor.createSeekIfAllowed(seek, me.id).map {
                            case HookResult.Refused => JsonBadRequest("Already playing too many games")
                            case HookResult.Created(id) => Ok(Json.obj("id" -> id))
                          }
                    case Right(None) => notFoundJson().toFuccess
          yield res
      )
  }

  def boardApiHookCancel = WithBoardApiHookAuthor { (author, _) => _ ?=>
    env.lobby.boardApiHookStream.cancel(orUserSri(author))
    NoContent
  }

  private def orUserSri(author: Either[Sri, lila.user.User]): Sri =
    author.fold(identity, u => Sri(s"user:${u.id}"))

  private def WithBoardApiHookAuthor(
      f: (Either[Sri, lila.user.User], Option[Sri]) => BodyContext[?] ?=> Fu[Result]
  ): EssentialAction =
    AnonOrScopedBody(parse.anyContent)(_.Board.Play, _.Web.Mobile): ctx ?=>
      NoBot:
        val reqSri = getAs[Sri]("sri")
        ctx.me match
          case Some(u) => f(Right(u), reqSri)
          case None =>
            reqSri match
              case Some(sri) => f(Left(sri), reqSri)
              case None => JsonBadRequest("Authentication required")

  def filterForm = Open:
    Ok.snip(views.setup.filter(forms.filter))

  def validateFen = Open:
    (get("fen").map(Fen.Full.clean): Option[Fen.Full]).flatMap(ValidFen(getBool("strict"))) match
      case None => BadRequest
      case Some(v) => Ok.snip(views.analyse.ui.miniSpan(v.fen.board, v.color))

  def apiAi = ScopedBody(_.Challenge.Write, _.Bot.Play, _.Board.Play, _.Web.Mobile) { ctx ?=> me ?=>
    limit.setupBotAi(me, rateLimited, cost = me.isBot.so(1)):
      limit.setupPost(req.ipAddress, rateLimited):
        bindForm(forms.api.ai)(
          doubleJsonFormError,
          config =>
            processor.apiAi(config).map { pov =>
              val json = env.game.jsonView.apiAiNewGame(pov, config.fen)
              Created(json).as(JSON)
            }
        )
  }

  private[controllers] def redirectPov(pov: Pov)(using ctx: Context) =
    val redir = Redirect(routes.Round.watcher(pov.gameId, Color.white))
    if ctx.isAuth then redir
    else
      redir.withCookies(
        env.security.lilaCookie.cookie(
          AnonCookie.name,
          pov.playerId.value,
          maxAge = AnonCookie.maxAge.some,
          httpOnly = false.some
        )
      )
