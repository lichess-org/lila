package lila.web

import io.mola.galimatias.URL

import scala.util.Try

import lila.common.HTTPRequest
import lila.core.config.BaseUrl
import lila.core.net.ValidReferrer

final class ReferrerRedirect(baseUrl: BaseUrl):

  private lazy val parsedBaseUrl = URL.parse(baseUrl.value)

  private val validCharsRegex = """^[\w-\.:/\?&=@#%\[\]\+~]+$""".r

  private val forbiddenPaths = List(
    "^/api/",
    "\\.(pgn|trf|csv|gif)$",
    "/export/",
    "/personal-data$",
    """/(\w\w/)?(login|signup|mobile)"""
  ).map(_.r)

  private def isForbiddenPath(path: String): Boolean =
    forbiddenPaths.exists(_.findFirstIn(path).isDefined)

  // allow relative and absolute redirects only to the same domain or
  // subdomains, excluding /mobile (which is shown after logout)
  private[web] def valid(referrer: String): Option[ValidReferrer] =
    validCharsRegex
      .matches(referrer)
      .so:
        Try {
          URL.parse(parsedBaseUrl, referrer)
        }.toOption
          .filter(_.scheme == parsedBaseUrl.scheme)
          .filter(_.host == parsedBaseUrl.host)
          .filterNot(url => isForbiddenPath(url.path))
          .map(_.toString)
          .map(ValidReferrer(_))

  def fromReq(using Req): Option[ValidReferrer] =
    fromQueryParam.orElse:
      HTTPRequest.referer.flatMap(valid)

  def fromQueryParam(using Req): Option[ValidReferrer] =
    HTTPRequest.queryStringGet("referrer").flatMap(valid)
