package lila.socket

import akka.stream.scaladsl.Flow
import play.api.libs.json.JsObject
import play.api.libs.streams.ActorFlow

trait WithSocket {

  type JsFlow = Flow[JsObject, JsObject, _]
}
