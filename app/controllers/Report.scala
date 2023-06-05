package controllers
package report

import play.api.mvc.{ AnyContentAsFormUrlEncoded, Result }
import play.api.data.*
import views.*

import lila.api.{ BodyContext, Context }
import lila.app.{ given, * }
import lila.common.HTTPRequest
import lila.report.{ Mod as AsMod, Report as ReportModel, Reporter, Room, Suspect }
import lila.report.Report.{ Id as ReportId }
import lila.user.{ Holder, User as UserModel }

final class Report(
    env: Env,
    userC: => User,
    modC: => Mod
) extends LilaController(env):

  import env.report.api

  private given Conversion[Holder, AsMod] = holder => AsMod(holder.user)

  def list = Secure(_.SeeReport) { _ ?=> me =>
    if env.streamer.liveStreamApi.isStreaming(me.user.id) && !getBool("force")
    then fuccess(Forbidden(html.site.message.streamingMod))
    else renderList(me, env.report.modFilters.get(me).fold("all")(_.key))
  }

  def listWithFilter(room: String) = Secure(_.SeeReport) { _ ?=> me =>
    env.report.modFilters.set(me, Room(room))
    if Room(room).fold(true)(Room.isGrantedFor(me))
    then renderList(me, room)
    else notFound
  }

  protected[controllers] def getScores =
    api.maxScores zip env.streamer.api.approval.countRequests zip env.appeal.api.countUnread

  private def renderList(me: Holder, room: String)(using Context) =
    api.openAndRecentWithFilter(me, 12, Room(room)) zip getScores flatMap {
      case (reports, ((scores, streamers), appeals)) =>
        env.user.lightUserApi.preloadMany(reports.flatMap(_.report.userIds)) inject
          Ok:
            html.report
              .list(
                reports.filter(r => lila.report.Reason.isGrantedFor(me)(r.report.reason)),
                room,
                scores,
                streamers,
                appeals
              )
    }

  def inquiry(reportOrAppealId: String) = Secure(_.SeeReport) { _ ?=> me =>
    api.inquiries
      .toggle(me, reportOrAppealId)
      .flatMap: (prev, next) =>
        prev.filter(_.isAppeal).map(_.user).??(env.appeal.api.setUnreadById) inject
          next.fold(
            Redirect:
              if prev.exists(_.isAppeal)
              then appeal.routes.Appeal.queue
              else report.routes.Report.list
          )(onInquiryStart)
  }

  private def onInquiryStart(inquiry: ReportModel): Result =
    if (inquiry.isRecentComm) Redirect(controllers.routes.Mod.communicationPrivate(inquiry.user))
    else if (inquiry.isComm) Redirect(controllers.routes.Mod.communicationPublic(inquiry.user))
    else modC.redirect(inquiry.user)

  protected[controllers] def onModAction(me: Holder)(goTo: Suspect)(using ctx: BodyContext[?]): Fu[Result] =
    if HTTPRequest.isXhr(ctx.req) then userC.renderModZoneActions(goTo.user.username)
    else
      api.inquiries
        .ofModId(me.id)
        .flatMap(_.fold(userC.modZoneOrRedirect(me, goTo.user.username))(onInquiryAction(_, me)))

  protected[controllers] def onInquiryAction(
      inquiry: ReportModel,
      me: Holder,
      processed: Boolean = false
  )(using ctx: BodyContext[?]): Fu[Result] =
    val dataOpt = ctx.body.body match
      case AnyContentAsFormUrlEncoded(data) => data.some
      case _                                => none
    def thenGoTo =
      dataOpt.flatMap(_ get "then").flatMap(_.headOption) flatMap {
        case "profile" => modC.userUrl(inquiry.user, mod = true).some
        case url       => url.some
      }
    def process() = !processed ?? api.process(me, inquiry)
    thenGoTo match
      case Some(url) => process() >> Redirect(url).toFuccess
      case _ =>
        def redirectToList = Redirect(routes.Report.listWithFilter(inquiry.room.key))
        if inquiry.isAppeal then process() >> Redirect(appeal.routes.Appeal.queue).toFuccess
        else if dataOpt.flatMap(_ get "next").exists(_.headOption contains "1") then
          process() >> {
            if inquiry.isSpontaneous then Redirect(modC.userUrl(inquiry.user, mod = true)).toFuccess
            else
              api.inquiries.toggleNext(me, inquiry.room) map {
                _.fold(redirectToList)(onInquiryStart)
              }
          }
        else if processed then userC.modZoneOrRedirect(me, inquiry.user)
        else onInquiryStart(inquiry).toFuccess

  def process(id: ReportId) = SecureBody(_.SeeReport) { _ ?=> me =>
    api byId id flatMap {
      _.fold(Redirect(routes.Report.list).toFuccess): inquiry =>
        inquiry.isAppeal.??(env.appeal.api.setReadById(inquiry.user)) >>
          api.process(me, inquiry) >>
          onInquiryAction(inquiry, me, processed = true)
    }
  }

  def xfiles(id: UserStr) = Secure(_.SeeReport) { _ ?=> _ =>
    api.moveToXfiles(id.id) inject Redirect(routes.Report.list)
  }

  def snooze(id: ReportId, dur: String) = SecureBody(_.SeeReport) { _ ?=> me =>
    api
      .snooze(me, id, dur)
      .map:
        _.fold(Redirect(routes.Report.list))(onInquiryStart)
  }

  def currentCheatInquiry(username: UserStr) = Secure(_.CheatHunter) { _ ?=> me =>
    OptionFuResult(env.user.repo byId username): user =>
      api.currentCheatReport(lila.report.Suspect(user)) flatMapz { report =>
        api.inquiries.toggle(me, Left(report.id)).void
      }
  }

  def form = Auth { _ ?=> _ =>
    getUserStr("username") ?? env.user.repo.byId flatMap { user =>
      if (user.map(_.id) has UserModel.lichessId) Redirect(controllers.routes.Main.contact).toFuccess
      else
        env.report.forms.createWithCaptcha map { (form, captcha) =>
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

  def create = AuthBody { _ ?=> me =>
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
          if data.user.id == me.id then notFound
          else
            api.create(data, Reporter(me)) inject
              Redirect(routes.Report.thanks).flashing("reported" -> data.user.name.value)
      )
  }

  def flag = AuthBody { _ ?=> me =>
    env.report.forms.flag
      .bindFromRequest()
      .fold(
        _ => BadRequest.toFuccess,
        data =>
          env.user.repo byId data.username flatMapz { user =>
            if (user == me) BadRequest.toFuccess
            else api.commFlag(Reporter(me), Suspect(user), data.resource, data.text) inject jsonOkResult
          }
      )
  }

  def thanks = Auth { ctx ?=> me =>
    ctx.req.flash.get("reported").flatMap(UserStr.read).fold(Redirect("/").toFuccess) { reported =>
      env.relation.api.fetchBlocks(me.id, reported.id) map { blocked =>
        html.report.thanks(reported.id, blocked)
      }
    }
  }
