package controllers

import play.api.libs.json._

import lila.app._
import lila.common.LightUser
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
    val userId = lila.user.User.normalize(username)
    for {
      contact <- env.user lightUser userId map (_ | LightUser.fallback(username))
      convo   <- env.msg.api.convoWith(me, contact)
      threads <- jsonThreads(me, convo.thread.some.filter(_.lastMsg.isEmpty))
      json = Json.obj(
        "me"      -> me.light,
        "threads" -> threads,
        "convo"   -> env.msg.json.convoWith(contact)(convo)
      )
      res <- negotiate(
        html = Ok(views.html.msg.home(json)).fuccess,
        api = _ => Ok(json).fuccess
      )
    } yield res
  }

  def search(q: String) = Auth { _ => me =>
    q.trim.some.filter(_.size > 1).filter(lila.user.User.couldBeUsername) match {
      case None    => BadRequest(jsonError("Invalid search query")).fuccess
      case Some(q) => env.msg.search(me, q) flatMap env.msg.json.searchResult(me) map { Ok(_) }
    }
  }

  def unreadCount = Auth { _ => me =>
    JsonOk(env.msg.api unreadCount me)
  }

  private def jsonThreads(me: lila.user.User, also: Option[lila.msg.MsgThread] = none) =
    env.msg.api.threads(me) flatMap { threads =>
      val all = also.fold(threads) { thread =>
        if (threads.exists(_.id == thread.id)) threads else thread :: threads
      }
      env.msg.json.threads(me)(all)
    }
}
