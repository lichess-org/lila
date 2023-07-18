package lila.security

import play.api.mvc.RequestHeader

import lila.common.{ ApiVersion, HTTPRequest }
import lila.socket.Socket.Sri

object Mobile:

  object AppVersion:

    def mustUpgrade(v: String) = mustUpgradeFromVersions(v)

    // only call if a more recent version is available in both stores!
    private val mustUpgradeFromVersions = Set(
      "5.1.0",
      "5.1.1",
      "5.2.0"
    )

  object Api:

    val currentVersion = ApiVersion.lichobile

    val acceptedVersions: Set[ApiVersion] = Set(1, 2, 3, 4, 5, 6) map { ApiVersion(_) }

    def requestVersion(req: RequestHeader): Option[ApiVersion] =
      HTTPRequest apiVersion req filter acceptedVersions.contains

    def requested(req: RequestHeader) = requestVersion(req).isDefined

  // Lichess Mobile/{version} ({build-number}) as:{username|anon} sri:{sri} os:{android|ios}/{os-version} dev:{device info}
  // see modules/api/src/test/MobileTest.scala
  case class LichessMobileUa(
      version: String,
      build: Int,
      userId: Option[UserId],
      sri: Sri,
      osName: String,
      osVersion: String,
      device: String
  )

  object LichessMobileUa:
    private val RegexOld =
      """(?i)lichess mobile/(\S+) \((\d*)\) as:(\S+) os:(android|ios)/(\S+) dev:(.*)""".r // remove soon
    private val Regex =
      """(?i)lichess mobile/(\S+) \((\d*)\) as:(\S+) sri:(\S+) os:(android|ios)/(\S+) dev:(.*)""".r
    def parse(req: RequestHeader): Option[LichessMobileUa] = HTTPRequest.userAgent(req) flatMap parse
    def parse(ua: UserAgent): Option[LichessMobileUa] = ua.value
      .startsWith("Lichess Mobile/")
      .so:
        ua.value match
          case Regex(version, build, user, sri, osName, osVersion, device) =>
            val userId = (user != "anon") option UserStr(user).id
            LichessMobileUa(version, ~build.toIntOption, userId, Sri(sri), osName, osVersion, device).some
          case RegexOld(version, build, user, osName, osVersion, device) =>
            val userId = (user != "anon") option UserStr(user).id
            LichessMobileUa(version, ~build.toIntOption, userId, Sri("old"), osName, osVersion, device).some
          case wut => none
