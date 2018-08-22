package lidraughts.mod

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

import lidraughts.common.HTTPRequest
import lidraughts.report.ModId

final class ModStream(system: ActorSystem) {

  import lidraughts.common.HttpStream._

  private val stringify =
    Enumeratee.map[JsValue].apply[String] { js =>
      Json.stringify(js) + "\n"
    }

  def enumerator: Enumerator[String] = {
    var stream: Option[ActorRef] = None
    Concurrent.unicast[JsValue](
      onStart = channel => {
        stream = system.lidraughtsBus.subscribeFun('userSignup) {
          case lidraughts.security.Signup(user, email, req, fp) =>
            channel push Json.obj(
              "t" -> "signup",
              "username" -> user.username,
              "email" -> email.value,
              "ip" -> HTTPRequest.lastRemoteAddress(req).value,
              "userAgent" -> HTTPRequest.userAgent(req),
              "fingerPrint" -> fp.map(_.value)
            )
        } some
      },
      onComplete = onComplete(stream, system)
    ) &> stringify
  }
}
