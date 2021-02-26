package controllers

import play.api.mvc.Result
import views._

import lila.api.Context
import lila.app._
import lila.report.Suspect

final class Appeal(env: Env, reportC: => Report) extends LilaController(env) {

  def home =
    Auth { implicit ctx => me =>
      env.appeal.api.mine(me) map { appeal =>
        Ok(html.appeal.discussion(appeal, env.appeal.forms.text))
      }
    }

  def post =
    AuthBody { implicit ctx => me =>
      implicit val req = ctx.body
      env.appeal.forms.text
        .bindFromRequest()
        .fold(
          err =>
            env.appeal.api.mine(me) map { appeal =>
              BadRequest(html.appeal.discussion(appeal, err))
            },
          text => env.appeal.api.post(text, me) inject Redirect(routes.Appeal.home).flashSuccess
        )
    }

  def queue =
    Secure(_.Appeals) { implicit ctx => me =>
      env.appeal.api.queue zip env.report.api.inquiries.allBySuspect zip reportC.getScores flatMap {
        case ((appeals, inquiries), scores ~ streamers ~ nbAppeals) =>
          (env.user.lightUserApi preloadMany appeals.map(_.id)) inject
            Ok(html.appeal.queue(appeals, inquiries, scores, streamers, nbAppeals))
      }
    }

  def show(username: String) =
    Secure(_.Appeals) { implicit ctx => me =>
      asMod(username) { (appeal, suspect) =>
        env.report.api.inquiries.ofSuspectId(suspect.user.id) map { inquiry =>
          Ok(html.appeal.discussion.show(appeal, suspect, inquiry, env.appeal.forms.text, getPresets))
        }
      }
    }

  def reply(username: String) =
    SecureBody(_.Appeals) { implicit ctx => me =>
      asMod(username) { (appeal, suspect) =>
        implicit val req = ctx.body
        env.appeal.forms.text
          .bindFromRequest()
          .fold(
            err =>
              env.report.api.inquiries.ofSuspectId(suspect.user.id) map { inquiry =>
                BadRequest(html.appeal.discussion.show(appeal, suspect, inquiry, err, getPresets))
              },
            text =>
              for {
                _ <- env.appeal.api.reply(text, appeal, me)
                _ <- env.security.automaticEmail.onAppealReply(suspect.user)
                _ <- env.mod.logApi.appealPost(me.id, suspect.user.id)
              } yield Redirect(routes.Appeal.show(username)).flashSuccess
          )
      }
    }

  def mute(username: String) =
    Secure(_.Appeals) { implicit ctx => me =>
      asMod(username) { (appeal, suspect) =>
        env.appeal.api.toggleMute(appeal) >>
          env.report.api.inquiries.toggle(lila.report.Mod(me), appeal.id) inject
          Redirect(routes.Appeal.queue)
      }
    }

  def notifySlack(username: String) =
    Secure(_.NotifySlack) { implicit ctx => me =>
      asMod(username) { (appeal, suspect) =>
        env.irc.slack.userAppeal(user = suspect.user, mod = me) inject NoContent
      }
    }

  private def getPresets = env.mod.presets.appealPresets.get()

  private def asMod(
      username: String
  )(f: (lila.appeal.Appeal, Suspect) => Fu[Result])(implicit ctx: Context): Fu[Result] =
    env.user.repo named username flatMap {
      _ ?? { user =>
        env.appeal.api get user flatMap {
          _ ?? { appeal =>
            f(appeal, Suspect(user)) dmap some
          }
        }
      }
    } flatMap {
      _.fold(notFound)(fuccess)
    }
}
