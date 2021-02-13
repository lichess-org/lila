package controllers

import lila.api.Context
import lila.app._
import lila.report.Suspect
import play.api.mvc.Result
import views._

final class Appeal(env: Env, reportC: => Report) extends LilaController(env) {

  def home =
    Auth { implicit ctx => me =>
      env.appeal.api.mine(me) map { appeal =>
        Ok(html.appeal.home(appeal, env.appeal.forms.text))
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
              BadRequest(html.appeal.home(appeal, err))
            },
          text => env.appeal.api.post(text, me) inject Redirect(routes.Appeal.home()).flashSuccess
        )
    }

  def queue =
    Secure(_.Appeals) { implicit ctx => me =>
      env.appeal.api.queue zip env.report.api.inquiries.allBySuspect zip reportC.getCounts flatMap {
        case ((appeals, inquiries), counts ~ streamers ~ nbAppeals) =>
          (env.user.lightUserApi preloadMany appeals.map(_.id)) inject
            Ok(html.appeal.queue(appeals, inquiries, counts, streamers, nbAppeals))
      }
    }

  def show(username: String) =
    Secure(_.Appeals) { implicit ctx => me =>
      asMod(username) { (appeal, suspect) =>
        env.report.api.inquiries.ofSuspectId(suspect.user.id) map { inquiry =>
          Ok(html.appeal.show(appeal, suspect, inquiry, env.appeal.forms.text, getPresets))
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
                BadRequest(html.appeal.show(appeal, suspect, inquiry, err, getPresets))
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

  def act(username: String, action: String) =
    Secure(_.Appeals) { implicit ctx => me =>
      asMod(username) { (appeal, suspect) =>
        val res = action match {
          case "close" =>
            for {
              _ <- env.appeal.api.close(appeal)
              _ <- env.mod.logApi.appealClose(me.id, suspect.user.id)
              _ <- env.report.api.inquiries.toggle(lila.report.Mod(me), appeal.id)
            } yield none
          case "open" =>
            env.appeal.api.open(appeal) inject Redirect(routes.Appeal.show(username)).flashSuccess.some
          case "mute" =>
            for {
              _ <- env.appeal.api.mute(appeal)
              _ <- env.report.api.inquiries.toggle(lila.report.Mod(me), appeal.id)
            } yield none
          case _ => funit inject none
        }
        res map {
          _ | Redirect(routes.Appeal.queue())
        }
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
