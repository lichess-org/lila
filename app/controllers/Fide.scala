package controllers

import play.api.mvc.*
import views.*
import lila.app.{ given, * }

final class Fide(env: Env) extends LilaController(env):

  def index(page: Int) = Open: ctx ?=>
    Reasonable(page):
      for
        players      <- env.player.paginator.best(page)
        renderedPage <- renderPage(html.fide.index(players))
      yield Ok(renderedPage)
