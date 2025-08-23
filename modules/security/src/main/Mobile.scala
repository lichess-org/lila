package lila.security

import play.api.mvc.RequestHeader

import lila.common.HTTPRequest
import lila.core.net.{ ApiVersion, UserAgent }
import lila.core.socket.Sri

object Mobile:

  object AppVersion:
    def mustUpgrade(v: String) = v.headOption.flatMap(_.toString.toIntOption).exists(_ < 6)

  object Api:

    val currentVersion = ApiVersion.lichobile

    val acceptedVersions: Set[ApiVersion] = ApiVersion.from(Set(1, 2, 3, 4, 5, 6))

    def requestVersion(req: RequestHeader): Option[ApiVersion] =
      HTTPRequest.apiVersion(req).filter(acceptedVersions.contains)

    def requested(req: RequestHeader) = requestVersion(req).isDefined

  // Lichess Mobile/{version} as:{username|anon} sri:{sri} os:{Android|iOS}/{os-version} dev:{device info}
  // see modules/api/src/test/MobileTest.scala
  import lila.core.net.LichessMobileUa

  object LichessMobileUa:
    def is(ua: UserAgent): Boolean = ua.value.startsWith("Lichess Mobile/")
    private val Regex =
      """(?i)lichess mobile/(\S+)(?: \(\d*\))? as:(\S+) sri:(\S+) os:(Android|iOS)/(\S+) dev:(.*)""".r
    def parse(req: RequestHeader): Option[LichessMobileUa] = HTTPRequest.userAgent(req).flatMap(parse)
    def parse(ua: UserAgent): Option[LichessMobileUa] = is(ua).so:
      ua.value match
        case Regex(version, user, sri, osName, osVersion, device) =>
          val userId = (user != "anon").option(UserStr(user).id)
          lila.core.net.LichessMobileUa(version, userId, Sri(sri), osName, osVersion, device).some
        case _ => none

  // LM/{version} {Android|iOS}/{os-version} {device info}
  // stored in security documents
  case class LichessMobileUaTrim(version: String, osName: String, osVersion: String, device: String)

  object LichessMobileUaTrim:
    def is(ua: UserAgent): Boolean = ua.value.startsWith("LM/")
    private val Regex = """LM/(\S+) (Android|iOS)/(\S+) (.*)""".r
    def parse(ua: UserAgent): Option[LichessMobileUaTrim] = is(ua).so:
      ua.value match
        case Regex(version, osName, osVersion, device) =>
          LichessMobileUaTrim(version, osName, osVersion, device).some
        case _ => none
    def write(m: LichessMobileUa) = s"""LM/${m.version} ${m.osName}/${m.osVersion} ${m.device.take(60)}"""
