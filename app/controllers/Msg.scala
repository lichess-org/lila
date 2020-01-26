package controllers

import play.api.libs.json._

import lila.api.Context
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
    if (username == "new") Redirect(get("user").fold(routes.Msg.home()) { routes.Msg.threadWith(_) }).fuccess
    else renderConvo(me, username)
  }

  private def renderConvo(me: lila.user.User, username: String)(implicit ctx: Context) =
    for {
      convo   <- env.msg.api.convoWith(me, username)
      threads <- jsonThreads(me, convo.thread.some.filter(_.lastMsg.isEmpty))
      json = Json.obj(
        "me"      -> me.light,
        "threads" -> threads,
        "convo"   -> env.msg.json.convo(convo)
      )
      res <- negotiate(
        html = Ok(views.html.msg.home(json)).fuccess,
        api = _ => Ok(json).fuccess
      )
    } yield res

  def search(q: String) = Auth { _ => me =>
    q.trim.some.filter(_.size > 1).filter(lila.user.User.couldBeUsername) match {
      case None    => BadRequest(jsonError("Invalid search query")).fuccess
      case Some(q) => env.msg.search(me, q) flatMap env.msg.json.searchResult(me) map { Ok(_) }
    }
  }

  def unreadCount = Auth { _ => me =>
    JsonOk(env.msg.api unreadCount me)
  }

  def threadDelete(username: String) = Auth { _ => me =>
    env.msg.api.delete(me, username) >>
      jsonThreads(me) map { threads =>
      Ok(
        Json.obj(
          "me"      -> me.light,
          "threads" -> threads
        )
      )
    }
  }

  private def jsonThreads(me: lila.user.User, also: Option[lila.msg.MsgThread] = none) =
    env.msg.api.threadsOf(me) flatMap { threads =>
      val all = also.fold(threads) { thread =>
        if (threads.exists(_.id == thread.id)) threads else thread :: threads
      }
      env.msg.json.threads(me)(all)
    }
}
