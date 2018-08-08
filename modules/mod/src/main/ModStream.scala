package lila.mod

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

import lila.report.ModId
import lila.common.HTTPRequest

final class ModStream(system: ActorSystem) {

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
            case lila.security.Signup(user, email, req, fp) =>
              channel push Json.obj(
                "t" -> "signup",
                "username" -> user.username,
                "email" -> email.value,
                "ip" -> HTTPRequest.lastRemoteAddress(req).value,
                "userAgent" -> HTTPRequest.userAgent(req),
                "fingerPrint" -> fp.map(_.value)
              )
          }
        }))
        system.lilaBus.subscribe(actor, 'userSignup)
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
