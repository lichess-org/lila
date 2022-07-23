package controllers

import play.api.mvc._
import views.html

import lila.api.Context
import lila.app._
import lila.common.LilaOpeningFamily

final class Opening(env: Env) extends LilaController(env) {

  def index =
    Secure(_.Beta) { implicit ctx => _ =>
      Ok(html.opening.index(LilaOpeningFamily.familyList)).fuccess
    }

  def family(key: String) =
    Secure(_.Beta) { implicit ctx => _ =>
      LilaOpeningFamily.find(key) ?? { family =>
        env.puzzle.opening.find(family) map { puzzle =>
          Ok(html.opening.family(family, puzzle))
        }
      }
    }
}
