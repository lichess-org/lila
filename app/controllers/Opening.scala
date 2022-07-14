package controllers

import play.api.mvc._
import views.html

import lila.api.Context
import lila.app._
import lila.common.LilaOpeningFamily

final class Opening(env: Env) extends LilaController(env) {

  def index =
    Open { implicit ctx =>
      Ok(html.opening.index(LilaOpeningFamily.familyList)).fuccess
    }

  def family(key: String) =
    Open { implicit ctx =>
      LilaOpeningFamily.find(key) ?? { family =>
        Ok(html.opening.family(family)).fuccess
      }
    }
}
