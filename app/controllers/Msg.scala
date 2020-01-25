package controllers

import play.api.libs.json._

import lila.app._
import lila.common.LightUser.lightUserWrites

final class Msg(
    env: Env
) extends LilaController(env) {

  def home = Auth { implicit ctx => me =>
    jsonThreads(me) flatMap { threads =>
      val json = Json.obj(
        "me"      -> me.light,
        "threads" -> threads
      )
      negotiate(
        html = Ok(views.html.msg.home(json)).fuccess,
        api = _ => Ok(json).fuccess
      )
    }
  }

  def threadWith(username: String) = Auth { implicit ctx => me =>
    env.user.repo named username flatMap {
      _ ?? { contact =>
        env.msg.api.convoWith(me, contact) map env.msg.json.convoWith(contact) flatMap { convo =>
          jsonThreads(me) flatMap { threads =>
            val json =
              Json.obj(
                "me"      -> me.light,
                "threads" -> threads,
                "convo"   -> convo
              )
            negotiate(
              html = Ok(views.html.msg.home(json)).fuccess,
              api = _ => Ok(json).fuccess
            )
          }
        }
      }
    }
  }

  private def jsonThreads(me: lila.user.User) =
    env.msg.api.threads(me) flatMap env.msg.json.threads(me)
}
