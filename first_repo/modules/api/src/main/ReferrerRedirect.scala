package lila.api

import io.lemonlabs.uri.{ AbsoluteUrl, Url }
import scala.util.Try

import lila.common.config.BaseUrl

final class ReferrerRedirect(baseUrl: BaseUrl) {

  val sillyLoginReferrers = Set("/login", "/signup", "/mobile")

  private lazy val lilaHost = AbsoluteUrl.parse(baseUrl.value).host.value

  private val validCharsRegex = """^[\w-\.:/\?&=@#%\[\]\+]+$""".r

  // allow relative and absolute redirects only to the same domain or
  // subdomains, excluding /mobile (which is shown after logout)
  def valid(referrer: String): Boolean =
    validCharsRegex.matches(referrer) &&
      !sillyLoginReferrers(referrer) && Try {
        val url = Url.parse(referrer)
        url.schemeOption.fold(true)(scheme => scheme == "http" || scheme == "https") &&
        url.hostOption.fold(true)(host => s".${host.value}".endsWith(s".$lilaHost"))
      }.getOrElse(false)
}
