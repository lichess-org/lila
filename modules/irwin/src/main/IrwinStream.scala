package lila.irwin

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

import lila.report.ModId

final class IrwinStream(system: ActorSystem) {

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
        val actor = system.actorOf(Props(new Actor {
          def receive = {
            case request: IrwinRequest =>
              push("request", request.suspect.value, Json.obj("origin" -> request.origin.key))
          }
        }))
        system.lilaBus.subscribe(actor, 'irwin)
      },
      onComplete = {
        stream.foreach { actor =>
          system.lilaBus.unsubscribe(actor)
          actor ! PoisonPill
        }
      }
    ) &> stringify
  }
}
