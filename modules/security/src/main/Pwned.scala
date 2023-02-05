package lila.security

import lila.common.IpAddress
import lila.user.User
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.DefaultBodyReadables.*
import java.security.MessageDigest
import play.api.libs.json.JsValue

final class Pwned(ws: StandaloneWSClient, url: String)(using Executor, akka.actor.Scheduler):

  def apply(pass: User.ClearPassword): Fu[Boolean] =
    if url.isEmpty then fuFalse
    else
      val sha1 = String(MessageDigest getInstance "SHA-1" digest pass.value.getBytes)
      ws.url(url)
        .addQueryStringParameters("sha1" -> sha1)
        .get()
        .withTimeout(1 seconds, "Pwned.get")
        .map {
          case res if res.status == 200 =>
            val n = (res.body[JsValue] \ "n").asOpt[Int]
            n.exists(_ > 0)
          case res =>
            logger.warn(s"Pwnd ${url} ${res.status} ${res.body[String] take 200}")
            false
        }
        .monValue { result =>
          _.security.pwned.get(result)
        }
