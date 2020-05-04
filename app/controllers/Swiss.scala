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
    env.swiss.api.byId(SwissId(id)) flatMap {
      _.fold(swissNotFound.fuccess) { swiss =>
        val page = getInt("page") | 1
        for {
          version  <- env.swiss.version(swiss.id)
          isInTeam <- ctx.userId.??(u => env.team.cached.teamIds(u).dmap(_ contains swiss.teamId))
          json <- env.swiss.json(
            swiss = swiss,
            me = ctx.me,
            page = page,
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
      }
    }
  }

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

  private def canHaveChat(swiss: SwissModel)(implicit ctx: Context): Fu[Boolean] =
    (swiss.hasChat && ctx.noKid) ?? ctx.userId.?? {
      env.team.api.belongsTo(swiss.teamId, _)
    }
}
