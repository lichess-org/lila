package controllers

import play.api.i18n.Lang
import play.api.libs.json.Json
import play.api.mvc.*
import scala.util.chaining.*
import views.*

import lila.api.context.*
import lila.app.{ given, * }
import lila.common.HTTPRequest
import lila.swiss.Swiss.ChatFor
import lila.swiss.{ Swiss as SwissModel, SwissForm }
import lila.user.{ User as UserModel }

final class Swiss(
    env: Env,
    tourC: Tournament,
    apiC: Api
)(using
    mat: akka.stream.Materializer
) extends LilaController(env):

  private def swissNotFound(using WebContext) = NotFound(html.swiss.bits.notFound())

  def home     = Open(serveHome)
  def homeLang = LangPage(routes.Swiss.home)(serveHome)

  private def serveHome(using WebContext) = NoBot {
    ctx.userId.so(env.team.cached.teamIdsList) flatMap
      env.swiss.feature.get map html.swiss.home.apply map { Ok(_) }
  }

  def show(id: SwissId) = Open:
    env.swiss.cache.swissCache.byId(id) flatMap { swissOption =>
      val page = getInt("page").filter(0.<)
      negotiate(
        html = swissOption.fold(swissNotFound.toFuccess): swiss =>
          for
            verdicts <- env.swiss.api.verdicts(swiss, ctx.me)
            version  <- env.swiss.version(swiss.id)
            isInTeam <- ctx.me so isUserInTheTeam(swiss.teamId)
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
                .findMine(swiss.id into ChatId, ctx.me)
                .flatMap: c =>
                  env.user.lightUserApi.preloadMany(c.chat.userIds) inject
                    c.copy(locked = !env.api.chatFreshness.of(swiss)).some
            streamers  <- streamerCache get swiss.id
            isLocalMod <- canChat so ctx.userId so { env.team.cached.isLeader(swiss.teamId, _) }
          yield Ok(html.swiss.show(swiss, verdicts, json, chat, streamers, isLocalMod)),
        api = _ =>
          swissOption.fold(notFoundJson("No such swiss tournament")): swiss =>
            for
              isInTeam      <- ctx.me.so(isUserInTheTeam(swiss.teamId))
              verdicts      <- env.swiss.api.verdicts(swiss, ctx.me)
              socketVersion <- getBool("socketVersion").so(env.swiss version swiss.id dmap some)
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

  private def isUserInTheTeam(teamId: lila.team.TeamId)(user: UserModel) =
    env.team.cached.teamIds(user.id).dmap(_ contains teamId)

  def round(id: SwissId, round: Int) = Open:
    OptionFuResult(env.swiss.cache.swissCache byId id): swiss =>
      (round > 0 && round <= swiss.round.value).option(lila.swiss.SwissRoundNumber(round)) so { r =>
        val page = getInt("page").filter(0.<)
        env.swiss.roundPager(swiss, r, page | 0) map { pager =>
          Ok(html.swiss.show.round(swiss, r, pager))
        }
      }

  private def CheckTeamLeader(teamId: TeamId)(f: => Fu[Result])(using ctx: WebContext): Fu[Result] =
    ctx.userId so { env.team.cached.isLeader(teamId, _) } flatMapz f

  def form(teamId: TeamId) = Auth { ctx ?=> me =>
    NoLameOrBot:
      CheckTeamLeader(teamId):
        Ok(html.swiss.form.create(env.swiss.forms.create(me), teamId))
  }

  def create(teamId: TeamId) = AuthBody { ctx ?=> me =>
    NoLameOrBot:
      CheckTeamLeader(teamId):
        env.swiss.forms
          .create(me)
          .bindFromRequest()
          .fold(
            err => BadRequest(html.swiss.form.create(err, teamId)).toFuccess,
            data =>
              tourC.rateLimitCreation(me, isPrivate = true, ctx.req, Redirect(routes.Team.show(teamId))):
                env.swiss.api.create(data, me, teamId) map { swiss =>
                  Redirect(routes.Swiss.show(swiss.id))
                }
          )
  }

  def apiCreate(teamId: TeamId) = ScopedBody(_.Tournament.Write) { ctx ?=> me =>
    if me.isBot || me.lame then notFoundJson("This account cannot create tournaments")
    else
      env.team.cached.isLeader(teamId, me.id) flatMap {
        if _ then
          env.swiss.forms
            .create(me)
            .bindFromRequest()
            .fold(
              jsonFormError(_)(using reqLang),
              data =>
                tourC.rateLimitCreation(me, isPrivate = true, ctx.req, rateLimited):
                  env.swiss.api.create(data, me, teamId) flatMap env.swiss.json.api map JsonOk
            )
        else notFoundJson("You're not a leader of that team")

      }
  }

  def apiTerminate(id: SwissId) = ScopedBody(_.Tournament.Write) { _ ?=> me =>
    env.swiss.cache.swissCache byId id flatMapz {
      case swiss if swiss.createdBy == me.id || isGranted(_.ManageTournament, me) =>
        env.swiss.api
          .kill(swiss)
          .map(_ => jsonOkResult)
      case _ => BadRequest(jsonError("Can't terminate that tournament: Permission denied"))
    }
  }

  def join(id: SwissId) = AuthBody { ctx ?=> me =>
    NoLameOrBot:
      doJoin(me, id, bodyPassword)
  }

  def apiJoin(id: SwissId) = ScopedBody(_.Tournament.Write) { ctx ?=> me =>
    if me.lame || me.isBot
    then Unauthorized(Json.obj("error" -> "This user cannot join tournaments"))
    else doJoin(me, id, bodyPassword)
  }

  private def bodyPassword(using Request[?]) =
    SwissForm.joinForm.bindFromRequest().fold(_ => none, identity)

  private def doJoin(me: UserModel, id: SwissId, password: Option[String]) =
    env.team.cached.teamIds(me.id) flatMap { teamIds =>
      env.swiss.api.join(id, me, teamIds.contains, password) flatMap { result =>
        if result then jsonOkResult
        else JsonBadRequest(jsonError("Could not join the tournament"))
      }
    }

  def withdraw(id: SwissId) = Auth { ctx ?=> me =>
    env.swiss.api.withdraw(id, me.id) >>
      negotiate(
        html = Redirect(routes.Swiss.show(id)),
        api = _ => jsonOkResult
      )
  }

  def apiWithdraw(id: SwissId) = ScopedBody(_.Tournament.Write) { _ ?=> me =>
    env.swiss.api.withdraw(id, me.id) inject jsonOkResult
  }

  def edit(id: SwissId) = Auth { ctx ?=> me =>
    WithEditableSwiss(id, me): swiss =>
      html.swiss.form.edit(swiss, env.swiss.forms.edit(me, swiss))
  }

  def update(id: SwissId) = AuthBody { ctx ?=> me =>
    WithEditableSwiss(id, me): swiss =>
      env.swiss.forms
        .edit(me, swiss)
        .bindFromRequest()
        .fold(
          err => BadRequest(html.swiss.form.edit(swiss, err)),
          data => env.swiss.api.update(swiss.id, data) inject Redirect(routes.Swiss.show(id))
        )
  }

  def apiUpdate(id: SwissId) = ScopedBody(_.Tournament.Write) { ctx ?=> me =>
    WithEditableSwiss(
      id,
      me,
      _ => Unauthorized(jsonError("This user cannot edit this swiss"))
    ): swiss =>
      env.swiss.forms
        .edit(me, swiss)
        .bindFromRequest()
        .fold(
          newJsonFormError,
          data =>
            env.swiss.api.update(swiss.id, data) flatMapz { swiss =>
              env.swiss.json.api(swiss) map JsonOk
            }
        )
  }

  def scheduleNextRound(id: SwissId) =
    def doSchedule(using BodyContext[?])(me: UserModel) = WithEditableSwiss(id, me): swiss =>
      env.swiss.forms.nextRound
        .bindFromRequest()
        .fold(
          err =>
            render.async:
              case Accepts.Json() => newJsonFormError(err)
              case _              => Redirect(routes.Swiss.show(id))
          ,
          date =>
            env.swiss.api.scheduleNextRound(swiss, date) inject render:
              case Accepts.Json() => NoContent
              case _              => Redirect(routes.Swiss.show(id))
        )
    AuthOrScopedBody(_.Tournament.Write)(doSchedule, doSchedule)

  def terminate(id: SwissId) = Auth { _ ?=> me =>
    WithEditableSwiss(id, me): swiss =>
      env.swiss.api kill swiss inject Redirect(routes.Team.show(swiss.teamId))
  }

  def standing(id: SwissId, page: Int) = Anon:
    WithSwiss(id): swiss =>
      JsonOk:
        env.swiss.standingApi(swiss, page)

  def pageOf(id: SwissId, userId: UserStr) = Anon:
    WithSwiss(id): swiss =>
      env.swiss.api.pageOf(swiss, userId.id) flatMapz { page =>
        JsonOk:
          env.swiss.standingApi(swiss, page)
      }

  def player(id: SwissId, userId: UserStr) = Anon:
    WithSwiss(id): swiss =>
      env.swiss.api.playerInfo(swiss, userId.id) flatMap {
        _.fold(notFoundJson()): player =>
          JsonOk(lila.swiss.SwissJson.playerJsonExt(swiss, player))
      }

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

  private def WithSwiss(id: SwissId)(f: SwissModel => Fu[Result]): Fu[Result] =
    env.swiss.cache.swissCache byId id flatMapz f

  private def WithEditableSwiss(
      id: SwissId,
      me: UserModel,
      fallback: SwissModel => Fu[Result] = swiss => Redirect(routes.Swiss.show(swiss.id))
  )(f: SwissModel => Fu[Result]): Fu[Result] =
    WithSwiss(id): swiss =>
      if swiss.createdBy == me.id && !swiss.isFinished then f(swiss)
      else if isGranted(_.ManageTournament, me) then f(swiss)
      else fallback(swiss)

  private[controllers] def canHaveChat(swiss: SwissModel.RoundInfo)(using ctx: WebContext): Fu[Boolean] =
    (ctx.noKid && ctx.noBot && HTTPRequest.isHuman(ctx.req)).so:
      swiss.chatFor match
        case ChatFor.NONE                  => fuFalse
        case _ if isGranted(_.ChatTimeout) => fuTrue
        case ChatFor.LEADERS               => ctx.userId so { env.team.cached.isLeader(swiss.teamId, _) }
        case ChatFor.MEMBERS               => ctx.userId so { env.team.api.belongsTo(swiss.teamId, _) }
        case _                             => fuTrue

  private val streamerCache =
    env.memo.cacheApi[SwissId, List[UserId]](64, "swiss.streamers") {
      _.refreshAfterWrite(15.seconds)
        .maximumSize(64)
        .buildAsyncFuture { id =>
          env.streamer.liveStreamApi.all.flatMap { streams =>
            env.swiss.api.filterPlaying(id, streams.streams.map(_.streamer.userId))
          }
        }
    }
