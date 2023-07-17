package lila.mod

import akka.stream.scaladsl.*
import play.api.libs.json.*

import lila.common.{ Bus, HTTPRequest }
import lila.common.Json.given
import lila.security.UserSignup

final class ModStream:

  private val classifier = "userSignup"

  private val blueprint =
    Source
      .queue[UserSignup](32, akka.stream.OverflowStrategy.dropHead)
      .map { case UserSignup(user, email, req, fp, suspIp) =>
        Json.obj(
          "t"           -> "signup",
          "username"    -> user.username,
          "email"       -> email.value,
          "ip"          -> HTTPRequest.ipAddress(req).value,
          "suspIp"      -> suspIp,
          "userAgent"   -> HTTPRequest.userAgent(req),
          "fingerPrint" -> fp
        )
      }
      .map { js =>
        s"${Json.stringify(js)}\n"
      }

  def apply()(using Executor): Source[String, ?] =
    blueprint.mapMaterializedValue: queue =>
      val sub = Bus.subscribeFun(classifier):
        case signup: UserSignup => queue.offer(signup)
      queue
        .watchCompletion()
        .addEffectAnyway:
          Bus.unsubscribe(sub, classifier)
