package controllers

import lila._
import team.{ Joined, Motivate, Team ⇒ TeamModel, TeamEdit }
import user.{ User ⇒ UserModel }
import views._
import http.Context
import security.Granter

import scalaz.effects._
import play.api.mvc._
import play.api.templates.Html
import play.api.libs.concurrent.Akka

object Team extends LilaController {

  private def teamRepo = env.team.teamRepo
  private def requestRepo = env.team.requestRepo
  private def memberRepo = env.team.memberRepo
  private def forms = env.team.forms
  private def api = env.team.api
  private def paginator = env.team.paginator

  def home(page: Int) = Open { implicit ctx ⇒
    Ok(html.team.home(paginator popularTeams page))
  }

  def show(id: String, page: Int) = Open { implicit ctx ⇒
    Async(Akka.future {
      IOptionIOk(teamRepo byId id) { team ⇒ renderTeam(team, page) }
    })
  }

  def renderTeam(team: TeamModel, page: Int = 1)(implicit ctx: Context) =
    env.team.teamInfo(team, ctx.me) map { info ⇒
      html.team.show(team, paginator.teamMembers(team, page), info)
    }

  def edit(id: String) = Auth { implicit ctx ⇒
    me ⇒ IOptionResult(teamRepo byId id) { team ⇒
      Owner(team) { io(Ok(html.team.edit(team, forms edit team))) }
    }
  }

  def update(id: String) = AuthBody { implicit ctx ⇒
    implicit me ⇒ IOptionResult(teamRepo byId id) { team ⇒
      Owner(team) {
        implicit val req = ctx.body
        forms.edit(team).bindFromRequest.fold(
          err ⇒ io(BadRequest(html.team.edit(team, err))),
          data ⇒ api.update(team, data, me) inject Redirect(routes.Team.show(team.id))
        )
      }
    }
  }

  def kickForm(id: String) = Auth { implicit ctx ⇒
    me ⇒ IOptionResult(teamRepo byId id) { team ⇒
      Owner(team) { 
        memberRepo userIdsByTeamId team.id map { userIds =>
          Ok(html.team.kick(team, userIds filterNot (me.id ==)))
        }
      }
    }
  }

  def kick(id: String) = AuthBody { implicit ctx ⇒
    implicit me ⇒ IOptionResult(teamRepo byId id) { team ⇒
      Owner(team) {
        implicit val req = ctx.body
        forms.kick.bindFromRequest.fold(
          _ ⇒ io(), 
          userId ⇒ api.kick(team, userId)
        ) inject Redirect(routes.Team.show(team.id))
      }
    }
  }

  def form = Auth { implicit ctx ⇒
    me ⇒ OnePerWeek(me) {
      Ok(html.team.form(forms.create, forms.captchaCreate))
    }
  }

  def create = AuthBody { implicit ctx ⇒
    implicit me ⇒ OnePerWeek(me) {
      IOResult {
        implicit val req = ctx.body
        forms.create.bindFromRequest.fold(
          err ⇒ io(BadRequest(html.team.form(err, forms.captchaCreate))),
          data ⇒ api.create(data, me) map { team ⇒ Redirect(routes.Team.show(team.id)) }
        )
      }
    }
  }

  def mine = Auth { implicit ctx ⇒
    me ⇒ IOk(api mine me map { html.team.mine(_) })
  }

  def joinPage(id: String) = Auth { implicit ctx ⇒
    me ⇒ IOptionResult(api.requestable(id, me)) { team ⇒
      team.open.fold(
        Ok(html.team.join(team)),
        Redirect(routes.Team.requestForm(team.id))
      )
    }
  }

  def join(id: String) = Auth { implicit ctx ⇒
    implicit me ⇒ IOResult(api join id map {
      case Some(Joined(team))   ⇒ Redirect(routes.Team.show(team.id))
      case Some(Motivate(team)) ⇒ Redirect(routes.Team.requestForm(team.id))
      case _                    ⇒ notFound
    })
  }

  def requestForm(id: String) = Auth { implicit ctx ⇒
    me ⇒ IOptionOk(api.requestable(id, me)) { team ⇒
      html.team.requestForm(team, forms.request, forms.captchaCreate)
    }
  }

  def requestCreate(id: String) = AuthBody { implicit ctx ⇒
    me ⇒ IOptionIOResult(api.requestable(id, me)) { team ⇒
      implicit val req = ctx.body
      forms.request.bindFromRequest.fold(
        err ⇒ io(BadRequest(html.team.requestForm(team, err, forms.captchaCreate))),
        setup ⇒ api.createRequest(team, setup, me) inject Redirect(routes.Team.show(team.id))
      )
    }
  }

  def requestProcess(requestId: String) = AuthBody { implicit ctx ⇒
    me ⇒ IOptionIORedirect(for {
      requestOption ← requestRepo byId requestId
      teamOption ← ~requestOption.map(req ⇒ teamRepo.owned(req.team, me.id))
    } yield (teamOption |@| requestOption).tupled) {
      case (team, request) ⇒ {
        implicit val req = ctx.body
        forms.processRequest.bindFromRequest.fold(
          _ ⇒ io(),
          res ⇒ api.processRequest(team, request, (res == "accept"))
        ) inject routes.Team.show(team.id)
      }
    }
  }

  def quit(id: String) = Auth { implicit ctx ⇒
    implicit me ⇒ IOResult(api quit id map {
      case Some(team) ⇒ Redirect(routes.Team.show(team.id))
      case _          ⇒ notFound
    })
  }

  private def OnePerWeek[A <: Result](me: UserModel)(a: ⇒ A)(implicit ctx: Context): Result = {
    !Granter.superAdmin(me) &&
      api.hasCreatedRecently(me).unsafePerformIO
  } fold (Forbidden(views.html.team.createLimit()), a)

  private def Owner(team: TeamModel)(a: ⇒ IO[Result])(implicit ctx: Context): Result = {
    ~ctx.me.map(me ⇒ team.isCreator(me.id) || Granter.superAdmin(me))
  }.fold(a, renderTeam(team) map { Forbidden(_) }).unsafePerformIO
}
