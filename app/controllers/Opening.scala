package controllers

import play.api.libs.json.Json
import play.api.mvc._
import views.html

import lila.api.Context
import lila.app._
import lila.common.LilaOpeningFamily

final class Opening(env: Env) extends LilaController(env) {

  def index =
    Open { implicit ctx =>
      env.opening.api.getPopular map { pop =>
        Ok(html.opening.index(pop))
      }
    }

  def show(key: String) =
    Secure(_.Beta) { implicit ctx => _ =>
      env.opening.api.find(key) flatMap {
        _ ?? { op =>
          val actualKey = op.data.opening.familyKeyOrKey.value
          if (actualKey == key)
            env.puzzle.opening.find(op.data.opening.family) map { puzzle =>
              Ok(html.opening.show(op, puzzle))
            }
          else fuccess(Redirect(routes.Opening.show(actualKey)))
        }
      }
    }
}
