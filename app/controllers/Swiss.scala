package controllers

import play.api.i18n.Lang
import play.api.libs.json.Json
import play.api.mvc.*
import scala.util.chaining.*
import views.*

import lila.api.Context
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

  private def swissNotFound(using Context) = NotFound(html.swiss.bits.notFound())

  def home     = Open(serveHome)
  def homeLang = LangPage(routes.Swiss.home)(serveHome)

  private def serveHome(using Context) = NoBot {
    ctx.userId.??(env.team.cached.teamIdsList) flatMap
      env.swiss.feature.get map html.swiss.home.apply map { Ok(_) }
  }

  def show(id: SwissId) = Open:
    env.swiss.cache.swissCache.byId(id) flatMap { swissOption =>
      val page = getInt("page").filter(0.<)
      negotiate(
        html = swissOption.fold(swissNotFound.toFuccess) { swiss =>
          for {
            verdicts <- env.swiss.api.verdicts(swiss, ctx.me)
            version  <- env.swiss.version(swiss.id)
            isInTeam <- ctx.me ?? isUserInTheTeam(swiss.teamId)
            json <- env.swiss.json(
              swiss = swiss,
              me = ctx.me,
              verdicts = verdicts,
              reqPage = page,
              socketVersion = version.some,
              playerInfo = none,
              isInTeam = isInTeam
            )
            canChat <- canHaveChat(swiss)
            chat <-
              canChat ?? env.chat.api.userChat.cached
                .findMine(swiss.id into ChatId, ctx.me)
                .dmap(some)
            _ <- chat ?? { c =>
              env.user.lightUserApi.preloadMany(c.chat.userIds)
            }
            streamers  <- streamerCache get swiss.id
            isLocalMod <- canChat ?? ctx.userId ?? { env.team.cached.isLeader(swiss.teamId, _) }
          } yield Ok(html.swiss.show(swiss, verdicts, json, chat, streamers, isLocalMod))
        },
        api = _ =>
          swissOption.fold(notFoundJson("No such swiss tournament")) { swiss =>
            for {
              isInTeam      <- ctx.me.??(isUserInTheTeam(swiss.teamId))
              verdicts      <- env.swiss.api.verdicts(swiss, ctx.me)
              socketVersion <- getBool("socketVersion", ctx.req).??(env.swiss version swiss.id dmap some)
              playerInfo <- getUserStr("playerInfo", ctx.req).map(_.id).?? {
                env.swiss.api.playerInfo(swiss, _)
              }
              page = getInt("page", ctx.req).filter(0.<)
              json <- env.swiss.json(
                swiss = swiss,
                me = ctx.me,
                verdicts = verdicts,
                reqPage = page,
                socketVersion = socketVersion,
                playerInfo = playerInfo,
                isInTeam = isInTeam
              )
            } yield JsonOk(json)
          }
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
      (round > 0 && round <= swiss.round.value).option(lila.swiss.SwissRoundNumber(round)) ?? { r =>
        val page = getInt("page").filter(0.<)
        env.swiss.roundPager(swiss, r, page | 0) map { pager =>
          Ok(html.swiss.show.round(swiss, r, pager))
        }
      }

  private def CheckTeamLeader(teamId: TeamId)(f: => Fu[Result])(using ctx: Context): Fu[Result] =
    ctx.userId ?? { env.team.cached.isLeader(teamId, _) } flatMapz f

  def form(teamId: TeamId) = Auth { ctx ?=> me =>
    NoLameOrBot {
      CheckTeamLeader(teamId) {
        Ok(html.swiss.form.create(env.swiss.forms.create(me), teamId)).toFuccess
      }
    }
  }

  def create(teamId: TeamId) = AuthBody { ctx ?=> me =>
    NoLameOrBot:
      CheckTeamLeader(teamId):
        env.swiss.forms
          .create(me)
          .bindFromRequest()(ctx.body, formBinding)
          .fold(
            err => BadRequest(html.swiss.form.create(err, teamId)).toFuccess,
            data =>
              tourC.rateLimitCreation(me, isPrivate = true, ctx.req, Redirect(routes.Team.show(teamId))) {
                env.swiss.api.create(data, me, teamId) map { swiss =>
                  Redirect(routes.Swiss.show(swiss.id))
                }
              }
          )
  }

  def apiCreate(teamId: TeamId) = ScopedBody(_.Tournament.Write) { req ?=> me =>
    if me.isBot || me.lame then notFoundJson("This account cannot create tournaments")
    else
      env.team.cached.isLeader(teamId, me.id) flatMap {
        if _ then
          env.swiss.forms
            .create(me)
            .bindFromRequest()
            .fold(
              jsonFormErrorDefaultLang,
              data =>
                tourC.rateLimitCreation(me, isPrivate = true, req, rateLimited):
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
      case _ => BadRequest(jsonError("Can't terminate that tournament: Permission denied")).toFuccess
    }
  }

  def join(id: SwissId) = AuthBody { ctx ?=> me =>
    NoLameOrBot:
      doJoin(me, id, bodyPassword)
  }

  def apiJoin(id: SwissId) = ScopedBody(_.Tournament.Write) { req ?=> me =>
    if me.lame || me.isBot
    then Unauthorized(Json.obj("error" -> "This user cannot join tournaments")).toFuccess
    else doJoin(me, id, bodyPassword)
  }

  private def bodyPassword(using Request[?]) =
    SwissForm.joinForm.bindFromRequest().fold(_ => none, identity)

  private def doJoin(me: UserModel, id: SwissId, password: Option[String]) =
    env.team.cached.teamIds(me.id) flatMap { teamIds =>
      env.swiss.api.join(id, me, teamIds.contains, password) flatMap { result =>
        fuccess {
          if (result) jsonOkResult
          else BadRequest(Json.obj("error" -> "Could not join the tournament"))
        }
      }
    }

  def withdraw(id: SwissId) = Auth { ctx ?=> me =>
    env.swiss.api.withdraw(id, me.id) >>
      negotiate(
        html = Redirect(routes.Swiss.show(id)).toFuccess,
        api = _ => fuccess(jsonOkResult)
      )
  }

  def apiWithdraw(id: SwissId) = ScopedBody(_.Tournament.Write) { _ ?=> me =>
    env.swiss.api.withdraw(id, me.id) inject jsonOkResult
  }

  def edit(id: SwissId) = Auth { ctx ?=> me =>
    WithEditableSwiss(id, me) { swiss =>
      Ok(html.swiss.form.edit(swiss, env.swiss.forms.edit(me, swiss))).toFuccess
    }
  }

  def update(id: SwissId) = AuthBody { ctx ?=> me =>
    WithEditableSwiss(id, me): swiss =>
      env.swiss.forms
        .edit(me, swiss)
        .bindFromRequest()
        .fold(
          err => BadRequest(html.swiss.form.edit(swiss, err)).toFuccess,
          data => env.swiss.api.update(swiss.id, data) inject Redirect(routes.Swiss.show(id))
        )
  }

  def apiUpdate(id: SwissId) = ScopedBody(_.Tournament.Write) { req ?=> me =>
    given play.api.i18n.Lang = reqLang
    WithEditableSwiss(
      id,
      me,
      _ => Unauthorized(Json.obj("error" -> "This user cannot edit this swiss")).toFuccess
    ) { swiss =>
      env.swiss.forms
        .edit(me, swiss)
        .bindFromRequest()
        .fold(
          newJsonFormError,
          data => {
            env.swiss.api.update(swiss.id, data) flatMapz { swiss =>
              env.swiss.json.api(swiss) map JsonOk
            }
          }
        )
    }
  }

  def scheduleNextRound(id: SwissId) =
    def doSchedule(using Request[?])(me: UserModel) = WithEditableSwiss(id, me) { swiss =>
      env.swiss.forms.nextRound
        .bindFromRequest()
        .fold(
          err =>
            render.async:
              case Accepts.Json() => newJsonFormError(err)(using reqLang)
              case _              => Redirect(routes.Swiss.show(id)).toFuccess
          ,
          date =>
            env.swiss.api.scheduleNextRound(swiss, date) inject render:
              case Accepts.Json() => NoContent
              case _              => Redirect(routes.Swiss.show(id))
        )
    }
    AuthOrScopedBody(_.Tournament.Write)(
      auth = doSchedule,
      scoped = doSchedule
    )

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
          JsonOk(fuccess(lila.swiss.SwissJson.playerJsonExt(swiss, player)))
      }

  def exportTrf(id: SwissId) = Anon:
    env.swiss.cache.swissCache byId id map {
      case None => NotFound("Tournament not found")
      case Some(swiss) =>
        Ok.chunked(env.swiss.trf(swiss, sorted = true) intersperse "\n")
          .pipe(asAttachmentStream(env.api.gameApiV2.filename(swiss, "trf")))
    }

  def byTeam(id: TeamId) = Anon:
    apiC.jsonDownload {
      env.swiss.api
        .byTeamCursor(id)
        .documentSource(getInt("max", req) | 100)
        .mapAsync(4)(env.swiss.json.api)
        .throttle(20, 1.second)
    }.toFuccess

  private def WithSwiss(id: SwissId)(f: SwissModel => Fu[Result]): Fu[Result] =
    env.swiss.cache.swissCache byId id flatMapz f

  private def WithEditableSwiss(
      id: SwissId,
      me: UserModel,
      fallback: SwissModel => Fu[Result] = swiss => Redirect(routes.Swiss.show(swiss.id)).toFuccess
  )(
      f: SwissModel => Fu[Result]
  ): Fu[Result] =
    WithSwiss(id) { swiss =>
      if (swiss.createdBy == me.id && !swiss.isFinished) f(swiss)
      else if (isGranted(_.ManageTournament, me)) f(swiss)
      else fallback(swiss)
    }

  private def canHaveChat(swiss: SwissModel)(using Context): Fu[Boolean] =
    canHaveChat(swiss.roundInfo)

  private[controllers] def canHaveChat(swiss: SwissModel.RoundInfo)(using ctx: Context): Fu[Boolean] =
    (ctx.noKid && ctx.noBot && HTTPRequest.isHuman(ctx.req)) ?? {
      swiss.chatFor match
        case ChatFor.NONE                  => fuFalse
        case _ if isGranted(_.ChatTimeout) => fuTrue
        case ChatFor.LEADERS               => ctx.userId ?? { env.team.cached.isLeader(swiss.teamId, _) }
        case ChatFor.MEMBERS               => ctx.userId ?? { env.team.api.belongsTo(swiss.teamId, _) }
        case _                             => fuTrue
    }

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
