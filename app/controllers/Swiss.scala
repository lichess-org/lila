package controllers

import play.api.i18n.Lang
import play.api.libs.json.Json
import play.api.mvc.*
import scala.util.chaining.*
import views.*

import lila.app.{ given, * }
import lila.common.HTTPRequest
import lila.swiss.Swiss.ChatFor
import lila.swiss.{ Swiss as SwissModel, SwissForm }

final class Swiss(
    env: Env,
    tourC: Tournament,
    apiC: Api
)(using akka.stream.Materializer)
    extends LilaController(env):

  private def swissNotFound(using Context) = NotFound.page(html.swiss.bits.notFound())

  def home     = Open(serveHome)
  def homeLang = LangPage(routes.Swiss.home)(serveHome)

  private def serveHome(using Context) = NoBot:
    for
      teamIds <- ctx.userId.so(env.team.cached.teamIdsList)
      swiss   <- env.swiss.feature.get(teamIds)
      page    <- renderPage(html.swiss.home(swiss))
    yield Ok(page)

  def show(id: SwissId) = Open:
    env.swiss.cache.swissCache.byId(id) flatMap { swissOption =>
      val page = getInt("page").filter(0.<)
      negotiate(
        html = swissOption.fold(swissNotFound): swiss =>
          for
            verdicts <- env.swiss.api.verdicts(swiss)
            version  <- env.swiss.version(swiss.id)
            isInTeam <- ctx.me.so(isUserInTheTeam(swiss.teamId)(_))
            json <- env.swiss.json(
              swiss = swiss,
              me = ctx.me,
              verdicts = verdicts,
              reqPage = page,
              socketVersion = version.some,
              playerInfo = none,
              isInTeam = isInTeam
            )
            canChat <- canHaveChat(swiss.roundInfo)
            chat <-
              canChat so env.chat.api.userChat.cached
                .findMine(swiss.id into ChatId)
                .flatMap: c =>
                  env.user.lightUserApi.preloadMany(c.chat.userIds) inject
                    c.copy(locked = !env.api.chatFreshness.of(swiss)).some
            streamers  <- streamerCache get swiss.id
            isLocalMod <- ctx.me.so { env.team.api.hasPerm(swiss.teamId, _, _.Comm) }
            page       <- renderPage(html.swiss.show(swiss, verdicts, json, chat, streamers, isLocalMod))
          yield Ok(page),
        json = swissOption.fold[Fu[Result]](notFoundJson("No such Swiss tournament")): swiss =>
          for
            isInTeam      <- ctx.me.so(isUserInTheTeam(swiss.teamId)(_))
            verdicts      <- env.swiss.api.verdicts(swiss)
            socketVersion <- getBool("socketVersion").soFu(env.swiss version swiss.id)
            playerInfo <- getUserStr("playerInfo").so: u =>
              env.swiss.api.playerInfo(swiss, u.id)
            page = getInt("page").filter(0.<)
            json <- env.swiss.json(
              swiss = swiss,
              me = ctx.me,
              verdicts = verdicts,
              reqPage = page,
              socketVersion = socketVersion,
              playerInfo = playerInfo,
              isInTeam = isInTeam
            )
          yield JsonOk(json)
      )
    }

  def apiShow(id: SwissId) = Anon:
    env.swiss.cache.swissCache byId id flatMap {
      case Some(swiss) => env.swiss.json.api(swiss) map JsonOk
      case _           => notFoundJson()
    }

  private def isUserInTheTeam(teamId: lila.team.TeamId)(user: UserId) =
    env.team.cached.teamIds(user).dmap(_ contains teamId)

  def round(id: SwissId, round: Int) = Open:
    Found(env.swiss.cache.swissCache byId id): swiss =>
      (round > 0 && round <= swiss.round.value).option(lila.swiss.SwissRoundNumber(round)) so { r =>
        val page = getInt("page").filter(0.<)
        env.swiss.roundPager(swiss, r, page | 0) flatMap { pager =>
          Ok.page(html.swiss.show.round(swiss, r, pager))
        }
      }

  private def CheckTeamLeader(teamId: TeamId)(f: => Fu[Result])(using ctx: Context): Fu[Result] =
    ctx.me so { env.team.api.isGranted(teamId, _, _.Tour) } elseNotFound f

  def form(teamId: TeamId) = Auth { ctx ?=> me ?=>
    NoLameOrBot:
      CheckTeamLeader(teamId):
        Ok.page(html.swiss.form.create(env.swiss.forms.create(me), teamId))
  }

  def create(teamId: TeamId) = AuthBody { ctx ?=> me ?=>
    NoLameOrBot:
      CheckTeamLeader(teamId):
        env.swiss.forms
          .create(me)
          .bindFromRequest()
          .fold(
            err => BadRequest.page(html.swiss.form.create(err, teamId)),
            data =>
              tourC.rateLimitCreation(isPrivate = true, Redirect(routes.Team.show(teamId))):
                env.swiss.api.create(data, teamId) map { swiss =>
                  Redirect(routes.Swiss.show(swiss.id))
                }
          )
  }

  def apiCreate(teamId: TeamId) = ScopedBody(_.Tournament.Write) { ctx ?=> me ?=>
    if me.isBot || me.lame then notFoundJson("This account cannot create tournaments")
    else
      env.team.api.isGranted(teamId, me, _.Tour) flatMap {
        if _ then
          env.swiss.forms
            .create(me)
            .bindFromRequest()
            .fold(
              doubleJsonFormError,
              data =>
                tourC.rateLimitCreation(isPrivate = true, rateLimited):
                  env.swiss.api.create(data, teamId) flatMap env.swiss.json.api map JsonOk
            )
        else notFoundJson("You're not a leader of that team")

      }
  }

  def apiTerminate(id: SwissId) = ScopedBody(_.Tournament.Write) { _ ?=> me ?=>
    Found(env.swiss.cache.swissCache byId id):
      case swiss if swiss.createdBy.is(me) || isGranted(_.ManageTournament) =>
        env.swiss.api
          .kill(swiss)
          .map(_ => jsonOkResult)
      case _ => BadRequest(jsonError("Can't terminate that tournament: Permission denied"))
  }

  def join(id: SwissId) = AuthOrScopedBody(_.Tournament.Write) { _ ?=> _ ?=>
    NoLameOrBot:
      doJoin(id, bodyPassword)
  }

  private def bodyPassword(using Request[?]) =
    SwissForm.joinForm.bindFromRequest().fold(_ => none, identity)

  private def doJoin(id: SwissId, password: Option[String])(using me: Me) =
    env.team.cached.teamIds(me) flatMap { teamIds =>
      env.swiss.api.join(id, teamIds.contains, password) flatMap { result =>
        if result then jsonOkResult
        else JsonBadRequest(jsonError("Could not join the tournament"))
      }
    }

  def withdraw(id: SwissId) = AuthOrScoped(_.Tournament.Write) { ctx ?=> me ?=>
    env.swiss.api.withdraw(id, me) >>
      negotiate(
        Redirect(routes.Swiss.show(id)),
        jsonOkResult
      )
  }

  def edit(id: SwissId) = Auth { ctx ?=> me ?=>
    WithEditableSwiss(id): swiss =>
      Ok.page(html.swiss.form.edit(swiss, env.swiss.forms.edit(me, swiss)))
  }

  def update(id: SwissId) = AuthBody { ctx ?=> me ?=>
    WithEditableSwiss(id): swiss =>
      env.swiss.forms
        .edit(me, swiss)
        .bindFromRequest()
        .fold(
          err => BadRequest.page(html.swiss.form.edit(swiss, err)),
          data => env.swiss.api.update(swiss.id, data) >> Redirect(routes.Swiss.show(id))
        )
  }

  def apiUpdate(id: SwissId) = ScopedBody(_.Tournament.Write) { req ?=> me ?=>
    WithEditableSwiss(id): swiss =>
      env.swiss.forms
        .edit(me, swiss)
        .bindFromRequest()
        .fold(
          err => jsonFormError(err),
          data =>
            env.swiss.api.update(swiss.id, data) >>
              FoundOk(env.swiss.api.update(swiss.id, data))(env.swiss.json.api)
        )
  }

  def scheduleNextRound(id: SwissId) =
    AuthOrScopedBody(_.Tournament.Write) { ctx ?=> me ?=>
      WithEditableSwiss(id): swiss =>
        env.swiss.forms.nextRound
          .bindFromRequest()
          .fold(
            err => negotiate(Redirect(routes.Swiss.show(id)), jsonFormError(err)),
            date =>
              env.swiss.api.scheduleNextRound(swiss, date) >>
                negotiate(Redirect(routes.Swiss.show(id)), NoContent)
          )
    }

  def terminate(id: SwissId) = Auth { _ ?=> me ?=>
    WithEditableSwiss(id): swiss =>
      env.swiss.api kill swiss inject Redirect(routes.Team.show(swiss.teamId))
  }

  def standing(id: SwissId, page: Int) = Anon:
    WithSwiss(id): swiss =>
      JsonOk:
        env.swiss.standingApi(swiss, page)

  def pageOf(id: SwissId, userId: UserStr) = Anon:
    WithSwiss(id): swiss =>
      Found(env.swiss.api.pageOf(swiss, userId.id)): page =>
        JsonOk:
          env.swiss.standingApi(swiss, page)

  def player(id: SwissId, userId: UserStr) = Anon:
    WithSwiss(id): swiss =>
      Found(env.swiss.api.playerInfo(swiss, userId.id)): player =>
        JsonOk(lila.swiss.SwissJson.playerJsonExt(swiss, player))

  def exportTrf(id: SwissId) = Anon:
    env.swiss.cache.swissCache byId id map {
      case None => NotFound("Tournament not found")
      case Some(swiss) =>
        Ok.chunked(env.swiss.trf(swiss, sorted = true) intersperse "\n")
          .pipe(asAttachmentStream(env.api.gameApiV2.filename(swiss, "trf")))
    }

  def byTeam(id: TeamId) = Anon:
    apiC.jsonDownload:
      env.swiss.api
        .byTeamCursor(id)
        .documentSource(getInt("max") | 100)
        .mapAsync(4)(env.swiss.json.api)
        .throttle(20, 1.second)

  private def WithSwiss(id: SwissId)(f: SwissModel => Fu[Result])(using Context): Fu[Result] =
    env.swiss.cache.swissCache byId id orNotFound f

  private def WithEditableSwiss(
      id: SwissId,
      fallback: SwissModel => Context ?=> Fu[Result] = swiss =>
        negotiate(
          Redirect(routes.Swiss.show(swiss.id)),
          Unauthorized(jsonError("This user cannot edit this swiss"))
        )
  )(f: SwissModel => Fu[Result])(using me: Me)(using Context): Fu[Result] =
    WithSwiss(id): swiss =>
      if swiss.createdBy.is(me) && !swiss.isFinished then f(swiss)
      else if isGranted(_.ManageTournament) then f(swiss)
      else fallback(swiss)

  private[controllers] def canHaveChat(swiss: SwissModel.RoundInfo)(using ctx: Context): Fu[Boolean] =
    (ctx.kid.no && ctx.noBot && HTTPRequest.isHuman(ctx.req)).so:
      swiss.chatFor match
        case ChatFor.NONE                     => fuFalse
        case _ if isGrantedOpt(_.ChatTimeout) => fuTrue
        case ChatFor.LEADERS                  => ctx.me so { env.team.api.isLeader(swiss.teamId, _) }
        case ChatFor.MEMBERS                  => ctx.me so { env.team.api.belongsTo(swiss.teamId, _) }
        case _                                => fuTrue

  private val streamerCache =
    env.memo.cacheApi[SwissId, List[UserId]](64, "swiss.streamers"):
      _.refreshAfterWrite(15.seconds)
        .maximumSize(64)
        .buildAsyncFuture: id =>
          env.streamer.liveStreamApi.all.flatMap: streams =>
            env.swiss.api.filterPlaying(id, streams.streams.map(_.streamer.userId))
