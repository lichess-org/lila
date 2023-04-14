package controllers
package appeal

import play.api.mvc.Result
import views.*

import lila.api.Context
import lila.app.{ given, * }
import lila.report.Suspect
import play.api.data.Form

final class Appeal(env: Env, reportC: => report.Report, prismicC: => Prismic, userC: => User)
    extends LilaController(env):

  private def form(implicit ctx: Context) =
    if (isGranted(_.Appeals)) lila.appeal.Appeal.modForm
    else lila.appeal.Appeal.form

  def home =
    Auth { implicit ctx => me =>
      renderAppealOrTree(me) map { Ok(_) }
    }

  def landing =
    Auth { implicit ctx => _ =>
      if (ctx.isAppealUser || isGranted(_.Appeals))
        pageHit
        OptionOk(prismicC getBookmark "appeal-landing") { case (doc, resolver) =>
          views.html.site.page.lone(doc, resolver)
        }
      else notFound
    }

  private def renderAppealOrTree(
      me: lila.user.User,
      err: Option[Form[String]] = None
  )(implicit ctx: Context) = env.appeal.api mine me flatMap {
    case None =>
      env.playban.api.currentBan(me.id).dmap(_.isDefined) map {
        html.appeal.tree(me, _)
      }
    case Some(a) => fuccess(html.appeal.discussion(a, me, err | form))
  }

  def post =
    AuthBody { implicit ctx => me =>
      given play.api.mvc.Request[?] = ctx.body
      form
        .bindFromRequest()
        .fold(
          err => renderAppealOrTree(me, err.some) map { BadRequest(_) },
          text => env.appeal.api.post(text, me) inject Redirect(routes.Appeal.home).flashSuccess
        )
    }

  def queue =
    Secure(_.Appeals) { implicit ctx => me =>
      env.appeal.api.queueOf(
        me.user
      ) zip env.report.api.inquiries.allBySuspect zip reportC.getScores flatMap {
        case ((appeals, inquiries), ((scores, streamers), nbAppeals)) =>
          env.user.lightUserApi preloadUsers appeals.map(_.user)
          env.mod.logApi.wereMarkedBy(me.id into ModId, appeals.map(_.user.id)) map { markedByMap =>
            Ok(html.appeal.queue(appeals, inquiries, markedByMap, scores, streamers, nbAppeals))
          }
      }
    }

  def show(username: UserStr) =
    Secure(_.Appeals) { implicit ctx => me =>
      asMod(username) { (appeal, suspect) =>
        getModData(me, suspect) map { modData =>
          Ok(html.appeal.discussion.show(appeal, form, modData))
        }
      }
    }

  def reply(username: UserStr) =
    SecureBody(_.Appeals) { implicit ctx => me =>
      asMod(username) { (appeal, suspect) =>
        given play.api.mvc.Request[?] = ctx.body
        form
          .bindFromRequest()
          .fold(
            err =>
              getModData(me, suspect) map { modData =>
                BadRequest(html.appeal.discussion.show(appeal, err, modData))
              },
            text =>
              for {
                _ <- env.mailer.automaticEmail.onAppealReply(suspect.user)
                preset = getPresets.findLike(text)
                _ <- env.appeal.api.reply(text, appeal, me, preset.map(_.name))
                _ <- env.mod.logApi.appealPost(me.id into ModId, suspect.user.id)
              } yield Redirect(s"${routes.Appeal.show(username.value)}#appeal-actions")
          )
      }
    }

  private def getModData(me: lila.user.Holder, suspect: Suspect)(using Context) =
    for
      users      <- env.security.userLogins(suspect.user, 100)
      logins     <- userC.loginsTableData(suspect.user, users, 100)
      appeals    <- env.appeal.api.byUserIds(suspect.user.id :: logins.userLogins.otherUserIds)
      inquiry    <- env.report.api.inquiries.ofSuspectId(suspect.user.id)
      markedByMe <- env.mod.logApi.wasMarkedBy(me.id into ModId, suspect.user.id)
    yield html.appeal.discussion.ModData(
      mod = me,
      suspect = suspect,
      presets = getPresets,
      logins = logins,
      appeals = appeals,
      renderIp = env.mod.ipRender(me),
      inquiry = inquiry.filter(_.mod == me.user.id),
      markedByMe = markedByMe
    )

  def mute(username: UserStr) =
    Secure(_.Appeals) { implicit ctx => me =>
      asMod(username) { (appeal, _) =>
        env.appeal.api.toggleMute(appeal) >>
          env.report.api.inquiries.toggle(lila.report.Mod(me.user), Right(appeal.userId)) inject
          Redirect(routes.Appeal.queue)
      }
    }

  def sendToZulip(username: UserStr) =
    Secure(_.SendToZulip) { implicit ctx => me =>
      asMod(username) { (_, suspect) =>
        env.irc.api.userAppeal(user = suspect.user, mod = me) inject NoContent
      }
    }

  def snooze(username: UserStr, dur: String) =
    Secure(_.Appeals) { implicit ctx => me =>
      asMod(username) { (appeal, _) =>
        env.appeal.api.snooze(me.user, appeal.id, dur)
        env.report.api.inquiries.toggle(lila.report.Mod(me.user), Right(appeal.userId)) inject
          Redirect(routes.Appeal.queue)
      }
    }

  private def getPresets = env.mod.presets.appealPresets.get()

  private def asMod(
      username: UserStr
  )(f: (lila.appeal.Appeal, Suspect) => Fu[Result])(using Context): Fu[Result] =
    env.user.repo byId username flatMapz { user =>
      env.appeal.api get user flatMapz { appeal =>
        f(appeal, Suspect(user)) dmap some
      }
    } flatMap {
      _.fold(notFound)(fuccess)
    }
