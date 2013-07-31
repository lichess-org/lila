package controllers

import play.api.mvc._
import play.api.templates.Html
import views._

import lila.app._
import lila.game.Pov

object TV extends LilaController with Watcher {

  def index = Open { implicit ctx ⇒
    OptionFuResult(Env.game.featured.one) { game ⇒
      watch(Pov creator game)
    }
  }
}
