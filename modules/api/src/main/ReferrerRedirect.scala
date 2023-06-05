package lila.api

import io.mola.galimatias.URL
import scala.util.Try

import lila.common.config.BaseUrl

final class ReferrerRedirect(baseUrl: BaseUrl) {

  private val sillyLoginReferrersSet   = Set("/login", "/signup", "/mobile")
  def sillyLoginReferrers(ref: String) = sillyLoginReferrersSet(ref)

  private lazy val parsedBaseUrl = URL.parse(baseUrl.value)

  private val validCharsRegex = """^[\w-\.:/\?&=@#%\[\]]+$""".r

  // allow relative and absolute redirects only to the same domain or
  // subdomains, excluding /mobile (which is shown after logout)
  def valid(referrer: String): Option[String] =
    (!sillyLoginReferrers(referrer) && validCharsRegex.matches(referrer)) ?? Try {
      URL.parse(URL.parse(baseUrl.value), referrer)
    }.toOption
      .filter { url =>
        (url.scheme == parsedBaseUrl.scheme) && s".${url.host}".endsWith(s".${parsedBaseUrl.host}")
      }
      .map(_.toString)
}
