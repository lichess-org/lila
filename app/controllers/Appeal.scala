package controllers
package appeal

import play.api.mvc.Result
import views.*

import lila.app.{ given, * }
import lila.appeal.{ Appeal as AppealModel }
import lila.report.{ Suspect, Mod }
import play.api.data.Form

final class Appeal(env: Env, reportC: => report.Report, prismicC: => Prismic, userC: => User)
    extends LilaController(env):

  private def modForm(using Context)  = AppealModel.modForm
  private def userForm(using Context) = AppealModel.form

  def home = Auth { _ ?=> me ?=>
    Ok async renderAppealOrTree()
  }

  def landing = Auth { ctx ?=> _ ?=>
    if ctx.isAppealUser || isGranted(_.Appeals) then
      FoundPage(prismicC getBookmark "appeal-landing"): (doc, resolver) =>
        views.html.site.page.lone(doc, resolver)
    else notFound
  }

  private def renderAppealOrTree(
      err: Option[Form[String]] = None
  )(using Context)(using me: Me): Fu[Frag] = env.appeal.api.byId(me) flatMap {
    case None =>
      renderAsync:
        env.playban.api.currentBan(me).dmap(_.isDefined) map { html.appeal.tree(me, _) }
    case Some(a) => renderPage(html.appeal.discussion(a, me, err | userForm))
  }

  def post = AuthBody { ctx ?=> me ?=>
    userForm
      .bindFromRequest()
      .fold(
        err => renderAppealOrTree(err.some) map { BadRequest(_) },
        text => env.appeal.api.post(text) inject Redirect(routes.Appeal.home).flashSuccess
      )
  }

  def queue(filterStr: Option[String] = None) = Secure(_.Appeals) { ctx ?=> me ?=>
    val filter = env.appeal.api.modFilter.fromQuery(filterStr)
    for
      appeals                          <- env.appeal.api.myQueue(filter)
      inquiries                        <- env.report.api.inquiries.allBySuspect
      ((scores, streamers), nbAppeals) <- reportC.getScores
      _ = env.user.lightUserApi preloadUsers appeals.map(_.user)
      markedByMap <- env.mod.logApi.wereMarkedBy(appeals.map(_.user.id))
      page <- renderPage(
        html.appeal.queue(appeals, inquiries, filter, markedByMap, scores, streamers, nbAppeals)
      )
    yield Ok(page)
  }

  def show(username: UserStr) = Secure(_.Appeals) { ctx ?=> me ?=>
    asMod(username): (appeal, suspect) =>
      getModData(suspect).flatMap: modData =>
        Ok.page(html.appeal.discussion.show(appeal, modForm, modData))
  }

  def reply(username: UserStr) = SecureBody(_.Appeals) { ctx ?=> me ?=>
    asMod(username): (appeal, suspect) =>
      modForm
        .bindFromRequest()
        .fold(
          err =>
            getModData(suspect).flatMap: modData =>
              BadRequest.page(html.appeal.discussion.show(appeal, err, modData)),
          (text, process) =>
            for
              _ <- env.mailer.automaticEmail.onAppealReply(suspect.user)
              preset = getPresets.findLike(text)
              _ <- env.appeal.api.reply(text, appeal, preset.map(_.name))
              _ <- env.mod.logApi.appealPost(suspect.user.id)
              result <-
                if process then
                  env.report.api.inquiries.toggle(Right(appeal.userId)) inject
                    Redirect(routes.Appeal.queue())
                else Redirect(s"${routes.Appeal.show(username.value)}#appeal-actions").toFuccess
            yield result
        )
  }

  private def getModData(suspect: Suspect)(using Context)(using me: Me) =
    for
      users      <- env.security.userLogins(suspect.user, 100)
      logins     <- userC.loginsTableData(suspect.user, users, 100)
      appeals    <- env.appeal.api.byUserIds(suspect.user.id :: logins.userLogins.otherUserIds)
      inquiry    <- env.report.api.inquiries.ofSuspectId(suspect.user.id)
      markedByMe <- env.mod.logApi.wasMarkedBy(suspect.user.id)
    yield html.appeal.discussion.ModData(
      mod = me,
      suspect = suspect,
      presets = getPresets,
      logins = logins,
      appeals = appeals,
      renderIp = env.mod.ipRender.apply,
      inquiry = inquiry.filter(_.mod is me),
      markedByMe = markedByMe
    )

  def mute(username: UserStr) = Secure(_.Appeals) { _ ?=> _ ?=>
    asMod(username): (appeal, _) =>
      env.appeal.api.toggleMute(appeal) >>
        env.report.api.inquiries.toggle(Right(appeal.userId)) inject
        Redirect(routes.Appeal.queue())
  }

  def sendToZulip(username: UserStr) = Secure(_.SendToZulip) { _ ?=> _ ?=>
    asMod(username): (_, suspect) =>
      env.irc.api.userAppeal(suspect.user) inject NoContent
  }

  def snooze(username: UserStr, dur: String) = Secure(_.Appeals) { _ ?=> _ ?=>
    asMod(username): (appeal, _) =>
      env.appeal.api.snooze(appeal.id, dur)
      env.report.api.inquiries.toggle(Right(appeal.userId)) inject
        Redirect(routes.Appeal.queue())
  }

  private def getPresets = env.mod.presets.appealPresets.get()

  private def asMod(
      username: UserStr
  )(f: (AppealModel, Suspect) => Fu[Result])(using Context): Fu[Result] =
    env.user.repo byId username flatMapz { user =>
      env.appeal.api byId user flatMapz { appeal =>
        f(appeal, Suspect(user)) dmap some
      }
    } flatMap {
      _.fold(notFound)(fuccess)
    }
