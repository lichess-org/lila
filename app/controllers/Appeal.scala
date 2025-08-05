package controllers
package appeal

import play.api.data.Form
import play.api.mvc.Result

import lila.app.{ *, given }
import lila.appeal.Appeal as AppealModel
import lila.report.Suspect

final class Appeal(env: Env, reportC: => report.Report, userC: => User) extends LilaController(env):

  private def modForm = AppealModel.modForm
  private def userForm = AppealModel.form

  def home = Auth { _ ?=> me ?=>
    Ok.async(renderAppealOrTree()).map(_.hasPersonalData)
  }

  def landing = Auth { ctx ?=> _ ?=>
    if ctx.isAppealUser || isGranted(_.Appeals) then
      FoundPage(env.cms.renderKey("appeal-landing")):
        views.cms.lone
      .map(_.hasPersonalData)
    else notFound
  }

  def closedByTeacher = Auth { ctx ?=> _ ?=>
    if ctx.isAppealUser || isGranted(_.Appeals) then
      FoundPage(env.cms.renderKey("account-closed-by-teacher")):
        views.cms.lone
    else notFound
  }

  private def renderAppealOrTree(
      err: Option[Form[String]] = None
  )(using Context)(using me: Me) = env.appeal.api
    .byId(me)
    .flatMap:
      case None =>
        for
          playban <- env.playban.api.currentBan(me).dmap(_.isDefined)
          // if no blog, consider it's visible because even if it is not,
          // for now the user has not been negatively impacted
          ublogIsVisible <- env.ublog.api.getUserBlogOption(me).dmap(_.forall(_.visible))
        yield views.appeal.tree.page(me, playban, ublogIsVisible)
      case Some(a) => views.appeal.discussion(a, me, err | userForm)

  def post = AuthBody { ctx ?=> me ?=>
    bindForm(userForm)(
      err => BadRequest.async(renderAppealOrTree(err.some)),
      text => env.appeal.api.post(text).inject(Redirect(routes.Appeal.home).flashSuccess)
    )
  }

  def queue(filterStr: Option[String] = None) = Secure(_.Appeals) { ctx ?=> me ?=>
    val filter = env.appeal.api.modFilter.fromQuery(filterStr)
    for
      appeals <- env.appeal.api.myQueue(filter)
      inquiries <- env.report.api.inquiries.allBySuspect
      (scores, pending) <- reportC.getScores
      _ <- env.user.lightUserApi.preloadMany(appeals.map(_.user.id))
      markedByMap <- env.mod.logApi.wereMarkedBy(appeals.map(_.user.id))
      page <- renderPage(views.appeal.queue(appeals, inquiries, filter, markedByMap, scores, pending))
    yield Ok(page)
  }

  def show(username: UserStr) = Secure(_.Appeals) { ctx ?=> me ?=>
    asMod(username): (appeal, suspect) =>
      getModData(suspect).flatMap: modData =>
        Ok.page(views.appeal.discussion.show(appeal, modForm, modData))
  }

  def reply(username: UserStr) = SecureBody(_.Appeals) { ctx ?=> me ?=>
    asMod(username): (appeal, suspect) =>
      bindForm(modForm)(
        err =>
          getModData(suspect).flatMap: modData =>
            BadRequest.page(views.appeal.discussion.show(appeal, err, modData)),
        (text, process) =>
          for
            _ <- env.mailer.automaticEmail.onAppealReply(suspect.user)
            _ <- env.appeal.api.reply(text, appeal)
            result <-
              if process then
                env.report.api.inquiries
                  .toggle(Right(appeal.userId))
                  .inject(Redirect(routes.Appeal.queue()))
              else Redirect(s"${routes.Appeal.show(username)}#appeal-actions").toFuccess
          yield result
      )
  }

  private def getModData(suspect: Suspect)(using Context)(using me: Me) =
    for
      users <- env.security.userLogins(suspect.user, 100)
      logins <- userC.loginsTableData(suspect.user, users, 100)
      appeals <- env.appeal.api.byUserIds(suspect.user.id :: logins.userLogins.otherUserIds)
      inquiry <- env.report.api.inquiries.ofSuspectId(suspect.user.id)
      markedByMe <- env.mod.logApi.wasMarkedBy(suspect.user.id)
    yield views.appeal.discussion.ModData(
      mod = me,
      suspect = suspect,
      presets = getPresets,
      logins = logins,
      appeals = appeals,
      renderIp = env.mod.ipRender.apply,
      inquiry = inquiry.filter(_.mod.is(me)),
      markedByMe = markedByMe
    )

  def mute(username: UserStr) = Secure(_.Appeals) { _ ?=> _ ?=>
    asMod(username): (appeal, _) =>
      for
        _ <- env.appeal.api.toggleMute(appeal)
        _ <- env.report.api.inquiries.toggle(Right(appeal.userId))
      yield Redirect(routes.Appeal.queue())
  }

  def sendToZulip(username: UserStr) = Secure(_.SendToZulip) { _ ?=> _ ?=>
    asMod(username): (_, s) =>
      for _ <- env.irc.api.userAppeal(s.user.light)
      yield NoContent
  }

  def snooze(username: UserStr, dur: String) = Secure(_.Appeals) { _ ?=> _ ?=>
    asMod(username): (appeal, _) =>
      env.appeal.api.snooze(appeal.id, dur)
      env.report.api.inquiries.toggle(Right(appeal.userId)).inject(Redirect(routes.Appeal.queue()))
  }

  private def getPresets = env.mod.presets.appealPresets.get()

  private def asMod(
      username: UserStr
  )(f: (AppealModel, Suspect) => Fu[Result])(using Context): Fu[Result] =
    meOrFetch(username)
      .flatMapz: user =>
        env.appeal.api
          .byId(user)
          .flatMapz: appeal =>
            f(appeal, Suspect(user)).dmap(some)
      .flatMap(_.so(fuccess))
