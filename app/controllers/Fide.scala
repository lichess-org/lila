package controllers

import play.api.mvc.*
import views.*
import lila.app.{ given, * }

final class Fide(env: Env) extends LilaController(env):

  def index(page: Int) = Open:
    Reasonable(page):
      for
        players      <- env.fide.paginator.best(page)
        renderedPage <- renderPage(html.fide.index(players))
      yield Ok(renderedPage)

  def show(id: chess.FideId, slug: String) = Open:
    Found(env.fide.api.fetch(id)): player =>
      if player.slug != slug then Redirect(routes.Fide.show(id, player.slug))
      else Ok.page(html.fide.show(player))
