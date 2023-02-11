package lila.security

import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.DefaultBodyReadables.*
import com.roundeights.hasher.Implicits.*
import play.api.libs.json.JsValue

// https://github.com/lichess-org/lila-pwned
final class Pwned(ws: StandaloneWSClient, url: String)(using Executor):

  def apply(pass: lila.user.User.ClearPassword): Fu[Boolean] =
    if url.isEmpty then fuFalse
    else
      ws.url(url)
        .addQueryStringParameters("sha1" -> pass.value.sha1)
        .withRequestTimeout(1.second)
        .get()
        .map {
          case res if res.status == 200 =>
            (res.body[JsValue] \ "n").asOpt[Int].exists(_ > 0)
          case res =>
            logger.warn(s"Pwnd ${url} ${res.status} ${res.body[String] take 200}")
            false
        }
        .monValue { result =>
          _.security.pwned.get(result)
        }
