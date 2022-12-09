package controllers

import play.api.data.Form
import play.api.data.Forms.*
import play.api.libs.json.*
import play.api.mvc.*
import scala.concurrent.duration.*
import views.*

import lila.api.Context
import lila.app.{ given, * }
import lila.common.{ config, HTTPRequest, IpAddress }
import lila.memo.RateLimit
import lila.team.{ Requesting, Team as TeamModel }
import lila.user.{ Holder, User as UserModel }
import Api.ApiResult

final class Team(
    env: Env,
    apiC: => Api
) extends LilaController(env):

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

  def show(id: TeamId, page: Int, mod: Boolean) =
    Open { implicit ctx =>
      Reasonable(page) {
        OptionFuResult(api team id) { renderTeam(_, page, mod) }
      }
    }

  def members(id: TeamId, page: Int) =
    Open { implicit ctx =>
      Reasonable(page, config.Max(50)) {
        OptionFuResult(api teamEnabled id) { team =>
          val canSee =
            fuccess(team.publicMembers || isGranted(_.ManageTeam)) >>| ctx.userId.?? {
              api.belongsTo(team.id, _)
            }
          canSee flatMap {
            case true =>
              paginator.teamMembersWithDate(team, page) map {
                html.team.members(team, _)
              }
            case false => authorizationFailed
          }
        }
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

  private def renderTeam(team: TeamModel, page: Int = 1, requestModView: Boolean = false)(using
      ctx: Context
  ) =
    for {
      info    <- env.teamInfo(team, ctx.me, withForum = canHaveForum(team, requestModView))
      members <- paginator.teamMembers(team, page)
      log     <- (requestModView && isGranted(_.ManageTeam)).??(env.mod.logApi.teamLog(team.id))
      hasChat = canHaveChat(team, info, requestModView)
      chat <-
        hasChat ?? env.chat.api.userChat.cached
          .findMine(ChatId(team.id), ctx.me)
          .map(some)
      _ <- env.user.lightUserApi preloadMany {
        team.leaders.toList ::: info.userIds ::: chat.??(_.chat.userIds)
      }
      version <- hasChat ?? env.team.version(team.id).dmap(some)
    } yield Ok(html.team.show(team, members, info, chat, version, requestModView, log))
      .withCanonical(routes.Team.show(team.id))

  private def canHaveChat(team: TeamModel, info: lila.app.mashup.TeamInfo, requestModView: Boolean = false)(
      implicit ctx: Context
  ): Boolean =
    team.enabled && !team.isChatFor(_.NONE) && ctx.noKid && HTTPRequest.isHuman(ctx.req) && {
      (team.isChatFor(_.LEADERS) && ctx.userId.exists(team.leaders)) ||
      (team.isChatFor(_.MEMBERS) && info.mine) ||
      (isGranted(_.Shusher) && requestModView)
    }

  private def canHaveForum(team: TeamModel, requestModView: Boolean)(isMember: Boolean)(using
      ctx: Context
  ): Boolean =
    team.enabled && !team.isForumFor(_.NONE) && ctx.noKid && {
      team.isForumFor(_.EVERYONE) ||
      (team.isForumFor(_.LEADERS) && ctx.userId.exists(team.leaders)) ||
      (team.isForumFor(_.MEMBERS) && isMember) ||
      (isGranted(_.ModerateForum) && requestModView)
    }

  def users(teamId: TeamId) =
    AnonOrScoped(_.Team.Read) { req => me =>
      api teamEnabled teamId flatMap {
        _ ?? { team =>
          val canView: Fu[Boolean] =
            if (team.publicMembers) fuccess(true)
            else me.??(u => api.belongsTo(team.id, u.id))
          canView map {
            case true =>
              apiC.jsonStream(
                env.team
                  .memberStream(team, config.MaxPerSecond(20))
                  .map(env.api.userApi.one)
              )(req)
            case false => Unauthorized
          }
        }
      }
    }

  def tournaments(teamId: TeamId) =
    Open { implicit ctx =>
      api teamEnabled teamId flatMap {
        _ ?? { team =>
          env.teamInfo.tournaments(team, 30, 30) map { tours =>
            Ok(html.team.tournaments.page(team, tours))
          }
        }
      }
    }

  def edit(id: TeamId) =
    Auth { implicit ctx => _ =>
      WithOwnedTeamEnabled(id) { team =>
        fuccess(html.team.form.edit(team, forms edit team))
      }
    }

  def update(id: TeamId) =
    AuthBody { implicit ctx => me =>
      WithOwnedTeamEnabled(id) { team =>
        given play.api.mvc.Request[?] = ctx.body
        forms
          .edit(team)
          .bindFromRequest()
          .fold(
            err => BadRequest(html.team.form.edit(team, err)).toFuccess,
            data => api.update(team, data, me) inject Redirect(routes.Team.show(team.id)).flashSuccess
          )
      }
    }

  def kickForm(id: TeamId) =
    Auth { implicit ctx => _ =>
      WithOwnedTeamEnabled(id) { team =>
        Ok(html.team.admin.kick(team, forms.members)).toFuccess
      }
    }

  def kick(id: TeamId) =
    AuthBody { implicit ctx => me =>
      WithOwnedTeamEnabled(id) { team =>
        given play.api.mvc.Request[?] = ctx.body
        forms.members.bindFromRequest().value ?? { api.kickMembers(team, _, me).sequenceFu } inject Redirect(
          routes.Team.show(team.id)
        ).flashSuccess
      }
    }

  private val ApiKickRateLimitPerIP = lila.memo.RateLimit.composite[IpAddress](
    key = "team.kick.api.ip",
    enforce = env.net.rateLimit.value
  )(
    ("fast", 10, 2.minutes),
    ("slow", 50, 1.day)
  )
  private val kickLimitReportOnce = lila.memo.OnceEvery[UserId](10.minutes)

  def kickUser(teamId: TeamId, username: UserStr) =
    Scoped(_.Team.Lead) { req => me =>
      WithOwnedTeamEnabledApi(teamId, me) { team =>
        ApiKickRateLimitPerIP[Fu[ApiResult]](
          HTTPRequest ipAddress req,
          cost = if (me.isVerified || me.isApiHog) 0 else 1
        ) {
          api.kick(team, username.id, me) inject ApiResult.Done
        } {
          if (kickLimitReportOnce(username.id))
            lila
              .log("security")
              .warn(s"API team.kick limited team:${teamId} user:${me.id} ip:${HTTPRequest ipAddress req}")
          fuccess(ApiResult.Limited)
        }
      }
    }

  def leadersForm(id: TeamId) =
    Auth { implicit ctx => _ =>
      WithOwnedTeamEnabled(id) { team =>
        Ok(html.team.admin.leaders(team, forms leaders team)).toFuccess
      }
    }

  def leaders(id: TeamId) =
    AuthBody { implicit ctx => me =>
      WithOwnedTeamEnabled(id) { team =>
        given play.api.mvc.Request[?] = ctx.body
        forms.leaders(team).bindFromRequest().value ?? {
          api.setLeaders(team, _, me, isGranted(_.ManageTeam))
        } inject Redirect(
          routes.Team.show(team.id)
        ).flashSuccess
      }
    }

  def close(id: TeamId) =
    SecureBody(_.ManageTeam) { implicit ctx => me =>
      OptionFuResult(api team id) { team =>
        implicit val body: play.api.mvc.Request[?] = ctx.body
        forms.explain
          .bindFromRequest()
          .fold(
            _ => funit,
            explain =>
              api.delete(team, me.user, explain) >>
                env.mod.logApi.deleteTeam(me.id into ModId, team.id, explain)
          ) inject Redirect(routes.Team all 1).flashSuccess
      }
    }

  def disable(id: TeamId) =
    AuthBody { implicit ctx => me =>
      WithOwnedTeamEnabled(id) { team =>
        implicit val body: play.api.mvc.Request[?] = ctx.body
        forms.explain
          .bindFromRequest()
          .fold(
            _ => funit,
            explain =>
              api.toggleEnabled(team, me, explain) >> {
                env.mod.logApi.toggleTeam(me.id into ModId, team.id, team.enabled, explain)
              }
          )
      } inject Redirect(routes.Team show id).flashSuccess
    }

  def form =
    Auth { implicit ctx => me =>
      LimitPerWeek(me) {
        forms.anyCaptcha map { captcha =>
          Ok(html.team.form.create(forms.create, captcha))
        }
      }
    }

  private val OneAtATime = new lila.memo.FutureConcurrencyLimit[UserId](
    key = "team.concurrency.user",
    ttl = 10.minutes,
    maxConcurrency = 1
  )
  def create = AuthBody { implicit ctx => implicit me =>
    OneAtATime(me.id, rateLimitedFu) {
      api hasJoinedTooManyTeams me flatMap { tooMany =>
        if (tooMany) tooManyTeams(me)
        else
          LimitPerWeek(me) {
            given play.api.mvc.Request[?] = ctx.body
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

  def join(id: TeamId) =
    AuthOrScopedBody(_.Team.Write)(
      auth = implicit ctx =>
        me =>
          api.teamEnabled(id) flatMap {
            _ ?? { team =>
              OneAtATime(me.id, rateLimitedFu) {
                api hasJoinedTooManyTeams me flatMap { tooMany =>
                  if (tooMany)
                    negotiate(
                      html = tooManyTeams(me),
                      api = _ => BadRequest(jsonError("You have joined too many teams")).toFuccess
                    )
                  else
                    negotiate(
                      html = webJoin(team, me, request = none, password = none),
                      api = _ => {
                        implicit val body: play.api.mvc.Request[?] = ctx.body
                        forms
                          .apiRequest(team)
                          .bindFromRequest()
                          .fold(
                            newJsonFormError,
                            setup =>
                              api.join(team, me, setup.message, setup.password) flatMap {
                                case Requesting.Joined => jsonOkResult.toFuccess
                                case Requesting.NeedRequest =>
                                  BadRequest(jsonError("This team requires confirmation.")).toFuccess
                                case Requesting.NeedPassword =>
                                  BadRequest(jsonError("This team requires a password.")).toFuccess
                              }
                          )
                      }
                    )
                }
              }
            }
          },
      scoped = implicit req =>
        me =>
          api.team(id) flatMap {
            _ ?? { team =>
              given play.api.i18n.Lang = reqLang
              forms
                .apiRequest(team)
                .bindFromRequest()
                .fold(
                  newJsonFormError,
                  setup =>
                    OneAtATime(me.id, rateLimitedFu) {
                      api.join(team, me, setup.message, setup.password) flatMap {
                        case Requesting.Joined => jsonOkResult.toFuccess
                        case Requesting.NeedPassword =>
                          Forbidden(jsonError("This team requires a password.")).toFuccess
                        case Requesting.NeedRequest =>
                          Forbidden(
                            jsonError(
                              "This team requires confirmation, and is not owned by the oAuth app owner."
                            )
                          ).toFuccess
                      }
                    }
                )
            }
          }
    )

  def subscribe(teamId: TeamId) =
    def doSub(req: Request[?], me: UserModel) =
      Form(single("subscribe" -> optional(boolean)))
        .bindFromRequest()(req, formBinding)
        .fold(_ => funit, v => api.subscribe(teamId, me.id, ~v))
    AuthOrScopedBody(_.Team.Write)(
      auth = ctx => me => doSub(ctx.body, me) inject jsonOkResult,
      scoped = req => me => doSub(req, me) inject jsonOkResult
    )

  def requests =
    Auth { implicit ctx => me =>
      import lila.memo.CacheApi.*
      env.team.cached.nbRequests invalidate me.id
      api requestsWithUsers me map { html.team.request.all(_) }
    }

  def requestForm(id: TeamId) =
    Auth { implicit ctx => me =>
      OptionFuOk(api.requestable(id, me)) { team =>
        fuccess(html.team.request.requestForm(team, forms.request(team)))
      }
    }

  def requestCreate(id: TeamId) =
    AuthBody { implicit ctx => me =>
      OptionFuResult(api.requestable(id, me)) { team =>
        given play.api.mvc.Request[?] = ctx.body
        forms
          .request(team)
          .bindFromRequest()
          .fold(
            err => BadRequest(html.team.request.requestForm(team, err)).toFuccess,
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
      case Requesting.Joined => Redirect(routes.Team.show(team.id)).flashSuccess.toFuccess
      case Requesting.NeedRequest | Requesting.NeedPassword =>
        Redirect(routes.Team.requestForm(team.id)).flashSuccess.toFuccess
    }

  def requestProcess(requestId: String) =
    AuthBody { implicit ctx => me =>
      import cats.implicits.*
      OptionFuRedirectUrl(for {
        requestOption <- api request requestId
        teamOption    <- requestOption.??(req => env.team.teamRepo.byLeader(req.team, me.id))
      } yield (teamOption, requestOption).mapN((_, _))) { case (team, request) =>
        given play.api.mvc.Request[?] = ctx.body
        forms.processRequest
          .bindFromRequest()
          .fold(
            _ => fuccess(routes.Team.show(team.id).toString),
            { case (decision, url) =>
              api.processRequest(team, request, decision) inject url
            }
          )
      }
    }

  def declinedRequests(id: TeamId, page: Int) =
    Auth { implicit ctx => _ =>
      WithOwnedTeamEnabled(id) { team =>
        paginator.declinedRequests(team, page) map { requests =>
          Ok(html.team.declinedRequest.all(team, requests))
        }
      }
    }

  def quit(id: TeamId) =
    AuthOrScoped(_.Team.Write)(
      auth = implicit ctx =>
        me =>
          OptionFuResult(api team id) { team =>
            if (team isOnlyLeader me.id)
              negotiate(
                html = Redirect(routes.Team.edit(team.id))
                  .flashFailure(lila.i18n.I18nKeys.team.onlyLeaderLeavesTeam.txt())
                  .toFuccess,
                api = _ => jsonOkResult.toFuccess
              )
            else
              api.cancelRequestOrQuit(team, me) >>
                negotiate(
                  html = Redirect(routes.Team.mine).flashSuccess.toFuccess,
                  api = _ => jsonOkResult.toFuccess
                )
          }(ctx),
      scoped = _ =>
        me =>
          api team id flatMap {
            _.fold(notFoundJson()) { team =>
              api.cancelRequestOrQuit(team, me) inject jsonOkResult
            }
          }
    )

  def autocomplete =
    Action.async { req =>
      get("term", req).filter(_.nonEmpty) match
        case None => BadRequest("No search term provided").toFuccess
        case Some(term) =>
          for {
            teams <- api.autocomplete(term, 10)
            _     <- env.user.lightUserApi preloadMany teams.map(_.createdBy)
          } yield JsonOk {
            JsArray(teams map { team =>
              Json.obj(
                "id"      -> team.id,
                "name"    -> team.name,
                "owner"   -> env.user.lightUserApi.syncFallback(team.createdBy).name,
                "members" -> team.nbMembers
              )
            })
          }
    }

  def pmAll(id: TeamId) =
    Auth { implicit ctx => _ =>
      WithOwnedTeamEnabled(id) { team =>
        renderPmAll(team, forms.pmAll)
      }
    }

  private def renderPmAll(team: TeamModel, form: Form[?])(implicit ctx: Context) =
    for {
      tours  <- env.tournament.api.visibleByTeam(team.id, 0, 20).dmap(_.next)
      unsubs <- env.team.cached.unsubs.get(team.id)
    } yield Ok(html.team.admin.pmAll(team, form, tours, unsubs))

  def pmAllSubmit(id: TeamId) =
    AuthOrScopedBody(_.Team.Lead)(
      auth = implicit ctx =>
        me =>
          WithOwnedTeamEnabled(id) { team =>
            doPmAll(team, me)(using ctx.body).fold(
              err => renderPmAll(team, err),
              _ map { res =>
                Redirect(routes.Team.show(team.id))
                  .flashing(res match {
                    case RateLimit.Result.Through => "success" -> ""
                    case RateLimit.Result.Limited => "failure" -> rateLimitedMsg
                  })
              }
            )
          },
      scoped = implicit req =>
        me =>
          api teamEnabled id flatMap {
            _.filter(_ leaders me.id) ?? { team =>
              doPmAll(team, me).fold(
                err => BadRequest(errorsAsJson(err)(using reqLang)).toFuccess,
                _ map {
                  case RateLimit.Result.Through => jsonOkResult
                  case RateLimit.Result.Limited => rateLimitedJson
                }
              )
            }
          }
    )

  // API

  def apiAll(page: Int) =
    Action.async {
      import env.team.jsonView.given
      import lila.common.paginator.PaginatorJson.given
      JsonOk {
        paginator popularTeams page flatMap { pager =>
          env.user.lightUserApi.preloadMany(pager.currentPageResults.flatMap(_.leaders)) inject pager
        }
      }
    }

  def apiShow(id: TeamId) =
    Open { ctx =>
      JsonOptionOk {
        api teamEnabled id flatMap {
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
      import env.team.jsonView.given
      import lila.common.paginator.PaginatorJson.given
      JsonOk {
        if (text.trim.isEmpty) paginator popularTeams page
        else env.teamSearch(text, page)
      }
    }

  def apiTeamsOf(username: UserStr) =
    Action.async {
      import env.team.jsonView.given
      JsonOk {
        api teamsOf username flatMap { teams =>
          env.user.lightUserApi.preloadMany(teams.flatMap(_.leaders)) inject teams
        }
      }
    }

  def apiRequests(teamId: TeamId) =
    Scoped(_.Team.Read) { _ => me =>
      WithOwnedTeamEnabledApi(teamId, me) { team =>
        api.requestsWithUsers(team) map { reqs =>
          ApiResult.Data(JsArray(reqs map env.team.jsonView.requestWithUserWrites.writes))
        }
      }
    }

  def apiRequestProcess(teamId: TeamId, userId: UserStr, decision: String) =
    Scoped(_.Team.Lead) { req => me =>
      WithOwnedTeamEnabledApi(teamId, me) { team =>
        api request lila.team.Request.makeId(team.id, userId.id) flatMap {
          case None      => fuccess(ApiResult.ClientError("No such team join request"))
          case Some(req) => api.processRequest(team, req, decision) inject ApiResult.Done
        }
      }
    }

  private def doPmAll(team: TeamModel, me: UserModel)(using
      req: Request[?]
  ): Either[Form[?], Fu[RateLimit.Result]] =
    forms.pmAll
      .bindFromRequest()
      .fold(
        err => Left(err),
        msg =>
          Right {
            PmAllLimitPerTeam[RateLimit.Result](team.id, if (me.isVerifiedOrAdmin) 1 else pmAllCost) {
              val url = s"${env.net.baseUrl}${routes.Team.show(team.id)}"
              val full = s"""$msg
---
You received this because you are subscribed to messages of the team $url."""
              env.msg.api
                .multiPost(
                  Holder(me),
                  env.team.memberStream.subscribedIds(team, config.MaxPerSecond(50)),
                  full
                )
                .addEffect { nb =>
                  lila.mon.msg.teamBulk(team.id).record(nb).unit
                }
              // we don't wait for the stream to complete, it would make lichess time out
              fuccess(RateLimit.Result.Through)
            }(RateLimit.Result.Limited)
          }
      )

  private val pmAllCost = 5
  private val PmAllLimitPerTeam = env.memo.mongoRateLimitApi[lila.team.TeamId](
    "team.pm.all",
    credits = 7 * pmAllCost,
    duration = 7.days
  )

  private def LimitPerWeek[A <: Result](me: UserModel)(a: => Fu[A])(implicit ctx: Context): Fu[Result] =
    api.countCreatedRecently(me) flatMap { count =>
      val allow =
        isGranted(_.ManageTeam) ||
          (isGranted(_.Verified) && count < 100) ||
          (isGranted(_.Teacher) && count < 10) ||
          count < 3
      if (allow) a
      else Forbidden(views.html.site.message.teamCreateLimit).toFuccess
    }

  private def WithOwnedTeam(teamId: TeamId)(f: TeamModel => Fu[Result])(implicit ctx: Context): Fu[Result] =
    OptionFuResult(api team teamId) { team =>
      if (ctx.userId.exists(team.leaders.contains) || isGranted(_.ManageTeam)) f(team)
      else Redirect(routes.Team.show(team.id)).toFuccess
    }

  private def WithOwnedTeamEnabled(
      teamId: TeamId
  )(f: TeamModel => Fu[Result])(implicit ctx: Context): Fu[Result] =
    WithOwnedTeam(teamId) { team =>
      if (team.enabled || isGranted(_.ManageTeam)) f(team)
      else notFound
    }

  private def WithOwnedTeamEnabledApi(teamId: TeamId, me: UserModel)(
      f: TeamModel => Fu[ApiResult]
  ): Fu[Result] =
    api teamEnabled teamId flatMap {
      case Some(team) if team leaders me.id => f(team)
      case Some(_)                          => fuccess(ApiResult.ClientError("Not your team"))
      case None                             => fuccess(ApiResult.NoData)
    } map apiC.toHttp
