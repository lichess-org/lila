package controllers

import akka.stream.scaladsl._
import play.api.http._
import play.api.libs.json._
import play.api.mvc.WebSocket.MessageFlowTransformer

import lila.app._

trait LilaSocket { self: LilaController =>

  protected implicit val jsonMessageFlowTransformer: MessageFlowTransformer[JsObject, JsObject] = {
    import scala.util.control.NonFatal
    import play.api.libs.streams.AkkaStreams
    import websocket._
    def closeOnException[T](block: => Option[T]) = try {
      Left(block getOrElse {
        sys error "Not a JsObject"
      })
    }
    catch {
      case NonFatal(e) => Right(CloseMessage(Some(CloseCodes.Unacceptable),
        "Unable to parse json message"))
    }

    new MessageFlowTransformer[JsObject, JsObject] {
      def transform(flow: Flow[JsObject, JsObject, _]) = {
        AkkaStreams.bypassWith[Message, JsObject, Message](Flow[Message].collect {
          case BinaryMessage(data) => closeOnException(Json.parse(data.iterator.asInputStream).asOpt[JsObject])
          case TextMessage(text)   => closeOnException(Json.parse(text).asOpt[JsObject])
        })(flow map { json => TextMessage(Json.stringify(json)) })
      }
    }
  }
}
