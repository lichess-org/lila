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
        val actor = system.actorOf(Props(new Actor {
          def receive = {
            case request: IrwinRequest =>
              channel push Json.obj(
                "t" -> "request",
                "origin" -> request.origin.key,
                "user" -> request.suspect.value
              )
            case lila.hub.actorApi.report.Created(userId, "cheat" | "cheatprint", by) if by != ModId.irwin.value =>
              channel push Json.obj(
                "t" -> "reportCreated",
                "user" -> userId
              )
            case lila.hub.actorApi.report.Processed(userId, "cheat" | "cheatprint") =>
              lila.user.UserRepo.isEngine(userId) foreach { marked =>
                channel push Json.obj(
                  "t" -> "reportProcessed",
                  "user" -> userId,
                  "marked" -> marked
                )
              }
            case lila.hub.actorApi.mod.MarkCheater(userId, value) =>
              channel push Json.obj(
                "t" -> "mark",
                "user" -> userId,
                "marked" -> value
              )
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
