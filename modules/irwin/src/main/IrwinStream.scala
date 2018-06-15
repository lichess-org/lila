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
            case lila.hub.actorApi.report.Created(userId, "cheat" | "cheatprint", by) if by != ModId.irwin.value =>
              push("reportCreated", userId, Json.obj())
            case lila.hub.actorApi.report.Processed(userId, "cheat" | "cheatprint") =>
              lila.user.UserRepo.isEngine(userId) foreach { marked =>
                push("reportProcessed", userId, Json.obj("marked" -> marked))
              }
            case lila.hub.actorApi.mod.MarkCheater(userId, value) =>
              push("mark", userId, Json.obj("marked" -> value))
          }
        }))
        system.lilaBus.subscribe(actor, 'report, 'adjustCheater, 'irwin)
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
