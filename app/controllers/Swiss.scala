package controllers

import play.api.i18n.Lang
import play.api.libs.json.Json
import play.api.mvc._
import scala.concurrent.duration._
import scala.util.chaining._
import views._

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.swiss.Swiss.{ ChatFor, Id => SwissId }
import lila.swiss.{ Swiss => SwissModel, SwissForm }
import lila.user.{ User => UserModel }

final class Swiss(
    env: Env,
    tourC: Tournament,
    apiC: Api
)(implicit
    mat: akka.stream.Materializer
) extends LilaController(env) {

  private def swissNotFound(implicit ctx: Context) = NotFound(html.swiss.bits.notFound())

  def home =
    Open { implicit ctx =>
      ctx.userId.??(env.team.cached.teamIdsList) flatMap
        env.swiss.feature.get map html.swiss.home.apply map { Ok(_) }
    }

  def show(id: String) =
    Open { implicit ctx =>
      env.swiss.api.byId(SwissId(id)) flatMap { swissOption =>
        val page = getInt("page").filter(0.<)
        negotiate(
          html = swissOption.fold(swissNotFound.fuccess) { swiss =>
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
                  .findMine(lila.chat.Chat.Id(swiss.id.value), ctx.me)
                  .dmap(some)
              _ <- chat ?? { c =>
                env.user.lightUserApi.preloadMany(c.chat.userIds)
              }
              streamers  <- streamerCache get swiss.id
              isLocalMod <- canChat ?? canModChat(swiss)
            } yield Ok(html.swiss.show(swiss, verdicts, json, chat, streamers, isLocalMod))
          },
          api = _ =>
            swissOption.fold(notFoundJson("No such swiss tournament")) { swiss =>
              for {
                isInTeam      <- ctx.me.??(isUserInTheTeam(swiss.teamId))
                verdicts      <- env.swiss.api.verdicts(swiss, ctx.me)
                socketVersion <- getBool("socketVersion", ctx.req).??(env.swiss version swiss.id dmap some)
                playerInfo    <- get("playerInfo", ctx.req).?? { env.swiss.api.playerInfo(swiss, _) }
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
    }

  def apiShow(id: String) =
    Action.async { implicit req =>
      env.swiss.api byId lila.swiss.Swiss.Id(id) flatMap {
        case Some(swiss) => env.swiss.json.api(swiss) map JsonOk
        case _           => notFoundJson()
      }
    }

  private def isUserInTheTeam(teamId: lila.team.Team.ID)(user: UserModel) =
    env.team.cached.teamIds(user.id).dmap(_ contains teamId)

  def round(id: String, round: Int) =
    Open { implicit ctx =>
      OptionFuResult(env.swiss.api.byId(SwissId(id))) { swiss =>
        (round > 0 && round <= swiss.round.value).option(lila.swiss.SwissRound.Number(round)) ?? { r =>
          val page = getInt("page").filter(0.<)
          env.swiss.roundPager(swiss, r, page | 0) map { pager =>
            Ok(html.swiss.show.round(swiss, r, pager))
          }
        }
      }
    }

  private def CheckTeamLeader(teamId: String)(f: => Fu[Result])(implicit ctx: Context): Fu[Result] =
    ctx.userId ?? { env.team.cached.isLeader(teamId, _) } flatMap { _ ?? f }

  def form(teamId: String) =
    Auth { implicit ctx => me =>
      NoLameOrBot {
        CheckTeamLeader(teamId) {
          Ok(html.swiss.form.create(env.swiss.forms.create(me), teamId)).fuccess
        }
      }
    }

  def create(teamId: String) =
    AuthBody { implicit ctx => me =>
      NoLameOrBot {
        CheckTeamLeader(teamId) {
          env.swiss.forms
            .create(me)
            .bindFromRequest()(ctx.body, formBinding)
            .fold(
              err => BadRequest(html.swiss.form.create(err, teamId)).fuccess,
              data =>
                tourC.rateLimitCreation(me, isPrivate = true, ctx.req, Redirect(routes.Team.show(teamId))) {
                  env.swiss.api.create(data, me, teamId) map { swiss =>
                    Redirect(routes.Swiss.show(swiss.id.value))
                  }
                }
            )
        }
      }
    }

  def apiCreate(teamId: String) =
    ScopedBody(_.Tournament.Write) { implicit req => me =>
      if (me.isBot || me.lame) notFoundJson("This account cannot create tournaments")
      else
        env.team.cached.isLeader(teamId, me.id) flatMap {
          case false => notFoundJson("You're not a leader of that team")
          case _ =>
            env.swiss.forms
              .create(me)
              .bindFromRequest()
              .fold(
                jsonFormErrorDefaultLang,
                data =>
                  tourC.rateLimitCreation(me, isPrivate = true, req, rateLimited) {
                    env.swiss.api.create(data, me, teamId) flatMap env.swiss.json.api map JsonOk
                  }
              )
        }
    }

  def apiTerminate(id: String) =
    ScopedBody(_.Tournament.Write) { implicit req => me =>
      env.swiss.api byId lila.swiss.Swiss.Id(id) flatMap {
        _ ?? {
          case swiss if swiss.createdBy == me.id || isGranted(_.ManageTournament, me) =>
            env.swiss.api
              .kill(swiss)
              .map(_ => jsonOkResult)
          case _ => BadRequest(jsonError("Can't terminate that tournament: Permission denied")).fuccess
        }
      }
    }

  def join(id: String) =
    AuthBody { implicit ctx => me =>
      NoLameOrBot {
        doJoin(me, SwissId(id), bodyPassword(ctx.body))
      }
    }

  def apiJoin(id: String) =
    ScopedBody(_.Tournament.Write) { implicit req => me =>
      if (me.lame || me.isBot) Unauthorized(Json.obj("error" -> "This user cannot join tournaments")).fuccess
      else doJoin(me, SwissId(id), bodyPassword)
    }

  private def bodyPassword(implicit req: Request[_]) =
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

  def withdraw(id: String) =
    Auth { implicit ctx => me =>
      env.swiss.api.withdraw(SwissId(id), me.id) >>
        negotiate(
          html = Redirect(routes.Swiss.show(id)).fuccess,
          api = _ => fuccess(jsonOkResult)
        )
    }

  def edit(id: String) =
    Auth { implicit ctx => me =>
      WithEditableSwiss(id, me) { swiss =>
        Ok(html.swiss.form.edit(swiss, env.swiss.forms.edit(me, swiss))).fuccess
      }
    }

  def update(id: String) =
    AuthBody { implicit ctx => me =>
      WithEditableSwiss(id, me) { swiss =>
        implicit val req = ctx.body
        env.swiss.forms
          .edit(me, swiss)
          .bindFromRequest()
          .fold(
            err => BadRequest(html.swiss.form.edit(swiss, err)).fuccess,
            data => env.swiss.api.update(swiss.id, data) inject Redirect(routes.Swiss.show(id))
          )
      }
    }

  def apiUpdate(id: String) =
    ScopedBody(_.Tournament.Write) { implicit req => me =>
      implicit val lang = reqLang
      WithEditableSwiss(
        id,
        me,
        _ => Unauthorized(Json.obj("error" -> "This user cannot edit this swiss")).fuccess
      ) { swiss =>
        env.swiss.forms
          .edit(me, swiss)
          .bindFromRequest()
          .fold(
            newJsonFormError,
            data => {
              env.swiss.api.update(swiss.id, data) flatMap {
                _ ?? { swiss =>
                  env.swiss.json.api(swiss) map JsonOk
                }
              }
            }
          )
      }
    }

  def scheduleNextRound(id: String) =
    AuthBody { implicit ctx => me =>
      WithEditableSwiss(id, me) { swiss =>
        implicit val req = ctx.body
        env.swiss.forms.nextRound
          .bindFromRequest()
          .fold(
            _ => Redirect(routes.Swiss.show(id)).fuccess,
            date => env.swiss.api.scheduleNextRound(swiss, date) inject Redirect(routes.Swiss.show(id))
          )
      }
    }

  def terminate(id: String) =
    Auth { implicit ctx => me =>
      WithEditableSwiss(id, me) { swiss =>
        env.swiss.api kill swiss inject Redirect(routes.Team.show(swiss.teamId))
      }
    }

  def standing(id: String, page: Int) =
    Action.async {
      WithSwiss(id) { swiss =>
        JsonOk {
          env.swiss.standingApi(swiss, page)
        }
      }
    }

  def pageOf(id: String, userId: String) =
    Action.async {
      WithSwiss(id) { swiss =>
        env.swiss.api.pageOf(swiss, UserModel normalize userId) flatMap {
          _ ?? { page =>
            JsonOk {
              env.swiss.standingApi(swiss, page)
            }
          }
        }
      }
    }

  def player(id: String, userId: String) =
    Action.async {
      WithSwiss(id) { swiss =>
        env.swiss.api.playerInfo(swiss, userId) flatMap {
          _.fold(notFoundJson()) { player =>
            JsonOk(fuccess(lila.swiss.SwissJson.playerJsonExt(swiss, player)))
          }
        }
      }
    }

  def exportTrf(id: String) =
    Action.async {
      env.swiss.api.byId(SwissId(id)) map {
        case None => NotFound("Tournament not found")
        case Some(swiss) =>
          Ok.chunked(env.swiss.trf(swiss, sorted = true) intersperse "\n")
            .pipe(asAttachmentStream(env.api.gameApiV2.filename(swiss, "trf")))
      }
    }

  def byTeam(id: String) =
    Action.async { implicit req =>
      apiC.jsonStream {
        env.swiss.api
          .byTeamCursor(id)
          .documentSource(getInt("max", req) | 100)
          .mapAsync(4)(env.swiss.json.api)
          .throttle(20, 1.second)
      }.fuccess
    }

  private def WithSwiss(id: String)(f: SwissModel => Fu[Result]): Fu[Result] =
    env.swiss.api.byId(SwissId(id)) flatMap { _ ?? f }

  private def WithEditableSwiss(
      id: String,
      me: UserModel,
      fallback: SwissModel => Fu[Result] = swiss => Redirect(routes.Swiss.show(swiss.id.value)).fuccess
  )(
      f: SwissModel => Fu[Result]
  ): Fu[Result] =
    WithSwiss(id) { swiss =>
      if (swiss.createdBy == me.id && !swiss.isFinished) f(swiss)
      else if (isGranted(_.ManageTournament, me)) f(swiss)
      else fallback(swiss)
    }

  private def canHaveChat(swiss: SwissModel)(implicit ctx: Context): Fu[Boolean] =
    canHaveChat(swiss.roundInfo)

  private[controllers] def canHaveChat(swiss: SwissModel.RoundInfo)(implicit ctx: Context): Fu[Boolean] =
    (ctx.noKid && ctx.noBot && HTTPRequest.isHuman(ctx.req)) ?? {
      swiss.chatFor match {
        case ChatFor.NONE                  => fuFalse
        case _ if isGranted(_.ChatTimeout) => fuTrue
        case ChatFor.LEADERS               => ctx.userId ?? { env.team.cached.isLeader(swiss.teamId, _) }
        case ChatFor.MEMBERS               => ctx.userId ?? { env.team.api.belongsTo(swiss.teamId, _) }
        case _                             => fuTrue
      }
    }

  private def canModChat(swiss: SwissModel)(implicit ctx: Context): Fu[Boolean] =
    if (isGranted(_.ChatTimeout)) fuTrue
    else ctx.userId ?? { env.team.cached.isLeader(swiss.teamId, _) }

  private val streamerCache =
    env.memo.cacheApi[SwissModel.Id, List[UserModel.ID]](64, "swiss.streamers") {
      _.refreshAfterWrite(15.seconds)
        .maximumSize(64)
        .buildAsyncFuture { id =>
          env.streamer.liveStreamApi.all.flatMap { streams =>
            env.swiss.api.filterPlaying(id, streams.streams.map(_.streamer.userId))
          }
        }
    }
}
