package controllers
package report

import play.api.data.*
import play.api.mvc.{ AnyContentAsFormUrlEncoded, Result }

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.core.id.ReportId
import lila.report.ui.PendingCounts
import lila.report.Room.Scores
import lila.report.{ Mod as AsMod, Report as ReportModel, Reporter, Room, Suspect }

final class Report(env: Env, userC: => User, modC: => Mod) extends LilaController(env):

  import env.report.api

  private given Conversion[Me, AsMod] = me => AsMod(me)

  def list = Secure(_.SeeReport) { _ ?=> me ?=>
    if env.streamer.liveStreamApi.isStreaming(me.user.id) && !getBool("force")
    then Forbidden.page(views.site.message.streamingMod)
    else renderList(env.report.modFilters.get(me).fold("all")(_.key))
  }

  def listWithFilter(room: String) = Secure(_.SeeReport) { _ ?=> me ?=>
    env.report.modFilters.set(me, Room(room))
    Room(room).forall(Room.isGranted).so(renderList(room))
  }

  protected[controllers] def getScores: Future[(Scores, PendingCounts)] = (
    api.maxScores,
    env.streamer.api.approval.countRequests,
    env.appeal.api.countUnread,
    env.title.api.countPending
  ).mapN: (scores, streamers, appeals, titles) =>
    (scores, PendingCounts(streamers, appeals, titles))

  private def renderList(room: String)(using Context, Me) =
    api.openAndRecentWithFilter(12, Room(room)).zip(getScores).flatMap { case (reports, (scores, pending)) =>
      env.user.lightUserApi.preloadMany(reports.flatMap(_.report.userIds)) >>
        Ok.page:
          val filteredReports = reports.filter(r => lila.report.Room.isGranted(r.report.room))
          views.report.list(filteredReports, room, scores, pending)
    }

  def inquiry(reportOrAppealId: String) = Secure(_.SeeReport) { _ ?=> me ?=>
    api.inquiries
      .toggle(reportOrAppealId, onlyOpen = getBool("onlyOpen"))
      .flatMap: (prev, next) =>
        prev
          .filter(_.isAppeal)
          .map(_.user)
          .so(env.appeal.api.setUnreadById)
          .inject(
            next.fold(
              Redirect:
                if prev.exists(_.isAppeal)
                then routes.Appeal.queue()
                else routes.Report.list
            )(onInquiryStart)
          )
  }

  private def onInquiryStart(inquiry: ReportModel): Result =
    if inquiry.isRecentComm then Redirect(routes.Mod.communicationPrivate(inquiry.user))
    else if inquiry.is(_.Comm) then Redirect(routes.Mod.communicationPublic(inquiry.user))
    else modC.redirect(inquiry.user)

  protected[controllers] def onModAction(goTo: Suspect)(using ctx: BodyContext[?], me: Me): Fu[Result] =
    if HTTPRequest.isXhr(ctx.req) then userC.renderModZoneActions(goTo.user.username)
    else
      api.inquiries
        .ofModId(me.id)
        .flatMap(_.fold(userC.modZoneOrRedirect(goTo.user.username))(onInquiryAction(_)))

  protected[controllers] def onInquiryAction(
      inquiry: ReportModel,
      processed: Boolean = false
  )(using ctx: BodyContext[?], me: Me): Fu[Result] =
    val dataOpt = ctx.body.body match
      case AnyContentAsFormUrlEncoded(data) => data.some
      case _ => none
    def thenGoTo =
      dataOpt
        .flatMap(_.get("then"))
        .flatMap(_.headOption)
        .flatMap:
          case "profile" => modC.userUrl(inquiry.user, mod = true).some
          case url => url.some
    def process() = (!processed).so(api.process(inquiry))
    thenGoTo match
      case Some(url) => process().inject(Redirect(url))
      case _ =>
        if inquiry.isAppeal then process() >> Redirect(routes.Appeal.queue())
        else if dataOpt.flatMap(_.get("next")).exists(_.headOption contains "1") then
          process() >> {
            if inquiry.isSpontaneous
            then Redirect(modC.userUrl(inquiry.user, mod = true))
            else
              api.inquiries
                .toggleNext(inquiry.room)
                .map:
                  case Some(next) => onInquiryStart(next)
                  case _ => Redirect(routes.Report.listWithFilter(inquiry.room.key))
          }
        else if processed then userC.modZoneOrRedirect(inquiry.user)
        else onInquiryStart(inquiry)

  def process(id: ReportId) = SecureBody(_.SeeReport) { _ ?=> me ?=>
    api
      .byId(id)
      .flatMap:
        _.fold(Redirect(routes.Report.list).toFuccess): inquiry =>
          inquiry.isAppeal.so(env.appeal.api.setReadById(inquiry.user)) >>
            api.process(inquiry) >>
            onInquiryAction(inquiry, processed = true)
  }

  def xfiles(id: ReportId) = SecureBody(_.SeeReport) { _ ?=> _ ?=>
    api
      .byId(id)
      .flatMap:
        _.fold(Redirect(routes.Report.list).toFuccess): inquiry =>
          api.moveToXfiles(id) >> onInquiryAction(inquiry, processed = true)
  }

  def snooze(id: ReportId, dur: String) = SecureBody(_.SeeReport) { _ ?=> _ ?=>
    api
      .snooze(id, dur)
      .map:
        _.fold(Redirect(routes.Report.list))(onInquiryStart)
  }

  def currentCheatInquiry(username: UserStr) = Secure(_.CheatHunter) { _ ?=> me ?=>
    Found(env.user.repo.byId(username)): user =>
      Found(api.currentCheatReport(lila.report.Suspect(user))): report =>
        api.inquiries.toggle(Left(report.id)).inject(NoContent)
  }

  def form = Auth { _ ?=> _ ?=>
    getUserStr("username").so(env.user.repo.byId).flatMap { user =>
      if user.exists(_.is(UserId.lichess)) then Redirect(routes.Main.contact)
      else
        Ok.async:
          val form = env.report.forms.create
          val filledForm: Form[lila.report.ReportSetup] = (user, get("postUrl")) match
            case (Some(u), Some(pid)) =>
              form.fill(lila.report.ReportSetup(u.light, reason = ~get("reason"), text = s"$pid\n\n"))
            case _ => form
          views.report.ui.form(filledForm, user, get("from"))
    }
  }

  private val reportRateLimit =
    env.security.ipTrust.rateLimit(30, 3.hours, "report.create", _.proxyMultiplier(3))

  def create = AuthBody { _ ?=> me ?=>
    bindForm(env.report.forms.create)(
      err =>
        for
          user <- getUserStr("username").so(env.user.repo.byId)
          page <- renderPage(views.report.ui.form(err, user, none))
        yield BadRequest(page),
      data =>
        if me.is(data.user.id) then BadRequest("You cannot report yourself")
        else
          reportRateLimit(rateLimited):
            for
              _ <- api.create(data, Reporter(me), Nil)
              _ <- api.isAutoBlock(data).so(env.relation.api.block(me, data.user.id))
            yield Redirect(routes.Report.thanks).flashing("reported" -> data.user.name.value)
    )
  }

  private def inboxFormPage(user: lila.core.user.User, form: Form[?])(using Context, Me) = for
    msgs <- env.msg.api.msgsToReport(user.id)
    page <- renderPage(views.report.ui.inbox(form, user, msgs))
  yield (msgs, page)

  def inboxForm(username: UserStr) = Auth { _ ?=> _ ?=>
    Found(env.user.repo.byId(username)): user =>
      inboxFormPage(user, env.report.forms.create).map: (msgs, page) =>
        if msgs.nonEmpty then Ok(page)
        else Redirect(s"${routes.Report.form}?username=${user.username}")
  }

  def inboxCreate(username: UserStr) = AuthBody { _ ?=> me ?=>
    Found(env.user.repo.byId(username)): user =>
      bindForm(env.report.forms.create)(
        err => inboxFormPage(user, err).map((_, page) => BadRequest(page)),
        data =>
          for
            msgs <- env.msg.api.msgsToReport(data.user.id, data.msgs.some)
            _ <- api.create(data, Reporter(me), msgs.map(_.text))
            _ <- api.isAutoBlock(data).so(env.relation.api.block(me, data.user.id))
          yield Redirect(routes.Report.thanks).flashing("reported" -> data.user.name.value)
      )
  }

  def flag = AuthBody { _ ?=> me ?=>
    bindForm(env.report.forms.flag)(
      _ => BadRequest,
      data =>
        Found(env.user.repo.byId(data.username)): user =>
          if user == me then BadRequest
          else api.commFlag(Reporter(me), Suspect(user), data.resource, data.text).inject(jsonOkResult)
    )
  }

  def thanks = Auth { ctx ?=> me ?=>
    ctx.req.flash
      .get("reported")
      .flatMap(UserStr.read)
      .fold(Redirect("/").toFuccess): reported =>
        Ok.async:
          env.relation.api
            .fetchBlocks(me, reported.id)
            .map:
              views.report.ui.thanks(reported.id, _)
  }
