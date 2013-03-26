package lila.app

import akka.actor._

import play.api.templates._
import views.{ html => V }

final class Renderer extends Actor {

  def receive = {
    case lila.game.actorApi.RenderFeaturedJs(game) => 
      V.game.featuredJsNoCtx(game)
  }
}
