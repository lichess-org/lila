package controllers

import play.api.libs.json.Json
import play.api.mvc._
import views.html

import lila.api.Context
import lila.app._
import lila.common.LilaOpeningFamily
import lila.opening.OpeningQuery

final class Opening(env: Env) extends LilaController(env) {

  def index = ???
  // Open { implicit ctx =>
  //   env.opening.api.getPopular map { pop =>
  //     Ok(html.opening.index(pop))
  //   }
  // }

  def query(q: String) =
    Secure(_.Beta) { implicit ctx => _ =>
      env.opening.api.lookup(q) map {
        _ ?? { page =>
          Ok(html.opening.show(page))
        }
      }
    }
}
