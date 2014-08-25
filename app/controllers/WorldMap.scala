package controllers

import play.api.libs.EventSource
import play.api.libs.json._
import play.api.mvc._, Results._

import lila.app._
import views._

object WorldMap extends LilaController {

  def index = Action {
    Ok(views.html.site.worldMap())
  }

  def stream = Action {
    Ok.chunked(
      Env.worldMap.stream.producer &> EventSource()
    ) as "text/event-stream"
  }
}
