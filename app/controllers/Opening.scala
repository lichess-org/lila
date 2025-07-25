package controllers

import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.core.net.Crawler
import lila.opening.OpeningQuery.queryFromUrl
import lila.opening.OpeningAccessControl

final class Opening(env: Env) extends LilaController(env):

  private given (using RequestHeader, Option[Me]): OpeningAccessControl =
    OpeningAccessControl(env.security.ip2proxy, limit.openingStatsProxy)

  def index(q: Option[String] = None) = Open:
    val searchQuery = ~q
    if searchQuery.nonEmpty then
      val results = env.opening.search(searchQuery)
      if HTTPRequest.isXhr(ctx.req)
      then Ok.snip(views.opening.ui.resultsList(results))
      else Ok.page(views.opening.ui.resultsPage(searchQuery, results, env.opening.api.readConfig))
    else
      FoundPage(env.opening.api.index): page =>
        isGrantedOpt(_.OpeningWiki)
          .so(env.opening.wiki.popularOpeningsWithShortWiki)
          .map:
            views.opening.ui.index(page, _)

  private val openingRateLimit =
    env.security.ipTrust.rateLimit(50, 10.minutes, "opening.byKeyAndMoves", _.proxyMultiplier(3))

  def byKeyAndMoves(key: String, moves: String) = Open:
    Firewall:
      val crawler = HTTPRequest.isCrawler(ctx.req)
      if moves.sizeIs > 10 && crawler.yes then Forbidden
      else
        openingRateLimit(rateLimited):
          env.opening.api
            .lookup(queryFromUrl(key, moves.some), isGrantedOpt(_.OpeningWiki), crawler)
            .flatMap:
              case None => Redirect(routes.Opening.index(key.some))
              case Some(page) =>
                val query = page.query.query
                if query.key.isEmpty then Redirect(routes.Opening.index(key.some))
                else if query.key != key then Redirect(routes.Opening.byKeyAndMoves(query.key, moves))
                else if moves.nonEmpty && page.query.pgnUnderscored != moves && !getBool("r") then
                  Redirect:
                    s"${routes.Opening.byKeyAndMoves(query.key, page.query.pgnUnderscored)}?r=1"
                else
                  Ok.async:
                    page.query.exactOpening.so(env.puzzle.opening.getClosestTo(_)).map { puzzle =>
                      val puzzleKey = puzzle.map(_.fold(_.family.key.value, _.opening.key.value))
                      views.opening.ui.show(page, puzzleKey)
                    }

  def config(thenTo: String) = OpenBody:
    NoCrawlers:
      val redir = Redirect:
        lila.common.HTTPRequest.referer(ctx.req) | {
          if thenTo.isEmpty || thenTo == "index" then routes.Opening.index().url
          else if thenTo.startsWith("q:") then routes.Opening.index(thenTo.drop(2).some).url
          else routes.Opening.byKeyAndMoves(thenTo, "").url
        }
      bindForm(lila.opening.OpeningConfig.form)(
        _ => redir,
        cfg => redir.withCookies(env.opening.config.write(cfg))
      )

  def wikiWrite(key: String, moves: String) = SecureBody(_.OpeningWiki) { ctx ?=> me ?=>
    env.opening.api
      .lookup(queryFromUrl(key, moves.some), isGranted(_.OpeningWiki), Crawler.No)
      .map(_.flatMap(_.query.exactOpening))
      .orNotFound: op =>
        val redirect = Redirect(routes.Opening.byKeyAndMoves(key, moves))
        bindForm(lila.opening.OpeningWiki.form)(
          _ => redirect,
          text =>
            for _ <- env.opening.wiki.write(op, text, me.userId)
            yield
              env.irc.api.openingEdit(me.light, key, moves)
              redirect
        )
  }

  def tree = Open:
    Ok.page(views.opening.ui.tree(lila.opening.OpeningTree.compute, env.opening.api.readConfig))
