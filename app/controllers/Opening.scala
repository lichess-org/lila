package controllers

import play.api.libs.json.Json
import play.api.mvc._
import views.html

import lila.api.Context
import lila.app._
import lila.common.LilaOpeningFamily
import lila.opening.OpeningQuery
import lila.common.HTTPRequest

final class Opening(env: Env) extends LilaController(env) {

  def index(q: Option[String] = None) =
    Open { implicit ctx =>
      val searchQuery = ~q
      if (searchQuery.nonEmpty) {
        val results = env.opening.search(searchQuery)
        Ok {
          if (HTTPRequest isXhr ctx.req) html.opening.search.resultsList(results)
          else html.opening.search.resultsPage(searchQuery, results, env.opening.api.readConfig)
        }.fuccess
      } else
        env.opening.api.index flatMap {
          _ ?? { page =>
            Ok(html.opening.index(page)).fuccess
          }
        }
    }

  def query(q: String) =
    Open { implicit ctx =>
      env.opening.api.lookup(q) flatMap {
        case None                                 => Redirect(routes.Opening.index()).fuccess
        case Some(page) if page.query.key.isEmpty => Redirect(routes.Opening.index()).fuccess
        case Some(page) if page.query.key != q    => Redirect(routes.Opening.query(page.query.key)).fuccess
        case Some(page) =>
          page.query.family.??(f => env.puzzle.opening.find(f)) map { puzzle =>
            Ok(html.opening.show(page, puzzle))
          }
      }
    }

  def config(thenTo: String) =
    OpenBody { implicit ctx =>
      implicit val req = ctx.body
      val redir =
        Redirect {
          if (thenTo.isEmpty || thenTo == "index") routes.Opening.index()
          else if (thenTo startsWith "q:") routes.Opening.index(thenTo.drop(2).some)
          else routes.Opening.query(thenTo)
        }
      lila.opening.OpeningConfig.form
        .bindFromRequest()
        .fold(_ => redir, cfg => redir.withCookies(env.opening.config.write(cfg)))
        .fuccess
    }
}
