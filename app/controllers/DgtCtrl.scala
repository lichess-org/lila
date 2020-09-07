package controllers

import lila.app._

final class DgtCtrl(env: Env) extends LilaController(env) {

  def index =
    Auth { implicit ctx => _ =>
      Ok(views.html.dgt.index).fuccess
    }

  def play =
    Auth { implicit ctx => _ =>
      Ok(views.html.dgt.play).fuccess
    }

  def config =
    Auth { implicit ctx => _ =>
      Ok(views.html.dgt.config).fuccess
    }
}
