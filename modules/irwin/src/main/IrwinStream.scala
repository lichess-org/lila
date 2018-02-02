package lila.irwin

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

final class IrwinStream(system: ActorSystem) {

  private val stringify =
    Enumeratee.map[JsValue].apply[String] { js =>
      Json.stringify(js) + "\n"
    }

  def enumerator: Enumerator[String] = {
    var stream: Option[ActorRef] = None
    Concurrent.unicast[JsValue](
      onStart = channel => {
        val actor = system.actorOf(Props(new Actor {
          def receive = {
            case lila.hub.actorApi.report.Created(userId, "cheat" | "cheatprint", _) =>
              channel push Json.obj(
                "t" -> "report",
                "user" -> userId
              )
            case lila.hub.actorApi.mod.MarkCheater(userId, value) =>
              channel push Json.obj(
                "t" -> "mark",
                "user" -> userId,
                "value" -> value
              )
          }
        }))
        system.lilaBus.subscribe(actor, 'report, 'adjustCheater)
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
