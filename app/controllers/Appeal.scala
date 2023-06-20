package controllers
package appeal

import play.api.mvc.Result
import views.*

import lila.app.{ given, * }
import lila.report.{ Suspect, Mod }
import play.api.data.Form

final class Appeal(env: Env, reportC: => report.Report, prismicC: => Prismic, userC: => User)
    extends LilaController(env):

  private def modForm(using WebContext)  = lila.appeal.Appeal.modForm
  private def userForm(using WebContext) = lila.appeal.Appeal.form

  def home = Auth { _ ?=> me ?=>
    renderAppealOrTree(me) map { Ok(_) }
  }

  def landing = Auth { ctx ?=> _ ?=>
    if ctx.isAppealUser || isGranted(_.Appeals) then
      pageHit
      OptionOk(prismicC getBookmark "appeal-landing"): (doc, resolver) =>
        views.html.site.page.lone(doc, resolver)
    else notFound
  }

  private def renderAppealOrTree(
      me: lila.user.User,
      err: Option[Form[String]] = None
  )(using WebContext) = env.appeal.api mine me flatMap {
    case None =>
      env.playban.api.currentBan(me.id).dmap(_.isDefined) map {
        html.appeal.tree(me, _)
      }
    case Some(a) => fuccess(html.appeal.discussion(a, me, err | userForm))
  }

  def post = AuthBody { ctx ?=> me ?=>
    userForm
      .bindFromRequest()
      .fold(
        err => renderAppealOrTree(me, err.some) map { BadRequest(_) },
        text => env.appeal.api.post(text, me) inject Redirect(routes.Appeal.home).flashSuccess
      )
  }

  def queue = Secure(_.Appeals) { ctx ?=> me ?=>
    env.appeal.api.queueOf(me.user) zip
      env.report.api.inquiries.allBySuspect zip reportC.getScores flatMap {
        case ((appeals, inquiries), ((scores, streamers), nbAppeals)) =>
          env.user.lightUserApi preloadUsers appeals.map(_.user)
          env.mod.logApi.wereMarkedBy(appeals.map(_.user.id)) map { markedByMap =>
            Ok(html.appeal.queue(appeals, inquiries, markedByMap, scores, streamers, nbAppeals))
          }
      }
  }

  def show(username: UserStr) = Secure(_.Appeals) { ctx ?=> me ?=>
    asMod(username): (appeal, suspect) =>
      getModData(suspect).map: modData =>
        Ok(html.appeal.discussion.show(appeal, modForm, modData))
  }

  def reply(username: UserStr) = SecureBody(_.Appeals) { ctx ?=> me ?=>
    asMod(username): (appeal, suspect) =>
      modForm
        .bindFromRequest()
        .fold(
          err =>
            getModData(suspect).map: modData =>
              BadRequest(html.appeal.discussion.show(appeal, err, modData)),
          { case (text, process) =>
            for {
              _ <- env.mailer.automaticEmail.onAppealReply(suspect.user)
              preset = getPresets.findLike(text)
              _ <- env.appeal.api.reply(text, appeal, preset.map(_.name))
              _ <- env.mod.logApi.appealPost(suspect.user.id)
              result <-
                if (process) {
                  env.report.api.inquiries.toggle(Mod(me.user), Right(appeal.userId)) inject
                    Redirect(routes.Appeal.queue)
                } else {
                  fuccess(Redirect(s"${routes.Appeal.show(username.value)}#appeal-actions"))
                }
            } yield result
          }
        )
  }

  private def getModData(suspect: Suspect)(using WebContext)(using me: Me) =
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

  def mute(username: UserStr) = Secure(_.Appeals) { ctx ?=> me ?=>
    asMod(username): (appeal, _) =>
      env.appeal.api.toggleMute(appeal) >>
        env.report.api.inquiries.toggle(Mod(me.user), Right(appeal.userId)) inject
        Redirect(routes.Appeal.queue)
  }

  def sendToZulip(username: UserStr) = Secure(_.SendToZulip) { ctx ?=> _ ?=>
    asMod(username): (_, suspect) =>
      env.irc.api.userAppeal(suspect.user) inject NoContent
  }

  def snooze(username: UserStr, dur: String) = Secure(_.Appeals) { ctx ?=> me ?=>
    asMod(username): (appeal, _) =>
      env.appeal.api.snooze(me.user, appeal.id, dur)
      env.report.api.inquiries.toggle(Mod(me.user), Right(appeal.userId)) inject
        Redirect(routes.Appeal.queue)
  }

  private def getPresets = env.mod.presets.appealPresets.get()

  private def asMod(
      username: UserStr
  )(f: (lila.appeal.Appeal, Suspect) => Fu[Result])(using WebContext): Fu[Result] =
    env.user.repo byId username flatMapz { user =>
      env.appeal.api get user flatMapz { appeal =>
        f(appeal, Suspect(user)) dmap some
      }
    } flatMap {
      _.fold(notFound)(fuccess)
    }
