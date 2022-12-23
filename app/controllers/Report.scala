package controllers
package report

import play.api.mvc.{ AnyContentAsFormUrlEncoded, Result }
import views.*

import lila.api.{ BodyContext, Context }
import lila.app.{ given, * }
import lila.common.HTTPRequest
import lila.report.{ Mod as AsMod, Report as ReportModel, Reporter, Room, Suspect }
import lila.report.Report.{ Id as ReportId }
import lila.user.{ Holder, User as UserModel }

import play.api.data.*

final class Report(
    env: Env,
    userC: => User,
    modC: => Mod
) extends LilaController(env):

  private def api = env.report.api

  private given Conversion[Holder, AsMod] = holder => AsMod(holder.user)

  def list =
    Secure(_.SeeReport) { implicit ctx => me =>
      if (env.streamer.liveStreamApi.isStreaming(me.user.id) && !getBool("force"))
        fuccess(Forbidden(html.site.message.streamingMod))
      else renderList(me, env.report.modFilters.get(me).fold("all")(_.key))
    }

  def listWithFilter(room: String) =
    Secure(_.SeeReport) { implicit ctx => me =>
      env.report.modFilters.set(me, Room(room))
      if (Room(room).fold(true)(Room.isGrantedFor(me))) renderList(me, room)
      else notFound
    }

  protected[controllers] def getScores =
    api.maxScores zip env.streamer.api.approval.countRequests zip env.appeal.api.countUnread

  private def renderList(me: Holder, room: String)(implicit ctx: Context) =
    api.openAndRecentWithFilter(me, 12, Room(room)) zip getScores flatMap {
      case (reports, ((scores, streamers), appeals)) =>
        env.user.lightUserApi.preloadMany(reports.flatMap(_.report.userIds)) inject
          Ok(
            html.report
              .list(
                reports.filter(r => lila.report.Reason.isGrantedFor(me)(r.report.reason)),
                room,
                scores,
                streamers,
                appeals
              )
          )
    }

  def inquiry(reportOrAppealId: String) =
    Secure(_.SeeReport) { _ => me =>
      api.inquiries.toggle(me, reportOrAppealId) flatMap { (prev, next) =>
        prev.filter(_.isAppeal).map(_.user).??(env.appeal.api.setUnreadById) inject
          next.fold(
            Redirect {
              if (prev.exists(_.isAppeal)) appeal.routes.Appeal.queue
              else report.routes.Report.list
            }
          )(onInquiryStart)
      }
    }

  private def onInquiryStart(inquiry: ReportModel): Result =
    if (inquiry.isRecentComm) Redirect(controllers.routes.Mod.communicationPrivate(inquiry.user))
    else if (inquiry.isComm) Redirect(controllers.routes.Mod.communicationPublic(inquiry.user))
    else modC.redirect(inquiry.user)

  protected[controllers] def onInquiryClose(
      inquiry: Option[ReportModel],
      me: Holder,
      goTo: Option[Suspect],
      force: Boolean = false
  )(using ctx: BodyContext[?]): Fu[Result] =
    goTo.ifTrue(HTTPRequest isXhr ctx.req) match
      case Some(suspect) => userC.renderModZoneActions(suspect.user.username)
      case None =>
        inquiry match
          case None =>
            goTo.fold(Redirect(routes.Report.list).toFuccess) { s =>
              userC.modZoneOrRedirect(me, s.user.username)
            }
          case Some(prev) if prev.isSpontaneous => Redirect(modC.userUrl(prev.user, mod = true)).toFuccess
          case Some(prev) =>
            val dataOpt = ctx.body.body match
              case AnyContentAsFormUrlEncoded(data) => data.some
              case _                                => none
            def thenGoTo =
              dataOpt.flatMap(_ get "then").flatMap(_.headOption) flatMap {
                case "profile" => modC.userUrl(prev.user, mod = true).some
                case url       => url.some
              }
            thenGoTo match
              case Some(url) => Redirect(url).toFuccess
              case _ =>
                def redirectToList = Redirect(routes.Report.listWithFilter(prev.room.key))
                if (prev.isAppeal) Redirect(appeal.routes.Appeal.queue).toFuccess
                else if (dataOpt.flatMap(_ get "next").exists(_.headOption contains "1"))
                  api.inquiries.toggleNext(me, prev.room) map {
                    _.fold(redirectToList)(onInquiryStart)
                  }
                else if (force) userC.modZoneOrRedirect(me, prev.user)
                else
                  api.inquiries.toggle(me, Left(prev.id)) map { (prev, next) =>
                    next
                      .fold(
                        if (prev.exists(_.isAppeal)) Redirect(appeal.routes.Appeal.queue)
                        else redirectToList
                      )(onInquiryStart)
                  }

  def process(id: ReportId) =
    SecureBody(_.SeeReport) { implicit ctx => me =>
      api byId id flatMap { inquiry =>
        inquiry.filter(_.isAppeal).map(_.user).??(env.appeal.api.setReadById) >>
          api.process(me, id) >>
          onInquiryClose(inquiry, me, none, force = true)
      }
    }

  def xfiles(id: UserStr) =
    Secure(_.SeeReport) { _ => _ =>
      api.moveToXfiles(id.id) inject Redirect(routes.Report.list)
    }

  def snooze(id: ReportId, dur: String) =
    SecureBody(_.SeeReport) { implicit ctx => me =>
      api.snooze(me, id, dur) map {
        _.fold(Redirect(routes.Report.list))(onInquiryStart)
      }
    }

  def currentCheatInquiry(username: UserStr) =
    Secure(_.CheatHunter) { implicit ctx => me =>
      OptionFuResult(env.user.repo byId username) { user =>
        api.currentCheatReport(lila.report.Suspect(user)) flatMap {
          _ ?? { report =>
            api.inquiries.toggle(me, Left(report.id)).void
          } inject modC.redirect(username, mod = true)
        }
      }
    }

  def form =
    Auth { implicit ctx => _ =>
      getUserStr("username") ?? env.user.repo.byId flatMap { user =>
        if (user.map(_.id) has UserModel.lichessId) Redirect(controllers.routes.Main.contact).toFuccess
        else
          env.report.forms.createWithCaptcha map { case (form, captcha) =>
            val filledForm: Form[lila.report.ReportSetup] = (user, get("postUrl")) match
              case (Some(u), Some(pid)) =>
                form.fill(
                  lila.report
                    .ReportSetup(user = u.light, reason = ~get("reason"), text = s"$pid\n\n", GameId(""), "")
                )
              case _ => form
            Ok(html.report.form(filledForm, user, captcha))
          }
      }
    }

  def create =
    AuthBody { implicit ctx => implicit me =>
      given play.api.mvc.Request[?] = ctx.body
      env.report.forms.create
        .bindFromRequest()
        .fold(
          err =>
            getUserStr("username") ?? env.user.repo.byId flatMap { user =>
              env.report.forms.anyCaptcha map { captcha =>
                BadRequest(html.report.form(err, user, captcha))
              }
            },
          data =>
            if (data.user.id == me.id) notFound
            else
              api.create(data, Reporter(me)) inject
                Redirect(routes.Report.thanks(data.user.name))
        )
    }

  def flag =
    AuthBody { implicit ctx => implicit me =>
      given play.api.mvc.Request[?] = ctx.body
      env.report.forms.flag
        .bindFromRequest()
        .fold(
          _ => BadRequest.toFuccess,
          data =>
            env.user.repo byId data.username flatMap {
              _ ?? { user =>
                if (user == me) BadRequest.toFuccess
                else api.commFlag(Reporter(me), Suspect(user), data.resource, data.text) inject jsonOkResult
              }
            }
        )
    }

  def thanks(reported: UserStr) =
    Auth { implicit ctx => me =>
      env.relation.api.fetchBlocks(me.id, reported.id) map { blocked =>
        html.report.thanks(reported.id, blocked)
      }
    }
