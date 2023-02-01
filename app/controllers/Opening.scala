package controllers

import play.api.libs.json.Json
import play.api.mvc.*
import views.html

import lila.api.Context
import lila.app.{ given, * }
import lila.common.{ HTTPRequest, LilaOpeningFamily }
import lila.opening.OpeningQuery.queryFromUrl

final class Opening(env: Env) extends LilaController(env):

  def index(q: Option[String] = None) =
    Open { implicit ctx =>
      val searchQuery = ~q
      if (searchQuery.nonEmpty)
        val results = env.opening.search(searchQuery)
        Ok {
          if (HTTPRequest isXhr ctx.req) html.opening.search.resultsList(results)
          else html.opening.search.resultsPage(searchQuery, results, env.opening.api.readConfig)
        }.toFuccess
      else
        env.opening.api.index flatMapz { page =>
          isGranted(_.OpeningWiki).??(env.opening.wiki.popularOpeningsWithShortWiki) map { wikiMissing =>
            Ok(html.opening.index(page, wikiMissing))
          }
        }
    }

  def byKeyAndMoves(key: String, moves: String) =
    Open { implicit ctx =>
      env.opening.api.lookup(queryFromUrl(key, moves.some), isGranted(_.OpeningWiki)) flatMap {
        case None => Redirect(routes.Opening.index(key.some)).toFuccess
        case Some(page) =>
          val query = page.query.query
          if (query.key.isEmpty) Redirect(routes.Opening.index(key.some)).toFuccess
          else if (query.key != key)
            Redirect(routes.Opening.byKeyAndMoves(query.key, moves)).toFuccess
          else if (moves.nonEmpty && query.moves.??(_.value) != moves)
            Redirect(routes.Opening.byKeyAndMoves(query.key, query.moves.??(_.value))).toFuccess
          else
            page.query.opening.??(env.puzzle.opening.getClosestTo) map { puzzle =>
              val puzzleKey = puzzle.map(_.fold(_.family.key.value, _.opening.key.value))
              Ok(html.opening.show(page, puzzleKey))
            }
      }
    }

  def config(thenTo: String) =
    OpenBody { implicit ctx =>
      NoCrawlers {
        given play.api.mvc.Request[?] = ctx.body
        val redir =
          Redirect {
            lila.common.HTTPRequest.referer(ctx.req) | {
              if (thenTo.isEmpty || thenTo == "index") routes.Opening.index().url
              else if (thenTo startsWith "q:") routes.Opening.index(thenTo.drop(2).some).url
              else routes.Opening.byKeyAndMoves(thenTo, "").url
            }
          }
        lila.opening.OpeningConfig.form
          .bindFromRequest()
          .fold(_ => redir, cfg => redir.withCookies(env.opening.config.write(cfg)))
          .toFuccess
      }
    }

  def wikiWrite(key: String, moves: String) = SecureBody(_.OpeningWiki) { implicit ctx => me =>
    given play.api.mvc.Request[?] = ctx.body
    env.opening.api
      .lookup(queryFromUrl(key, moves.some), isGranted(_.OpeningWiki))
      .map(_.flatMap(_.query.opening))
      .flatMapz { op =>
        val redirect = Redirect(routes.Opening.byKeyAndMoves(key, moves))
        lila.opening.OpeningWiki.form
          .bindFromRequest()
          .fold(
            _ => redirect.toFuccess,
            text => env.opening.wiki.write(op, text, me.user) inject redirect
          )
      }
  }

  def tree = Open { implicit ctx =>
    Ok(html.opening.tree(lila.opening.OpeningTree.compute, env.opening.api.readConfig)).toFuccess
  }
