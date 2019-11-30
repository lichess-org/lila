package lila.mod

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

import lila.common.{ Bus, HTTPRequest }
import lila.report.ModId

final class ModStream(system: ActorSystem) {

  private val stringify =
    Enumeratee.map[JsValue].apply[String] { js =>
      Json.stringify(js) + "\n"
    }

  private val classifier = "userSignup"

  def enumerator: Enumerator[String] = {
    var subscriber: Option[lila.common.Tellable] = None
    Concurrent.unicast[JsValue](
      onStart = channel => {
        subscriber = Bus.subscribeFun(classifier) {
          case lila.security.Signup(user, email, req, fp, suspIp) =>
            channel push Json.obj(
              "t" -> "signup",
              "username" -> user.username,
              "email" -> email.value,
              "ip" -> HTTPRequest.lastRemoteAddress(req).value,
              "suspIp" -> suspIp,
              "userAgent" -> HTTPRequest.userAgent(req),
              "fingerPrint" -> fp.map(_.value)
            )
        } some
      },
      onComplete = subscriber foreach { Bus.unsubscribe(_, classifier) }
    ) &> stringify
  }
}
