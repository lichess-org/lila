package controllers

import play.api.libs.json._
import play.api.mvc._
import play.api.data.Form
import scala.concurrent.duration._

import lila.api.Context
import lila.app._
import lila.common.config.MaxPerSecond
import lila.hub.LightTeam
import lila.security.Granter
import lila.team.{ Joined, Motivate, Team => TeamModel }
import lila.user.{ User => UserModel }
import views._

final class Team(
    env: Env,
    apiC: => Api
) extends LilaController(env) {

  private def forms     = env.team.forms
  private def api       = env.team.api
  private def paginator = env.team.paginator

  def all(page: Int) = Open { implicit ctx =>
    paginator popularTeams page map { html.team.list.all(_) }
  }

  def home(page: Int) = Open { implicit ctx =>
    ctx.me.??(api.hasTeams) map {
      case true  => Redirect(routes.Team.mine)
      case false => Redirect(routes.Team.all(page))
    }
  }

  def show(id: String, page: Int) = Open { implicit ctx =>
    OptionFuOk(api team id) { renderTeam(_, page) }
  }

  def search(text: String, page: Int) = OpenBody { implicit ctx =>
    if (text.trim.isEmpty) paginator popularTeams page map { html.team.list.all(_) } else
      env.teamSearch(text, page) map { html.team.list.search(text, _) }
  }

  private def renderTeam(team: TeamModel, page: Int = 1)(implicit ctx: Context) =
    for {
      info    <- env.teamInfo(team, ctx.me)
      members <- paginator.teamMembers(team, page)
      _       <- env.user.lightUserApi preloadMany info.userIds
    } yield html.team.show(team, members, info)

  def legacyUsers(teamId: String) = Action {
    MovedPermanently(routes.Team.users(teamId).url)
  }

  def users(teamId: String) = Action.async { implicit req =>
    api.team(teamId) flatMap {
      _ ?? { team =>
        apiC.jsonStream {
          env.team
            .memberStream(team, MaxPerSecond(20))
            .map(env.api.userApi.one)
        }.fuccess
      }
    }
  }

  def edit(id: String) = Auth { implicit ctx => _ =>
    WithOwnedTeam(id) { team =>
      fuccess(html.team.form.edit(team, forms edit team))
    }
  }

  def update(id: String) = AuthBody { implicit ctx => me =>
    WithOwnedTeam(id) { team =>
      implicit val req = ctx.body
      forms
        .edit(team)
        .bindFromRequest
        .fold(
          err => BadRequest(html.team.form.edit(team, err)).fuccess,
          data => api.update(team, data, me) inject Redirect(routes.Team.show(team.id)).flashSuccess
        )
    }
  }

  def kickForm(id: String) = Auth { implicit ctx => me =>
    WithOwnedTeam(id) { team =>
      env.team.memberRepo userIdsByTeam team.id map { userIds =>
        html.team.admin.kick(team, userIds - me.id)
      }
    }
  }

  def kick(id: String) = AuthBody { implicit ctx => me =>
    WithOwnedTeam(id) { team =>
      implicit val req = ctx.body
      forms.selectMember.bindFromRequest.value ?? { api.kick(team, _, me) } inject Redirect(
        routes.Team.show(team.id)
      ).flashSuccess
    }
  }
  def kickUser(teamId: String, userId: String) = Scoped(_.Team.Write) { _ => me =>
    api team teamId flatMap {
      _ ?? { team =>
        if (team isCreator me.id) api.kick(team, userId, me) inject jsonOkResult
        else Forbidden(jsonError("Not your team")).fuccess
      }
    }
  }

  def changeOwnerForm(id: String) = Auth { implicit ctx => _ =>
    WithOwnedTeam(id) { team =>
      env.team.memberRepo userIdsByTeam team.id map { userIds =>
        html.team.admin.changeOwner(team, userIds - team.createdBy)
      }
    }
  }

  def changeOwner(id: String) = AuthBody { implicit ctx => me =>
    WithOwnedTeam(id) { team =>
      implicit val req = ctx.body
      forms.selectMember.bindFromRequest.value ?? { api.changeOwner(team, _, me) } inject Redirect(
        routes.Team.show(team.id)
      ).flashSuccess
    }
  }

  def close(id: String) = Secure(_.ManageTeam) { implicit ctx => me =>
    OptionFuResult(api team id) { team =>
      (api delete team) >>
        env.mod.logApi.deleteTeam(me.id, team.name, team.description) inject
        Redirect(routes.Team all 1).flashSuccess
    }
  }

  def form = Auth { implicit ctx => me =>
    OnePerWeek(me) {
      forms.anyCaptcha map { captcha =>
        Ok(html.team.form.create(forms.create, captcha))
      }
    }
  }

  def create = AuthBody { implicit ctx => implicit me =>
    OnePerWeek(me) {
      implicit val req = ctx.body
      forms.create.bindFromRequest.fold(
        err =>
          forms.anyCaptcha map { captcha =>
            BadRequest(html.team.form.create(err, captcha))
          },
        data =>
          api.create(data, me) map { team =>
            Redirect(routes.Team.show(team.id)).flashSuccess
          }
      )
    }
  }

  def mine = Auth { implicit ctx => me =>
    api mine me map { html.team.list.mine(_) }
  }

  def join(id: String) = AuthOrScoped(_.Team.Write)(
    auth = ctx =>
      me =>
        api.join(id, me) flatMap {
          case Some(Joined(team))   => Redirect(routes.Team.show(team.id)).flashSuccess.fuccess
          case Some(Motivate(team)) => Redirect(routes.Team.requestForm(team.id)).flashSuccess.fuccess
          case _                    => notFound(ctx)
        },
    scoped = req =>
      me =>
        env.oAuth.server.fetchAppAuthor(req) flatMap {
          _ ?? { api.joinApi(id, me, _) }
        } map {
          case Some(Joined(_)) => jsonOkResult
          case Some(Motivate(_)) =>
            Forbidden(jsonError("This team requires confirmation, and is not owned by the oAuth app owner."))
          case _ => NotFound(jsonError("Team not found"))
        }
  )

  def requests = Auth { implicit ctx => me =>
    import lila.memo.CacheApi._
    env.team.cached.nbRequests invalidate me.id
    api requestsWithUsers me map { html.team.request.all(_) }
  }

  def requestForm(id: String) = Auth { implicit ctx => me =>
    OptionFuOk(api.requestable(id, me)) { team =>
      forms.anyCaptcha map { html.team.request.requestForm(team, forms.request, _) }
    }
  }

  def requestCreate(id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(api.requestable(id, me)) { team =>
      implicit val req = ctx.body
      forms.request.bindFromRequest.fold(
        err =>
          forms.anyCaptcha map { captcha =>
            BadRequest(html.team.request.requestForm(team, err, captcha))
          },
        setup => api.createRequest(team, setup, me) inject Redirect(routes.Team.show(team.id)).flashSuccess
      )
    }
  }

  def requestProcess(requestId: String) = AuthBody { implicit ctx => me =>
    OptionFuRedirectUrl(for {
      requestOption <- api request requestId
      teamOption    <- requestOption.??(req => env.team.teamRepo.owned(req.team, me.id))
    } yield (teamOption |@| requestOption).tupled) {
      case (team, request) =>
        implicit val req = ctx.body
        forms.processRequest.bindFromRequest.fold(
          _ => fuccess(routes.Team.show(team.id).toString), {
            case (decision, url) =>
              api.processRequest(team, request, (decision === "accept")) inject url
          }
        )
    }
  }

  def quit(id: String) = AuthOrScoped(_.Team.Write)(
    auth = ctx =>
      me =>
        OptionResult(api.quit(id, me)) { team =>
          Redirect(routes.Team.show(team.id)).flashSuccess
        }(ctx),
    scoped = _ =>
      me =>
        api.quit(id, me) flatMap {
          _.fold(notFoundJson())(_ => jsonOkResult.fuccess)
        }
  )

  def autocomplete = Action.async { req =>
    get("term", req).filter(_.nonEmpty) match {
      case None => BadRequest("No search term provided").fuccess
      case Some(term) =>
        for {
          teams <- api.autocomplete(term, 10)
          _     <- env.user.lightUserApi preloadMany teams.map(_.createdBy)
        } yield Ok {
          JsArray(teams map { team =>
            Json.obj(
              "id"      -> team.id,
              "name"    -> team.name,
              "owner"   -> env.user.lightUserApi.sync(team.createdBy).fold(team.createdBy)(_.name),
              "members" -> team.nbMembers
            )
          })
        } as JSON
    }
  }

  def pmAll(id: String) = Auth { implicit ctx => _ =>
    WithOwnedTeam(id) { team =>
      env.tournament.tournamentRepo.byTeamUpcoming(team.id, 3) map { tours =>
        Ok(html.team.admin.pmAll(team, forms.pmAll, tours))
      }
    }
  }

  def pmAllSubmit(id: String) = AuthOrScopedBody(_.Team.Write)(
    auth = implicit ctx =>
      me =>
        WithOwnedTeam(id) { team =>
          doPmAll(team, me)(ctx.body).fold(
            err =>
              env.tournament.tournamentRepo.byTeamUpcoming(team.id, 3) map { tours =>
                BadRequest(html.team.admin.pmAll(team, err, tours))
              },
            done => done inject Redirect(routes.Team.show(team.id)).flashSuccess
          )
        },
    scoped = implicit req =>
      me =>
        api team id flatMap {
          _.filter(_ isCreator me.id) ?? { team =>
            doPmAll(team, me).fold(
              err => BadRequest(errorsAsJson(err)(reqLang)).fuccess,
              done => done inject jsonOkResult
            )
          }
        }
  )

  private def doPmAll(team: TeamModel, me: UserModel)(implicit req: Request[_]): Either[Form[_], Funit] =
    forms.pmAll.bindFromRequest
      .fold(
        err => Left(err),
        msg =>
          Right {
            PmAllLimitPerUser(me.id) {
              val full = s"""$msg
---
You received this message because you are part of the team lichess.org${routes.Team.show(team.id)}."""
              env.team.memberRepo.userIdsByTeam(team.id) flatMap {
                env.msg.api.multiPost(me, _, full)
              }
            }
          }
      )

  private val PmAllLimitPerUser = new lila.memo.RateLimit[lila.user.User.ID](
    credits = 6,
    duration = 24 hours,
    name = "team pm all per user",
    key = "team.pmAll"
  )

  private def OnePerWeek[A <: Result](me: UserModel)(a: => Fu[A])(implicit ctx: Context): Fu[Result] =
    api.hasCreatedRecently(me) flatMap { did =>
      if (did && !Granter(_.ManageTeam)(me)) Forbidden(views.html.site.message.teamCreateLimit).fuccess
      else a
    }

  private def WithOwnedTeam(teamId: String)(f: TeamModel => Fu[Result])(implicit ctx: Context): Fu[Result] =
    OptionFuResult(api team teamId) { team =>
      if (ctx.userId.exists(team.isCreator) || isGranted(_.ManageTeam)) f(team)
      else renderTeam(team) map { Forbidden(_) }
    }

  private[controllers] def teamsIBelongTo(me: lila.user.User): Fu[List[LightTeam]] =
    api mine me map { _.map(_.light) }
}
