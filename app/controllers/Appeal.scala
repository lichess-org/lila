package controllers
package appeal

import play.api.data.Form
import play.api.mvc.Result

import lila.app.{ *, given }
import lila.appeal.Appeal as AppealModel
import lila.report.Suspect
import lila.core.misc.AppealTopic

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

  def post(topic: AppealTopic) = AuthBody { ctx ?=> me ?=>
    bindForm(userForm)(
      err => BadRequest.async(renderAppealOrTree(err.some)),
      text => env.appeal.api.post(topic, text).inject(Redirect(routes.Appeal.home).flashSuccess)
    )
  }

  def modQueue(filterStr: Option[String] = None) = Secure(_.Appeals) { ctx ?=> me ?=>
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

  def modHandle(username: UserStr, topic: AppealTopic) = Secure(_.Appeals) { ctx ?=> me ?=>
    Found(env.user.repo.byId(username)): user =>
      Found(env.appeal.api.find(user, topic)): appeal =>
        val appealUrl = routes.Appeal.modShow(username, topic)
        env.report.api.inquiries
          .ongoingAppealOf(user.id)
          .flatMap:
            case Some(ongoing) if ongoing.mod.isnt(me) =>
              for mod <- env.user.lightUserApi.asyncFallback(ongoing.mod)
              yield Redirect(appealUrl).flashFailure(s"Currently processed by ${mod.name}")
            case _ =>
              for _ <- env.report.api.inquiries.appeal(user, topic)
              yield Redirect(s"${appealUrl}#appeal-actions")
  }

  def modShow(username: UserStr, topic: AppealTopic) = Secure(_.Appeals) { ctx ?=> me ?=>
    asMod(username, topic): (appeal, suspect) =>
      getModData(suspect).flatMap: modData =>
        Ok.page(views.appeal.discussion.show(appeal, modForm, modData))
  }

  def modReply(username: UserStr, topic: AppealTopic) = SecureBody(_.Appeals) { ctx ?=> me ?=>
    asMod(username, topic): (appeal, suspect) =>
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
                  .toggle(Right(appeal.user))
                  .inject(Redirect(routes.Appeal.modQueue()))
              else Redirect(s"${routes.Appeal.modShow(username, topic)}#appeal-actions").toFuccess
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

  def mute(username: UserStr, topic: AppealTopic) = Secure(_.Appeals) { _ ?=> _ ?=>
    asMod(username, topic): (appeal, _) =>
      for
        _ <- env.appeal.api.toggleMute(appeal)
        _ <- env.report.api.inquiries.toggle(Right(appeal.user))
      yield Redirect(routes.Appeal.modQueue())
  }

  def sendToZulip(username: UserStr, topic: AppealTopic) = Secure(_.SendToZulip) { _ ?=> _ ?=>
    asMod(username, topic): (_, s) =>
      for _ <- env.irc.api.userAppeal(s.user.light)
      yield NoContent
  }

  def snooze(username: UserStr, topic: AppealTopic, dur: String) = Secure(_.Appeals) { _ ?=> _ ?=>
    asMod(username, topic): (appeal, _) =>
      env.appeal.api.snooze(appeal.id, dur)
      for _ <- env.report.api.inquiries.toggle(Right(appeal.user))
      yield Redirect(routes.Appeal.modQueue())
  }

  private def getPresets = env.mod.presets.appealPresets.get()

  private def asMod(username: UserStr, topic: AppealTopic)(
      f: (AppealModel, Suspect) => Fu[Result]
  )(using Context): Fu[Result] =
    meOrFetch(username)
      .flatMapz: user =>
        env.appeal.api
          .find(user, topic)
          .flatMapz: appeal =>
            f(appeal, Suspect(user)).dmap(some)
      .flatMap(_.so(fuccess))
