package lila.irwin

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

import lila.report.ModId

final class IrwinStream(system: ActorSystem) {

  import lila.common.HttpStream._

  private val stringify =
    Enumeratee.map[JsValue].apply[String] { js =>
      Json.stringify(js) + "\n"
    }

  def enumerator: Enumerator[String] = {
    var stream: Option[ActorRef] = None
    Concurrent.unicast[JsValue](
      onStart = channel => {
        def push(eventType: String, userId: String, payload: JsObject): Unit = {
          lila.mon.mod.irwin.streamEventType(eventType)()
          channel push payload ++ Json.obj(
            "t" -> eventType,
            "user" -> userId
          )
        }
        stream = system.lilaBus.subscribeFun('irwin) {
          case request: IrwinRequest =>
            push("request", request.suspect.value, Json.obj("origin" -> request.origin.key))
        } some
      },
      onComplete = onComplete(stream, system)
    ) &> stringify
  }
}
