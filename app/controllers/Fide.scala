package controllers

import play.api.mvc.*

import lila.app.{ *, given }
import lila.fide.Federation

final class Fide(env: Env) extends LilaController(env):

  def index(page: Int, q: Option[String] = None) = Open:
    Reasonable(page):
      val query = q.so(_.trim)
      chess.FideId
        .from(query.toIntOption)
        .so(env.fide.playerApi.fetch)
        .flatMap:
          case Some(player) => Redirect(routes.Fide.show(player.id, player.slug))
          case None =>
            for
              players      <- env.fide.paginator.best(page, query)
              renderedPage <- renderPage(views.fide.player.index(players, query))
            yield Ok(renderedPage)

  def show(id: chess.FideId, slug: String, page: Int) = Open:
    env.fide.repo.player
      .fetch(id)
      .flatMap:
        case None => NotFound.page(views.fide.player.notFound(id))
        case Some(player) =>
          if player.slug != slug then Redirect(routes.Fide.show(id, player.slug))
          else
            for
              tours    <- env.relay.playerTour.playerTours(player, page)
              rendered <- renderPage(views.fide.player.show(player, tours))
            yield Ok(rendered)

  def federations(page: Int) = Open:
    for
      feds         <- env.fide.paginator.federations(page)
      renderedPage <- renderPage(views.fide.federation.index(feds))
    yield Ok(renderedPage)

  def federation(slug: String, page: Int) = Open:
    Found(env.fide.federationApi.find(slug)): fed =>
      val fedSlug = Federation.nameToSlug(fed.name)
      if slug != fedSlug then Redirect(routes.Fide.federation(fedSlug))
      else
        for
          players  <- env.fide.paginator.federationPlayers(fed, page)
          rendered <- renderPage(views.fide.federation.show(fed, players))
        yield Ok(rendered)
