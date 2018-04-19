package lila.common

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

object HttpStream {

  val stringify =
    Enumeratee.map[JsObject].apply[String] { js =>
      Json.stringify(js) + "\n"
    }

  val stringifyOrEmpty =
    Enumeratee.map[Option[JsObject]].apply[String] {
      _ ?? Json.stringify + "\n"
    }

  def onComplete(stream: Option[ActorRef], system: ActorSystem) =
    stream foreach { actor =>
      system.lilaBus.unsubscribe(actor)
      actor ! PoisonPill
    }

  case object SetOnline
}
