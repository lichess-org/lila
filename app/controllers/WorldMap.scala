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

  def stream = Action.async {
    import akka.pattern.ask
    import makeTimeout.short
    import lila.worldMap.Stream
    import akka.stream.scaladsl.Source
    Env.worldMap.stream ? Stream.GetPublisher mapTo manifest[Stream.PublisherType] map { publisher =>
      val source = Source fromPublisher publisher
      Ok.chunked(source via EventSource.flow).as("text/event-stream")
    }
  }
}
