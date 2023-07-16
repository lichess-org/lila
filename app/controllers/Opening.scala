package controllers

import play.api.mvc.*
import views.html

import lila.app.{ given, * }
import lila.common.HTTPRequest
import lila.opening.OpeningQuery.queryFromUrl

final class Opening(env: Env) extends LilaController(env):

  def index(q: Option[String] = None) = Open:
    val searchQuery = ~q
    if searchQuery.nonEmpty then
      val results = env.opening.search(searchQuery)
      Ok.page:
        if HTTPRequest isXhr ctx.req
        then html.opening.search.resultsList(results)
        else html.opening.search.resultsPage(searchQuery, results, env.opening.api.readConfig)
    else
      FoundPage(env.opening.api.index): page =>
        isGrantedOpt(_.OpeningWiki).so(env.opening.wiki.popularOpeningsWithShortWiki) map {
          html.opening.index(page, _)
        }

  def byKeyAndMoves(key: String, moves: String) = Open:
    val crawler = HTTPRequest.isCrawler(ctx.req)
    if moves.sizeIs > 40 && crawler.yes then Forbidden
    else
      env.opening.api.lookup(queryFromUrl(key, moves.some), isGrantedOpt(_.OpeningWiki), crawler) flatMap {
        case None => Redirect(routes.Opening.index(key.some))
        case Some(page) =>
          val query = page.query.query
          if query.key.isEmpty then Redirect(routes.Opening.index(key.some))
          else if query.key != key then Redirect(routes.Opening.byKeyAndMoves(query.key, moves))
          else if moves.nonEmpty && page.query.pgnUnderscored != moves && !getBool("r") then
            Redirect:
              s"${routes.Opening.byKeyAndMoves(query.key, page.query.pgnUnderscored)}?r=1"
          else
            Ok.pageAsync:
              page.query.exactOpening.so(env.puzzle.opening.getClosestTo) map { puzzle =>
                val puzzleKey = puzzle.map(_.fold(_.family.key.value, _.opening.key.value))
                html.opening.show(page, puzzleKey)
              }
      }

  def config(thenTo: String) = OpenBody:
    NoCrawlers:
      val redir = Redirect:
        lila.common.HTTPRequest.referer(ctx.req) | {
          if thenTo.isEmpty || thenTo == "index" then routes.Opening.index().url
          else if thenTo startsWith "q:" then routes.Opening.index(thenTo.drop(2).some).url
          else routes.Opening.byKeyAndMoves(thenTo, "").url
        }
      lila.opening.OpeningConfig.form
        .bindFromRequest()
        .fold(_ => redir, cfg => redir.withCookies(env.opening.config.write(cfg)))

  def wikiWrite(key: String, moves: String) = SecureBody(_.OpeningWiki) { ctx ?=> me ?=>
    env.opening.api
      .lookup(queryFromUrl(key, moves.some), isGranted(_.OpeningWiki), Crawler.No)
      .map(_.flatMap(_.query.exactOpening))
      .orNotFound: op =>
        val redirect = Redirect(routes.Opening.byKeyAndMoves(key, moves))
        lila.opening.OpeningWiki.form
          .bindFromRequest()
          .fold(
            _ => redirect,
            text => env.opening.wiki.write(op, text, me.value) inject redirect
          )
  }

  def tree = Open:
    Ok.page(html.opening.tree(lila.opening.OpeningTree.compute, env.opening.api.readConfig))
