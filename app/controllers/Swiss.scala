package controllers

import play.api.mvc._
import play.api.libs.json.Json

import lila.api.Context
import lila.app._
import lila.swiss.{ Swiss => SwissModel }
import lila.swiss.Swiss.{ Id => SwissId }
import views._

final class Swiss(
    env: Env,
    tourC: Tournament
) extends LilaController(env) {

  private def swissNotFound(implicit ctx: Context) = NotFound(html.swiss.bits.notFound())

  def show(id: String) = Open { implicit ctx =>
    env.swiss.api.byId(SwissId(id)) flatMap { swissOption =>
      val page = getInt("page")
      negotiate(
        html = swissOption.fold(swissNotFound.fuccess) { swiss =>
          for {
            version  <- env.swiss.version(swiss.id)
            isInTeam <- isCtxInTheTeam(swiss.teamId)
            json <- env.swiss.json(
              swiss = swiss,
              me = ctx.me,
              reqPage = page,
              socketVersion = version.some,
              isInTeam = isInTeam
            )
            canChat <- canHaveChat(swiss)
            chat <- canChat ?? env.chat.api.userChat.cached
              .findMine(lila.chat.Chat.Id(swiss.id.value), ctx.me)
              .dmap(some)
            _ <- chat ?? { c =>
              env.user.lightUserApi.preloadMany(c.chat.userIds)
            }
          } yield Ok(html.swiss.show(swiss, json, chat))
        },
        api = _ =>
          swissOption.fold(notFoundJson("No such swiss tournament")) { swiss =>
            for {
              socketVersion <- getBool("socketVersion").??(env.swiss version swiss.id dmap some)
              isInTeam      <- isCtxInTheTeam(swiss.teamId)
              json <- env.swiss.json(
                swiss = swiss,
                me = ctx.me,
                reqPage = page,
                socketVersion = socketVersion,
                isInTeam = isInTeam
              )
            } yield Ok(json)
          }
      )
    }
  }

  private def isCtxInTheTeam(teamId: lila.team.Team.ID)(implicit ctx: Context) =
    ctx.userId.??(u => env.team.cached.teamIds(u).dmap(_ contains teamId))

  def form(teamId: String) = Auth { implicit ctx => me =>
    Ok(html.swiss.form.create(env.swiss.forms.create, teamId)).fuccess
  }

  def create(teamId: String) = AuthBody { implicit ctx => me =>
    env.team.teamRepo.isLeader(teamId, me.id) flatMap {
      case false => notFound
      case _ =>
        env.swiss.forms.create
          .bindFromRequest()(ctx.body)
          .fold(
            err => BadRequest(html.swiss.form.create(err, teamId)).fuccess,
            data =>
              tourC.rateLimitCreation(me, false, ctx.req) {
                env.swiss.api.create(data, me, teamId) map { swiss =>
                  Redirect(routes.Swiss.show(swiss.id.value))
                }
              }
          )
    }
  }

  def join(id: String) = AuthBody { implicit ctx => me =>
    NoLameOrBot {
      env.team.cached.teamIds(me.id) flatMap { teamIds =>
        env.swiss.api.join(SwissId(id), me, teamIds.contains) flatMap { result =>
          negotiate(
            html = Redirect(routes.Swiss.show(id)).fuccess,
            api = _ =>
              fuccess {
                if (result) jsonOkResult
                else BadRequest(Json.obj("joined" -> false))
              }
          )
        }
      }
    }
  }

  def edit(id: String) = Auth { implicit ctx => me =>
    WithEditableSwiss(id, me) { swiss =>
      Ok(html.swiss.form.edit(swiss, env.swiss.forms.edit(swiss))).fuccess
    }
  }

  def update(id: String) = AuthBody { implicit ctx => me =>
    WithEditableSwiss(id, me) { swiss =>
      implicit val req = ctx.body
      env.swiss.forms
        .edit(swiss)
        .bindFromRequest
        .fold(
          err => BadRequest(html.swiss.form.edit(swiss, err)).fuccess,
          data => env.swiss.api.update(swiss, data) inject Redirect(routes.Swiss.show(id)).flashSuccess
        )
    }

  }
  def terminate(id: String) = Auth { implicit ctx => me =>
    WithEditableSwiss(id, me) { swiss =>
      env.swiss.api kill swiss inject Redirect(routes.Team.show(swiss.teamId))
    }
  }

  private def WithEditableSwiss(id: String, me: lila.user.User)(
      f: SwissModel => Fu[Result]
  )(implicit ctx: Context): Fu[Result] =
    env.swiss.api byId SwissId(id) flatMap {
      case Some(t) if (t.createdBy == me.id && !t.isFinished) || isGranted(_.ManageTournament) =>
        f(t)
      case Some(t) => Redirect(routes.Swiss.show(t.id.value)).fuccess
      case _       => notFound
    }

  private def canHaveChat(swiss: SwissModel)(implicit ctx: Context): Fu[Boolean] =
    (swiss.hasChat && ctx.noKid) ?? ctx.userId.?? {
      env.team.api.belongsTo(swiss.teamId, _)
    }
}
