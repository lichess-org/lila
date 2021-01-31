package controllers

import play.api.libs.json.Json
import play.api.mvc._
import scala.concurrent.duration._
import views._

import lila.api.Context
import lila.app._
import lila.swiss.Swiss.{ Id => SwissId, ChatFor }
import lila.swiss.{ Swiss => SwissModel }

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
              isInTeam <- isCtxInTheTeam(swiss.teamId)
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
                socketVersion <- getBool("socketVersion").??(env.swiss version swiss.id dmap some)
                isInTeam      <- isCtxInTheTeam(swiss.teamId)
                playerInfo    <- get("playerInfo").?? { env.swiss.api.playerInfo(swiss, _) }
                verdicts      <- env.swiss.api.verdicts(swiss, ctx.me)
                json <- env.swiss.json(
                  swiss = swiss,
                  me = ctx.me,
                  verdicts = verdicts,
                  reqPage = page,
                  socketVersion = socketVersion,
                  playerInfo = playerInfo,
                  isInTeam = isInTeam
                )
              } yield Ok(json)
            }
        )
      }
    }

  private def isCtxInTheTeam(teamId: lila.team.Team.ID)(implicit ctx: Context) =
    ctx.userId.??(u => env.team.cached.teamIds(u).dmap(_ contains teamId))

  def form(teamId: String) =
    Open { implicit ctx =>
      Ok(html.swiss.form.create(env.swiss.forms.create, teamId)).fuccess
    }

  def create(teamId: String) =
    AuthBody { implicit ctx => me =>
      env.team.cached.isLeader(teamId, me.id) flatMap {
        case false => notFound
        case _ =>
          env.swiss.forms.create
            .bindFromRequest()(ctx.body)
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

  def apiCreate(teamId: String) =
    ScopedBody(_.Tournament.Write) { implicit req => me =>
      if (me.isBot || me.lame) notFoundJson("This account cannot create tournaments")
      else
        env.team.cached.isLeader(teamId, me.id) flatMap {
          case false => notFoundJson("You're not a leader of that team")
          case _ =>
            env.swiss.forms.create
              .bindFromRequest()
              .fold(
                jsonFormErrorDefaultLang,
                data =>
                  tourC.rateLimitCreation(me, isPrivate = true, req, rateLimited) {
                    JsonOk {
                      env.swiss.api.create(data, me, teamId) map env.swiss.json.api
                    }
                  }
              )
        }
    }

  def join(id: String) =
    AuthBody(parse.json) { implicit ctx => me =>
      NoLameOrBot {
        val password = ctx.body.body.\("password").asOpt[String]
        env.team.cached.teamIds(me.id) flatMap { teamIds =>
          env.swiss.api.join(SwissId(id), me, teamIds.contains, password) flatMap { result =>
            fuccess {
              if (result) jsonOkResult
              else BadRequest(Json.obj("joined" -> false))
            }
          }
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
        Ok(html.swiss.form.edit(swiss, env.swiss.forms.edit(swiss))).fuccess
      }
    }

  def update(id: String) =
    AuthBody { implicit ctx => me =>
      WithEditableSwiss(id, me) { swiss =>
        implicit val req = ctx.body
        env.swiss.forms
          .edit(swiss)
          .bindFromRequest()
          .fold(
            err => BadRequest(html.swiss.form.edit(swiss, err)).fuccess,
            data => env.swiss.api.update(swiss, data) inject Redirect(routes.Swiss.show(id))
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
        env.swiss.api.pageOf(swiss, lila.user.User normalize userId) flatMap {
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
            .withHeaders(CONTENT_DISPOSITION -> s"attachment; filename=lichess_swiss_$id.trf")
      }
    }

  def byTeam(id: String) =
    Action.async { implicit req =>
      apiC.jsonStream {
        env.swiss.api
          .byTeamCursor(id)
          .documentSource(getInt("max", req) | 100)
          .map(env.swiss.json.api)
          .throttle(20, 1.second)
      }.fuccess
    }

  private def WithSwiss(id: String)(f: SwissModel => Fu[Result]): Fu[Result] =
    env.swiss.api.byId(SwissId(id)) flatMap { _ ?? f }

  private def WithEditableSwiss(id: String, me: lila.user.User)(
      f: SwissModel => Fu[Result]
  )(implicit ctx: Context): Fu[Result] =
    WithSwiss(id) { swiss =>
      if (swiss.createdBy == me.id && !swiss.isFinished) f(swiss)
      else if (isGranted(_.ManageTournament)) f(swiss)
      else Redirect(routes.Swiss.show(swiss.id.value)).fuccess
    }

  private def canHaveChat(swiss: SwissModel)(implicit ctx: Context): Fu[Boolean] =
    canHaveChat(swiss.roundInfo)

  private[controllers] def canHaveChat(swiss: SwissModel.RoundInfo)(implicit ctx: Context): Fu[Boolean] =
    ctx.noKid ?? {
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
    env.memo.cacheApi[SwissModel.Id, List[lila.user.User.ID]](64, "swiss.streamers") {
      _.refreshAfterWrite(15.seconds)
        .maximumSize(64)
        .buildAsyncFuture { id =>
          env.streamer.liveStreamApi.all.flatMap { streams =>
            env.swiss.api.filterPlaying(id, streams.streams.map(_.streamer.userId))
          }
        }
    }
}
