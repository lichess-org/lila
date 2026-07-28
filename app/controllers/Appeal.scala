package controllers
package appeal

import play.api.data.Form
import play.api.mvc.Result

import lila.app.{ *, given }
import lila.appeal.{ Appeal as AppealModel, AppealTopicApi }
import lila.report.Suspect
import lila.core.misc.AppealTopic

final class Appeal(env: Env, reportC: => report.Report, userC: => User) extends LilaController(env):

  import lila.appeal.AppealForm.{ modForm, form as userForm, sleep as sleepForm }

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
  )(using Context)(using me: Me) = for
    appeals <- env.appeal.api.byTopic(me)
    status <- makeStatus(me)
    topic = AppealTopicApi.select(status, appeals)
    allAppeals = appeals.value.values.toList
  yield topic.flatMap(appeals.get) match
    case Some(a) => views.appeal.discussion.userShow(status, a, err | userForm, allAppeals)
    case None => views.appeal.tree.page(topic, status, appeals)

  private def makeStatus(user: lila.core.user.User) = for
    playban <- env.playban.api.currentBan(user).dmap(_.isDefined)
    blogHidden <- env.ublog.api.isHiddenWithPosts(user)
    modActions <- env.mod.logApi.recentActionsOf(user.id)
  yield lila.appeal.UserStatus(user, playban, blogHidden, modActions)

  def post(topic: AppealTopic) = AuthBody { ctx ?=> me ?=>
    for
      appeals <- env.appeal.api.byTopic(me)
      status <- makeStatus(me)
      res <-
        if AppealTopicApi.select(status, appeals).exists(_ == topic) then
          bindForm(userForm)(
            err => BadRequest.async(renderAppealOrTree(err.some)),
            text =>
              for _ <- env.appeal.api.post(topic, text, muted = appeals.muted)
              yield Redirect(routes.Appeal.home).flashSuccess
          )
        else fuccess(Redirect(routes.Appeal.home).flashFailure("You cannot post an appeal for this topic"))
    yield res
  }

  def modQueue = Secure(_.Appeals) { ctx ?=> me ?=>
    val topic = AppealTopicApi.topicFilter(get("topic"))
    for
      appeals <- env.appeal.api.myQueue(topic)
      inquiries <- env.report.api.inquiries.allBySuspect
      (scores, pending) <- reportC.getScores
      _ <- env.user.lightUserApi.preloadMany(appeals.map(_.user.id))
      markedByMap <- env.mod.logApi.wereMarkedBy(appeals.map(_.user.id))
      page <- renderPage(views.appeal.queue(appeals, inquiries, topic, markedByMap, scores, pending))
    yield Ok(page)
  }

  def modHandle(username: UserStr, topic: AppealTopic) = Secure(_.Appeals) { ctx ?=> me ?=>
    Found(env.user.repo.byId(username)): user =>
      Found(env.appeal.api.find(user, topic)): appeal =>
        env.report.api.inquiries
          .ongoingAppealOf(user.id)
          .flatMap:
            case Some(ongoing) if ongoing.mod.isnt(me) =>
              for mod <- env.user.lightUserApi.asyncFallback(ongoing.mod.userId)
              yield Redirect(appeal.modShowUrl).flashFailure(s"Currently processed by ${mod.name}")
            case _ =>
              for _ <- env.report.api.inquiries.appeal(user, topic)
              yield Redirect(appeal.modShowUrl)
  }

  def modShow(username: UserStr, topic: AppealTopic) = Secure(_.Appeals) { ctx ?=> me ?=>
    asMod(username, topic): (appeal, suspect) =>
      getModData(appeal, suspect).flatMap: modData =>
        Ok.page(views.appeal.discussion.modShow(appeal, modForm, modData))
  }

  def modShowAll(username: UserStr) = Secure(_.Appeals) { ctx ?=> me ?=>
    Found(meOrFetch(username)): user =>
      for
        appeals <- env.appeal.api.findAll(user)
        page <- Ok.page(views.appeal.ui.list(user, appeals))
      yield page
  }

  def modReply(username: UserStr, topic: AppealTopic) = SecureBody(_.Appeals) { ctx ?=> me ?=>
    asMod(username, topic): (appeal, suspect) =>
      bindForm(modForm)(
        err =>
          getModData(appeal, suspect).flatMap: modData =>
            BadRequest.page(views.appeal.discussion.modShow(appeal, err, modData)),
        (text, close, dismiss) =>
          for
            replied <- env.appeal.api.modReply(text, appeal)
            _ <- close.orZero.so(env.appeal.api.toggleClosed(replied, true, sleepMonths = 0))
            _ <- dismiss.orZero.so(env.report.api.inquiries.toggle(Right(appeal.user)).void)
            _ <- env.mailer.automaticEmail.onAppealReply(suspect.user)
          yield
            if dismiss.orZero then Redirect(routes.Appeal.modQueue)
            else Redirect(appeal.modShowUrl).flashSuccess("Reply sent")
      )
  }

  private def getModData(appeal: AppealModel, suspect: Suspect)(using Context)(using me: Me) =
    for
      status <- makeStatus(suspect.user)
      users <- env.security.userLogins(suspect.user, 100)
      logins <- userC.loginsTableData(suspect.user, users, 100)
      relatedAppeals <- env.appeal.api.byUserIds(suspect.user.id :: logins.userLogins.otherUserIds)
      inquiry <- env.report.api.inquiries.ofSuspectId(suspect.user.id)
      markedByMe <- env.mod.logApi.wasMarkedBy(suspect.user.id)
      given lila.mod.IpRender.RenderIp = env.mod.ipRender.apply
    yield lila.appeal.ui.ModData(
      mod = me,
      status = status,
      presets = env.mod.presets.asPairsFor(appeal.topic),
      relatedAppeals = relatedAppeals,
      inquiryBy = inquiry.map(_.mod),
      markedByMe = markedByMe,
      otherUsers = views.user.mod.otherUsers(suspect.user, logins, relatedAppeals, readOnly = true)
    )

  def toggleClosed(username: UserStr, topic: AppealTopic, v: Boolean) = SecureBody(_.Appeals) { _ ?=> _ ?=>
    asMod(username, topic): (appeal, _) =>
      val sleepMonths = bindForm(sleepForm)(_ => 0, _.orZero)
      for
        _ <- env.appeal.api.toggleClosed(appeal, v, sleepMonths = sleepMonths)
        _ <- v.so(env.report.api.inquiries.toggle(Right(appeal.user)).void)
      yield Redirect(if v then routes.Appeal.modQueue.url else appeal.modShowUrl)
  }

  def toggleMute(username: UserStr, topic: AppealTopic, v: Boolean) = SecureBody(_.Appeals) { _ ?=> _ ?=>
    asMod(username, topic): (appeal, _) =>
      for _ <- env.appeal.api.toggleMute(appeal.user, v)
      yield Redirect(appeal.modShowUrl)
  }

  def toggleRead(username: UserStr, topic: AppealTopic, v: Boolean) = SecureBody(_.Appeals) { _ ?=> _ ?=>
    asMod(username, topic): (appeal, _) =>
      for
        _ <- env.appeal.api.toggleRead(appeal, v)
        _ <- env.report.api.inquiries.toggle(Right(appeal.user)).void
      yield Redirect(routes.Appeal.modQueue)
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
      yield Redirect(routes.Appeal.modQueue)
  }

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
