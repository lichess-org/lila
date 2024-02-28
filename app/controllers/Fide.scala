package controllers

import play.api.mvc.*
import views.*
import lila.app.{ given, * }
import lila.fide.Federation

final class Fide(env: Env) extends LilaController(env):

  private def WIP(f: Fu[Result])(using Context): Fu[Result] =
    if env.api.mode == play.api.Mode.Dev || isGrantedOpt(_.LichessTeam)
    then f
    else notFound

  def index(page: Int) = Open:
    WIP:
      Reasonable(page):
        for
          players      <- env.fide.paginator.best(page)
          renderedPage <- renderPage(html.fide.index(players))
        yield Ok(renderedPage)

  def show(id: chess.FideId, slug: String) = Open:
    WIP:
      Found(env.fide.repo.player.fetch(id)): player =>
        if player.slug != slug then Redirect(routes.Fide.show(id, player.slug))
        else Ok.page(html.fide.show(player))

  def federations(page: Int) = Open:
    WIP:
      for
        feds         <- env.fide.paginator.federations(page)
        renderedPage <- renderPage(html.fide.federations(feds))
      yield Ok(renderedPage)

  def federation(slug: String) = Open:
    WIP:
      Found(env.fide.federationApi.find(slug).thenPp(slug)): fed =>
        val fedSlug = Federation.nameToSlug(fed.name)
        if slug != fedSlug then Redirect(routes.Fide.federation(fedSlug))
        else Ok.page(html.fide.federation(fed))
