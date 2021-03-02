package controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._
import views._

import lila.api.Context
import lila.app._
import lila.common.config.MaxPerSecond
import lila.team.{ Requesting, Team => TeamModel }
import lila.user.{ User => UserModel }

final class Team(
    env: Env,
    apiC: => Api
) extends LilaController(env) {

  private def forms     = env.team.forms
  private def api       = env.team.api
  private def paginator = env.team.paginator

  def all(page: Int) =
    Open { implicit ctx =>
      Reasonable(page) {
        paginator popularTeams page map {
          html.team.list.all(_)
        }
      }
    }

  def home(page: Int) =
    Open { implicit ctx =>
      ctx.me.??(api.hasTeams) map {
        case true  => Redirect(routes.Team.mine)
        case false => Redirect(routes.Team.all(page))
      }
    }

  def show(id: String, page: Int) =
    Open { implicit ctx =>
      Reasonable(page) {
        OptionFuOk(api team id) { renderTeam(_, page) }
      }
    }

  def search(text: String, page: Int) =
    OpenBody { implicit ctx =>
      Reasonable(page) {
        if (text.trim.isEmpty) paginator popularTeams page map { html.team.list.all(_) }
        else
          env.teamSearch(text, page) map { html.team.list.search(text, _) }
      }
    }

  private def renderTeam(team: TeamModel, page: Int = 1)(implicit ctx: Context) =
    for {
      info    <- env.teamInfo(team, ctx.me)
      members <- paginator.teamMembers(team, page)
      hasChat = canHaveChat(team, info)
      chat <-
        hasChat ?? env.chat.api.userChat.cached
          .findMine(lila.chat.Chat.Id(team.id), ctx.me)
          .map(some)
      _ <- env.user.lightUserApi preloadMany {
        info.userIds ::: chat.??(_.chat.userIds)
      }
      version <- hasChat ?? env.team.version(team.id).dmap(some)
    } yield html.team.show(team, members, info, chat, version)

  private def canHaveChat(team: TeamModel, info: lila.app.mashup.TeamInfo)(implicit ctx: Context): Boolean =
    team.enabled && !team.isChatFor(_.NONE) && ctx.noKid && {
      (team.isChatFor(_.LEADERS) && ctx.userId.exists(team.leaders)) ||
      (team.isChatFor(_.MEMBERS) && info.mine) ||
      isGranted(_.ChatTimeout)
    }

  def users(teamId: String) =
    Action.async { implicit req =>
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

  def tournaments(teamId: String) =
    Open { implicit ctx =>
      env.team.teamRepo.enabled(teamId) flatMap {
        _ ?? { team =>
          env.teamInfo.tournaments(team, 30, 30) map { tours =>
            Ok(html.team.tournaments.page(team, tours))
          }
        }
      }
    }

  def edit(id: String) =
    Auth { implicit ctx => _ =>
      WithOwnedTeam(id) { team =>
        fuccess(html.team.form.edit(team, forms edit team))
      }
    }

  def update(id: String) =
    AuthBody { implicit ctx => me =>
      WithOwnedTeam(id) { team =>
        implicit val req = ctx.body
        forms
          .edit(team)
          .bindFromRequest()
          .fold(
            err => BadRequest(html.team.form.edit(team, err)).fuccess,
            data => api.update(team, data, me) inject Redirect(routes.Team.show(team.id)).flashSuccess
          )
      }
    }

  def kickForm(id: String) =
    Auth { implicit ctx => me =>
      WithOwnedTeam(id) { team =>
        env.team.memberRepo userIdsByTeam team.id map { userIds =>
          html.team.admin.kick(team, userIds.filter(me.id !=))
        }
      }
    }

  def kick(id: String) =
    AuthBody { implicit ctx => me =>
      WithOwnedTeam(id) { team =>
        implicit val req = ctx.body
        forms.selectMember.bindFromRequest().value ?? { api.kick(team, _, me) } inject Redirect(
          routes.Team.kickForm(team.id)
        ).flashSuccess
      }
    }
  def kickUser(teamId: String, userId: String) =
    Scoped(_.Team.Write) { _ => me =>
      api team teamId flatMap {
        _ ?? { team =>
          if (team leaders me.id) api.kick(team, userId, me) inject jsonOkResult
          else Forbidden(jsonError("Not your team")).fuccess
        }
      }
    }

  def leadersForm(id: String) =
    Auth { implicit ctx => _ =>
      WithOwnedTeam(id) { team =>
        Ok(html.team.admin.leaders(team, forms leaders team)).fuccess
      }
    }

  def leaders(id: String) =
    AuthBody { implicit ctx => me =>
      WithOwnedTeam(id) { team =>
        implicit val req = ctx.body
        forms.leaders(team).bindFromRequest().value ?? {
          api.setLeaders(team, _, me, isGranted(_.ManageTeam))
        } inject Redirect(
          routes.Team.show(team.id)
        ).flashSuccess
      }
    }

  def close(id: String) =
    Secure(_.ManageTeam) { implicit ctx => me =>
      OptionFuResult(api team id) { team =>
        api.delete(team) >>
          env.mod.logApi.deleteTeam(me.id, team.id, team.name) inject
          Redirect(routes.Team all 1).flashSuccess
      }
    }

  def disable(id: String) =
    Auth { implicit ctx => me =>
      WithOwnedTeam(id) { team =>
        api.disable(team, me) >>
          env.mod.logApi.disableTeam(me.id, team.id, team.name) inject
          Redirect(routes.Team show id).flashSuccess
      }
    }

  def form =
    Auth { implicit ctx => me =>
      LimitPerWeek(me) {
        forms.anyCaptcha map { captcha =>
          Ok(html.team.form.create(forms.create, captcha))
        }
      }
    }

  def create =
    AuthBody { implicit ctx => implicit me =>
      api hasJoinedTooManyTeams me flatMap { tooMany =>
        if (tooMany) tooManyTeams(me)
        else
          LimitPerWeek(me) {
            implicit val req = ctx.body
            forms.create
              .bindFromRequest()
              .fold(
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
    }

  def mine =
    Auth { implicit ctx => me =>
      api mine me map {
        html.team.list.mine(_)
      }
    }

  private def tooManyTeams(me: UserModel)(implicit ctx: Context) =
    api mine me map html.team.list.mine map { BadRequest(_) }

  def leader =
    Auth { implicit ctx => me =>
      env.team.teamRepo enabledTeamsByLeader me.id map {
        html.team.list.ledByMe(_)
      }
    }

  def join(id: String) =
    AuthOrScopedBody(_.Team.Write)(
      auth = implicit ctx =>
        me =>
          api.team(id) flatMap {
            _ ?? { team =>
              api hasJoinedTooManyTeams me flatMap { tooMany =>
                if (tooMany)
                  negotiate(
                    html = tooManyTeams(me),
                    api = _ => BadRequest(jsonError("You have joined too many teams")).fuccess
                  )
                else
                  negotiate(
                    html = webJoin(team, me, request = none, password = none),
                    api = _ => {
                      implicit val body = ctx.body
                      forms
                        .apiRequest(team)
                        .bindFromRequest()
                        .fold(
                          newJsonFormError,
                          setup =>
                            api.join(team, me, setup.message, setup.password) flatMap {
                              case Requesting.Joined => jsonOkResult.fuccess
                              case Requesting.NeedRequest =>
                                BadRequest(jsonError("This team requires confirmation.")).fuccess
                              case Requesting.NeedPassword =>
                                BadRequest(jsonError("This team requires a password.")).fuccess
                              case _ => notFoundJson("Team not found")
                            }
                        )
                    }
                  )
              }
            }
          },
      scoped = implicit req =>
        me =>
          api.team(id) flatMap {
            _ ?? { team =>
              implicit val lang = reqLang
              forms
                .apiRequest(team)
                .bindFromRequest()
                .fold(
                  newJsonFormError,
                  setup =>
                    env.oAuth.server.fetchAppAuthor(req) flatMap {
                      api.joinApi(team, me, _, setup.message)
                    } flatMap {
                      case Requesting.Joined => jsonOkResult.fuccess
                      case Requesting.NeedRequest =>
                        Forbidden(
                          jsonError(
                            "This team requires confirmation, and is not owned by the oAuth app owner."
                          )
                        ).fuccess
                    }
                )
            }
          }
    )

  def subscribe(teamId: String) = {
    def doSub(req: Request[_], me: UserModel) =
      Form(single("subscribe" -> optional(boolean)))
        .bindFromRequest()(req, formBinding)
        .fold(_ => funit, v => api.subscribe(teamId, me.id, ~v))
    AuthOrScopedBody(_.Team.Write)(
      auth = ctx => me => doSub(ctx.body, me) inject jsonOkResult,
      scoped = req => me => doSub(req, me) inject jsonOkResult
    )
  }

  def requests =
    Auth { implicit ctx => me =>
      import lila.memo.CacheApi._
      env.team.cached.nbRequests invalidate me.id
      api requestsWithUsers me map { html.team.request.all(_) }
    }

  def requestForm(id: String) =
    Auth { implicit ctx => me =>
      OptionFuOk(api.requestable(id, me)) { team =>
        fuccess(html.team.request.requestForm(team, forms.request(team)))
      }
    }

  def requestCreate(id: String) =
    AuthBody { implicit ctx => me =>
      OptionFuResult(api.requestable(id, me)) { team =>
        implicit val req = ctx.body
        forms
          .request(team)
          .bindFromRequest()
          .fold(
            err => BadRequest(html.team.request.requestForm(team, err)).fuccess,
            setup =>
              if (team.open) webJoin(team, me, request = none, password = setup.password)
              else
                setup.message ?? { msg =>
                  api.createRequest(team, me, msg) inject Redirect(routes.Team.show(team.id)).flashSuccess
                }
          )
      }
    }

  private def webJoin(team: TeamModel, me: UserModel, request: Option[String], password: Option[String]) =
    api.join(team, me, request = request, password = password) flatMap {
      case Requesting.Joined => Redirect(routes.Team.show(team.id)).flashSuccess.fuccess
      case Requesting.NeedRequest | Requesting.NeedPassword =>
        Redirect(routes.Team.requestForm(team.id)).flashSuccess.fuccess
    }

  def requestProcess(requestId: String) =
    AuthBody { implicit ctx => me =>
      import cats.implicits._
      OptionFuRedirectUrl(for {
        requestOption <- api request requestId
        teamOption    <- requestOption.??(req => env.team.teamRepo.byLeader(req.team, me.id))
      } yield (teamOption, requestOption).mapN((_, _))) { case (team, request) =>
        implicit val req = ctx.body
        forms.processRequest
          .bindFromRequest()
          .fold(
            _ => fuccess(routes.Team.show(team.id).toString),
            { case (decision, url) =>
              api.processRequest(team, request, decision == "accept") inject url
            }
          )
      }
    }

  def quit(id: String) =
    AuthOrScoped(_.Team.Write)(
      auth = implicit ctx =>
        me =>
          OptionFuResult(api.cancelRequest(id, me) orElse api.quit(id, me)) { team =>
            negotiate(
              html = Redirect(routes.Team.mine).flashSuccess.fuccess,
              api = _ => jsonOkResult.fuccess
            )
          }(ctx),
      scoped = _ =>
        me =>
          api.quit(id, me) flatMap {
            _.fold(notFoundJson())(_ => jsonOkResult.fuccess)
          }
    )

  def autocomplete =
    Action.async { req =>
      get("term", req).filter(_.nonEmpty) match {
        case None => BadRequest("No search term provided").fuccess
        case Some(term) =>
          for {
            teams <- api.autocomplete(term, 10)
            _     <- env.user.lightUserApi preloadMany teams.map(_.createdBy)
          } yield JsonOk {
            JsArray(teams map { team =>
              Json.obj(
                "id"      -> team.id,
                "name"    -> team.name,
                "owner"   -> env.user.lightUserApi.sync(team.createdBy).fold(team.createdBy)(_.name),
                "members" -> team.nbMembers
              )
            })
          }
      }
    }

  def pmAll(id: String) =
    Auth { implicit ctx => _ =>
      WithOwnedTeam(id) { team =>
        env.tournament.api
          .visibleByTeam(team.id, 0, 20)
          .dmap(_.next)
          .map { tours =>
            Ok(html.team.admin.pmAll(team, forms.pmAll, tours))
          }
      }
    }

  def pmAllSubmit(id: String) =
    AuthOrScopedBody(_.Team.Write)(
      auth = implicit ctx =>
        me =>
          WithOwnedTeam(id) { team =>
            doPmAll(team, me)(ctx.body).fold(
              err =>
                env.tournament.api
                  .visibleByTeam(team.id, 0, 20)
                  .dmap(_.next)
                  .map { tours =>
                    BadRequest(html.team.admin.pmAll(team, err, tours))
                  },
              done => done inject Redirect(routes.Team.show(team.id)).flashSuccess
            )
          },
      scoped = implicit req =>
        me =>
          api team id flatMap {
            _.filter(_ leaders me.id) ?? { team =>
              doPmAll(team, me).fold(
                err => BadRequest(errorsAsJson(err)(reqLang)).fuccess,
                done => done inject jsonOkResult
              )
            }
          }
    )

  // API

  def apiAll(page: Int) =
    Action.async {
      import env.team.jsonView._
      import lila.common.paginator.PaginatorJson._
      JsonOk {
        paginator popularTeams page flatMap { pager =>
          env.user.lightUserApi.preloadMany(pager.currentPageResults.flatMap(_.leaders)) inject pager
        }
      }
    }

  def apiShow(id: String) =
    Open { ctx =>
      JsonOptionOk {
        api team id flatMap {
          _ ?? { team =>
            for {
              joined    <- ctx.userId.?? { api.belongsTo(id, _) }
              requested <- ctx.userId.ifFalse(joined).?? { env.team.requestRepo.exists(id, _) }
            } yield {
              env.team.jsonView.teamWrites.writes(team) ++ Json
                .obj(
                  "joined"    -> joined,
                  "requested" -> requested
                )
            }.some
          }
        }
      }
    }

  def apiSearch(text: String, page: Int) =
    Action.async {
      import env.team.jsonView._
      import lila.common.paginator.PaginatorJson._
      JsonOk {
        if (text.trim.isEmpty) paginator popularTeams page
        else env.teamSearch(text, page)
      }
    }

  def apiTeamsOf(username: String) =
    Action.async {
      import env.team.jsonView._
      JsonOk {
        api teamsOf username flatMap { teams =>
          env.user.lightUserApi.preloadMany(teams.flatMap(_.leaders)) inject teams
        }
      }
    }

  private def doPmAll(team: TeamModel, me: UserModel)(implicit req: Request[_]): Either[Form[_], Funit] =
    forms.pmAll
      .bindFromRequest()
      .fold(
        err => Left(err),
        msg =>
          Right {
            PmAllLimitPerUser(me.id) {
              val url  = s"${env.net.baseUrl}${routes.Team.show(team.id)}"
              val full = s"""$msg
---
You received this because you are subscribed to messages of the team $url."""
              env.msg.api
                .multiPost(me, env.team.memberStream.subscribedIds(team, MaxPerSecond(50)), full)
                .addEffect { nb =>
                  lila.mon.msg.teamBulk(team.id).record(nb).unit
                }
              funit // we don't wait for the stream to complete, it would make lichess time out
            }(funit)
          }
      )

  private val PmAllLimitPerUser = lila.memo.RateLimit.composite[lila.user.User.ID](
    key = "team.pm.all",
    enforce = env.net.rateLimit.value
  )(
    ("fast", 1, 3.minutes),
    ("slow", 4, 24.hours)
  )

  private def LimitPerWeek[A <: Result](me: UserModel)(a: => Fu[A])(implicit ctx: Context): Fu[Result] =
    api.countCreatedRecently(me) flatMap { count =>
      val allow =
        isGranted(_.ManageTeam) ||
          (isGranted(_.Verified) && count < 100) ||
          (isGranted(_.Teacher) && count < 10) ||
          count < 3
      if (allow) a
      else Forbidden(views.html.site.message.teamCreateLimit).fuccess
    }

  private def WithOwnedTeam(teamId: String)(f: TeamModel => Fu[Result])(implicit ctx: Context): Fu[Result] =
    OptionFuResult(api team teamId) { team =>
      if (ctx.userId.exists(team.leaders.contains) || isGranted(_.ManageTeam)) f(team)
      else renderTeam(team) map { Forbidden(_) }
    }
}
