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
      env.opening.api.popular map { pop =>
        Ok(html.opening.index(pop))
      }
    }

  def show(key: String) =
    Secure(_.Beta) { implicit ctx => _ =>
      env.opening.api.find(key) flatMap {
        _ ?? { op =>
          env.puzzle.opening.find(op.data.opening.family) map { puzzle =>
            Ok(html.opening.show(op, puzzle))
          }
        }
      }
    }
}
