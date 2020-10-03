package lila.api

import io.lemonlabs.uri.{ AbsoluteUrl, Url }
import scala.util.Try

import lila.common.config.BaseUrl

final class Util(baseUrl: BaseUrl) {

  val sillyLoginReferrers = Set("/login", "/signup", "/mobile")

  private lazy val lilaHost = AbsoluteUrl.parse(baseUrl.value).host.value

  // allow relative and absolute redirects only to the same domain or
  // subdomains, excluding /mobile (which is shown after logout)
  def goodReferrer(referrer: String): Boolean =
    referrer.nonEmpty &&
      !referrer.contains("\t") && // tab is ignored by the browser and allows injecting external URLs
      !sillyLoginReferrers(referrer) && Try {
        val url = Url.parse(referrer)
        url.schemeOption.fold(true)(scheme => scheme == "http" || scheme == "https") &&
        url.hostOption.fold(true)(host => s".${host.value}".endsWith(s".$lilaHost"))
      }.getOrElse(false)
}
