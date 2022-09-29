package controllers

import play.api.libs.json.Json
import play.api.mvc._
import views.html

import lila.api.Context
import lila.app._
import lila.common.LilaOpeningFamily
import lila.opening.OpeningQuery

final class Opening(env: Env) extends LilaController(env) {

  def index =
    Secure(_.Beta) { implicit ctx => _ =>
      env.opening.api.index flatMap {
        _ ?? { page =>
          Ok(html.opening.index(page)).fuccess
        }
      }
    }

  def query(q: String) =
    Secure(_.Beta) { implicit ctx => _ =>
      env.opening.api.lookup(q) flatMap {
        case None                                 => Redirect(routes.Opening.index).fuccess
        case Some(page) if page.query.key.isEmpty => Redirect(routes.Opening.index).fuccess
        case Some(page) if page.query.key != q    => Redirect(routes.Opening.query(page.query.key)).fuccess
        case Some(page) =>
          page.query.family.??(f => env.puzzle.opening.find(f)) map { puzzle =>
            Ok(html.opening.show(page, puzzle))
          }
      }
    }

  def config(q: String) =
    SecureBody(_.Beta) { implicit ctx => _ =>
      implicit val req = ctx.body
      val redir = Redirect(if (q.isEmpty || q == "index") routes.Opening.index else routes.Opening.query(q))
      lila.opening.OpeningConfig.form
        .bindFromRequest()
        .fold(_ => redir, cfg => redir.withCookies(env.opening.config.write(cfg)))
        .fuccess
    }
}
