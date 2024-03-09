package controllers

import lila.app._

import views.html

final class Insights(env: Env) extends LilaController(env) {

  def index =
    Auth { _ => me =>
      fuccess(Redirect(routes.Insights.user(me.username, "")))
    }

  def user(username: String, path: String) =
    Auth { implicit ctx => me =>
      pageHit
      val normalized = lila.user.User.normalize(username)
      val isMe       = normalized == me.id
      OptionFuResult(if (isMe) fuccess(me.some) else env.user.repo named normalized) { user =>
        val shareFu =
          if (isMe || isGranted(_.SeeInsights)) fuccess(true)
          else env.pref.api.getPref(user.id, pref => pref.insightsShare)
        shareFu map { share =>
          if (share) Ok(html.insights(user, path))
          else Ok(html.insights.privated(user))
        }
      }
    }

}
