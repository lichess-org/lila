package lila.mod

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

import lila.common.HTTPRequest
import lila.report.ModId

final class ModStream(system: ActorSystem) {

  private val stringify =
    Enumeratee.map[JsValue].apply[String] { js =>
      Json.stringify(js) + "\n"
    }

  private val classifier = 'userSignup

  def enumerator: Enumerator[String] = {
    var subscriber: Option[lila.common.Tellable] = None
    Concurrent.unicast[JsValue](
      onStart = channel => {
        subscriber = system.lilaBus.subscribeFun(classifier) {
          case lila.security.Signup(user, email, req, fp) =>
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
      onComplete = subscriber foreach { system.lilaBus.unsubscribe(_, classifier) }
    ) &> stringify
  }
}
