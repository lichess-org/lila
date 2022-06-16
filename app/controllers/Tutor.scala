package controllers

import scala.concurrent.duration._
import views._

import lila.api.Context
import lila.app._

final class Tutor(env: Env) extends LilaController(env) {

  def index = Auth { implicit ctx => me =>
    env.tutor.builder(me, ctx.ip) map { report =>
      Ok(views.html.tutor.home(report))
    }
  }
}
