package lila.mod

import akka.stream.scaladsl._
import play.api.libs.json._

import lila.common.{ Bus, HTTPRequest }
import lila.security.Signup

final class ModStream(implicit mat: akka.stream.Materializer) {

  private val classifier = "userSignup"

  private val blueprint =
    Source.queue[Signup](32, akka.stream.OverflowStrategy.dropHead)
      .map {
        case Signup(user, email, req, fp, suspIp) => Json.obj(
          "t" -> "signup",
          "username" -> user.username,
          "email" -> email.value,
          "ip" -> HTTPRequest.lastRemoteAddress(req).value,
          "suspIp" -> suspIp,
          "userAgent" -> HTTPRequest.userAgent(req),
          "fingerPrint" -> fp.map(_.value)
        )
      }
      .map { js => s"${Json.stringify(js)}\n" }

  def plugTo(sink: Sink[String, Fu[akka.Done]]): Unit = {

    val (queue, done) = blueprint.toMat(sink)(Keep.both).run()

    val sub = Bus.subscribeFun(classifier) {
      case signup: Signup => queue offer signup
    }
    done onComplete { _ => Bus.unsubscribe(sub, classifier) }
  }
}
