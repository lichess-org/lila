package controllers

import lila.app._

import play.api.libs.json._
import views.html

object Learn extends LilaController {

  val env = Env.learn

  import lila.learn.JSONHandlers._

  def index = Auth { implicit ctx =>
    me =>
      env.api.get(me) map { progress =>
        Ok(html.learn.index(me, Json toJson progress))
      }
  }
}
