package lila.security

import com.roundeights.hasher.Implicits.*
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.StandaloneWSClient

opaque type IsPwned = Boolean
object IsPwned extends YesNo[IsPwned]

final class PwnedApi(ws: StandaloneWSClient, rangeUrl: String)(using Executor):

  def isPwned(pass: lila.core.security.ClearPassword): Fu[IsPwned] =
    rangeUrl.nonEmpty.so:
      val (prefix, suffix) = pass.value.sha1.hex.toUpperCase.splitAt(5)
      val url = s"${rangeUrl}${prefix}"
      ws.url(url)
        .addHttpHeaders("Add-Padding" -> "true")
        .withRequestTimeout(1.second)
        .get()
        .map:
          case res if res.status == 200 =>
            IsPwned(res.body[String].contains(suffix))
          case res =>
            logger.warn(s"Pwnd ${url} ${res.status} ${res.body[String].take(200)}")
            IsPwned(false)
        .monValue: result =>
          _.security.pwned.get(result.yes)
