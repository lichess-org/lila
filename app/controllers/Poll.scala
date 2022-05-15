package controllers

import play.api.mvc._
import lila.api.Context
import play.api.data._
import play.api.data.Forms._
import lila.app._

final class Poll(env: Env) extends LilaController(env) {

  // private def api = env.poll.api
  case class Vote(choice: Int)

  val pollForm = Form(mapping("choice" -> number)(Vote.apply)(Vote.unapply))

  def index =
    Open { implicit ctx =>
      fuccess(BadRequest("Not implemented"))
    }

  def close(pid: String) =
    AuthBody { implicit ctx => me =>
      ctx.me.map(u => env.poll.api.close(pid, u.id))
      fuccess(Ok)
    }

  def vote(pid: String) =
    AuthBody { implicit ctx => me =>
      ctx.me.map(u =>
        env.poll.api.vote(pid, u.id, pollForm.bindFromRequest()(ctx.body, formBinding).get.choice)
      )
      fuccess(Ok)
    }
}
