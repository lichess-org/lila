package lila.security

import com.roundeights.hasher.Implicits.*
import play.api.libs.json.JsValue
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient

// https://github.com/lichess-org/lila-pwned
final class Pwned(ws: StandaloneWSClient, url: String)(using Executor):

  def apply(pass: lila.core.security.ClearPassword): Fu[Boolean] =
    url.nonEmpty.so(
      ws.url(url)
        .addQueryStringParameters("sha1" -> pass.value.sha1)
        .withRequestTimeout(1.second)
        .get()
        .map:
          case res if res.status == 200 =>
            (res.body[JsValue] \ "n").asOpt[Int].exists(_ > 0)
          case res =>
            logger.warn(s"Pwnd ${url} ${res.status} ${res.body[String].take(200)}")
            false
        .monValue: result =>
          _.security.pwned.get(result)
    )
