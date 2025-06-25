package lila.web

import io.mola.galimatias.URL

import scala.util.Try

import lila.core.config.BaseUrl

final class ReferrerRedirect(baseUrl: BaseUrl):

  private val sillyLoginReferrersSet   = Set("/login", "/signup", "/mobile")
  private val loginPattern             = """/\w\w/(login|signup|mobile)""".r.pattern
  def sillyLoginReferrers(ref: String) = sillyLoginReferrersSet(ref) || loginPattern.matcher(ref).matches

  private lazy val parsedBaseUrl = URL.parse(baseUrl.value)

  private val validCharsRegex = """^[\w-\.:/\?&=@#%\[\]\+~]+$""".r

  private val forbiddenRegexes = List(
    "^/api/".r,
    "\\.(pgn|trf|csv|gif)$".r,
    "/export/".r,
    "/personal-data$".r
  )

  private def isForbiddenPath(path: String): Boolean =
    forbiddenRegexes.exists(_.findFirstIn(path).isDefined)

  // allow relative and absolute redirects only to the same domain or
  // subdomains, excluding /mobile (which is shown after logout)
  def valid(referrer: String): Option[String] =
    (!sillyLoginReferrers(referrer) && validCharsRegex.matches(referrer)).so:
      Try {
        URL.parse(parsedBaseUrl, referrer)
      }.toOption
        .filter(_.scheme == parsedBaseUrl.scheme)
        .filter(_.host == parsedBaseUrl.host)
        .filterNot(url => isForbiddenPath(url.path))
        .map(_.toString)
