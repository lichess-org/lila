package lila.api

import io.mola.galimatias.{ URL, URLParsingSettings, StrictErrorHandler }
import scala.util.Try

import lila.common.config.BaseUrl

final class ReferrerRedirect(baseUrl: BaseUrl) {

  val sillyLoginReferrers = Set("/login", "/signup", "/mobile")

  private lazy val lilaHost = URL.parse(baseUrl.value).host.toString

  private val validCharsRegex = """^[\w-\.:/\?&=@#%\[\]\+]+$""".r

  // allow relative and absolute redirects only to the same domain or
  // subdomains, excluding /mobile (which is shown after logout)
  def valid(referrer: String): Option[String] =
    (!sillyLoginReferrers(referrer) && validCharsRegex.matches(referrer)) ?? Try {
        URL.parse(
          URLParsingSettings.create.withErrorHandler(StrictErrorHandler.getInstance),
          URL.parse(baseUrl.value),
          referrer
        )
      }.toOption
        .filter { url =>
          (url.scheme == "http" || url.scheme == "https") && s".${url.host}".endsWith(s".$lilaHost")
        }
        .map(_.toString)
}
