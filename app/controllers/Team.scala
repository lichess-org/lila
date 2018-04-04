package controllers

import lila.api.Context
import lila.app._
import lila.security.Granter
import lila.team.{ Joined, Motivate, Team => TeamModel, TeamRepo, MemberRepo }
import lila.user.{ User => UserModel }
import views._

import play.api.mvc._

object Team extends LilaController {

  private def forms = Env.team.forms
  private def api = Env.team.api
  private def paginator = Env.team.paginator
  private lazy val teamInfo = Env.current.teamInfo

  def all(page: Int) = Open { implicit ctx =>
    NotForKids {
      paginator popularTeams page map { html.team.all(_) }
    }
  }

  def home(page: Int) = Open { implicit ctx =>
    NotForKids {
      ctx.me.??(api.hasTeams) map {
        case true => Redirect(routes.Team.mine)
        case false => Redirect(routes.Team.all(page))
      }
    }
  }

  def show(id: String, page: Int) = Open { implicit ctx =>
    NotForKids {
      OptionFuOk(api team id) { renderTeam(_, page) }
    }
  }

  def search(text: String, page: Int) = OpenBody { implicit ctx =>
    NotForKids {
      text.trim.isEmpty.fold(
        paginator popularTeams page map { html.team.all(_) },
        Env.teamSearch(text, page) map { html.team.search(text, _) }
      )
    }
  }

  private def renderTeam(team: TeamModel, page: Int = 1)(implicit ctx: Context) = for {
    info <- teamInfo(team, ctx.me)
    members <- paginator.teamMembers(team, page)
    _ <- Env.user.lightUserApi preloadMany info.userIds
  } yield html.team.show(team, members, info)

  def users(teamId: String) = Api.ApiRequest { implicit ctx =>
    import Api.limitedDefault
    Env.team.api.team(teamId) flatMap {
      _ ?? { team =>
        val max = getInt("max") map (_ atLeast 1)
        val cost = 100
        val ip = lila.common.HTTPRequest lastRemoteAddress ctx.req
        Api.UsersRateLimitPerIP(ip, cost = cost) {
          Api.UsersRateLimitGlobal("-", cost = cost, msg = ip.value) {
            lila.mon.api.teamUsers.cost(cost)
            import play.api.libs.iteratee._
            Api.toApiResult {
              Env.team.pager.stream(team, max = max) &>
                Enumeratee.map(Env.api.userApi.one)
            } |> fuccess
          }
        }
      }
    }
  }

  def edit(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api team id) { team =>
      Owner(team) { fuccess(html.team.edit(team, forms edit team)) }
    }
  }

  def update(id: String) = AuthBody { implicit ctx => implicit me =>
    OptionFuResult(api team id) { team =>
      Owner(team) {
        implicit val req = ctx.body
        forms.edit(team).bindFromRequest.fold(
          err => BadRequest(html.team.edit(team, err)).fuccess,
          data => api.update(team, data, me) inject Redirect(routes.Team.show(team.id))
        )
      }
    }
  }

  def kickForm(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api team id) { team =>
      Owner(team) {
        MemberRepo userIdsByTeam team.id map { userIds =>
          html.team.kick(team, userIds.filterNot(me.id ==).toList.sorted)
        }
      }
    }
  }

  def kick(id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(api team id) { team =>
      Owner(team) {
        implicit val req = ctx.body
        forms.kick.bindFromRequest.value ?? { api.kick(team, _) } inject Redirect(routes.Team.show(team.id))
      }
    }
  }

  def close(id: String) = Secure(_.CloseTeam) { implicit ctx => me =>
    OptionFuResult(api team id) { team =>
      (api delete team) >>
        Env.mod.logApi.deleteTeam(me.id, team.name, team.description) inject
        Redirect(routes.Team all 1)
    }
  }

  def form = Auth { implicit ctx => me =>
    NotForKids {
      OnePerWeek(me) {
        forms.anyCaptcha map { captcha =>
          Ok(html.team.form(forms.create, captcha))
        }
      }
    }
  }

  def create = AuthBody { implicit ctx => implicit me =>
    OnePerWeek(me) {
      implicit val req = ctx.body
      forms.create.bindFromRequest.fold(
        err => forms.anyCaptcha map { captcha =>
          BadRequest(html.team.form(err, captcha))
        },
        data => api.create(data, me) ?? {
          _ map { team => Redirect(routes.Team.show(team.id)): Result }
        }
      )
    }
  }

  def mine = Auth { implicit ctx => me =>
    api mine me map { html.team.mine(_) }
  }

  def joinPage(id: String) = Auth { implicit ctx => me =>
    NotForKids {
      OptionResult(api.requestable(id, me)) { team =>
        team.open.fold(
          Ok(html.team.join(team)),
          Redirect(routes.Team.requestForm(team.id))
        )
      }
    }
  }

  def join(id: String) = Auth { implicit ctx => implicit me =>
    api join id flatMap {
      case Some(Joined(team)) => Redirect(routes.Team.show(team.id)).fuccess
      case Some(Motivate(team)) => Redirect(routes.Team.requestForm(team.id)).fuccess
      case _ => notFound
    }
  }

  def requests = Auth { implicit ctx => me =>
    Env.team.cached.nbRequests invalidate me.id
    api requestsWithUsers me map { html.team.allRequests(_) }
  }

  def requestForm(id: String) = Auth { implicit ctx => me =>
    OptionFuOk(api.requestable(id, me)) { team =>
      forms.anyCaptcha map { html.team.requestForm(team, forms.request, _) }
    }
  }

  def requestCreate(id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(api.requestable(id, me)) { team =>
      implicit val req = ctx.body
      forms.request.bindFromRequest.fold(
        err => forms.anyCaptcha map { captcha =>
          BadRequest(html.team.requestForm(team, err, captcha))
        },
        setup => api.createRequest(team, setup, me) inject Redirect(routes.Team.show(team.id))
      )
    }
  }

  def requestProcess(requestId: String) = AuthBody { implicit ctx => me =>
    OptionFuRedirectUrl(for {
      requestOption ← api request requestId
      teamOption ← requestOption.??(req => TeamRepo.owned(req.team, me.id))
    } yield (teamOption |@| requestOption).tupled) {
      case (team, request) => {
        implicit val req = ctx.body
        forms.processRequest.bindFromRequest.fold(
          _ => fuccess(routes.Team.show(team.id).toString), {
            case (decision, url) =>
              api.processRequest(team, request, (decision === "accept")) inject url
          }
        )
      }
    }
  }

  def quit(id: String) = Auth { implicit ctx => implicit me =>
    OptionResult(api quit id) { team =>
      Redirect(routes.Team.show(team.id))
    }
  }

  private def OnePerWeek[A <: Result](me: UserModel)(a: => Fu[A])(implicit ctx: Context): Fu[Result] =
    api.hasCreatedRecently(me) flatMap { did =>
      (did && !Granter.superAdmin(me)) fold (
        Forbidden(views.html.team.createLimit()).fuccess,
        a
      )
    }

  private def Owner(team: TeamModel)(a: => Fu[Result])(implicit ctx: Context): Fu[Result] = {
    ctx.me.??(me => team.isCreator(me.id) || Granter.superAdmin(me))
  }.fold(a, renderTeam(team) map { Forbidden(_) })
}
