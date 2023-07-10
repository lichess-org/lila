package lila.api

import play.api.mvc.RequestHeader

import lila.common.{ ApiVersion, HTTPRequest }

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

  // Lichess Mobile/{version} ({build-number}) as:{username|anon} os:{android|ios}/{os-version} dev:{device info}
  // see modules/api/src/test/MobileTest.scala
  case class LichessMobileUa(
      version: String,
      build: Int,
      as: Option[UserId],
      osName: String,
      osVersion: String,
      device: String
  )

  object LichessMobileUa:
    private val Regex = """lichess mobile/(\S+) \((\d*)\) as:(\S+) os:(\w+)/(\S+) dev:(.*)""".r
    def parse(ua: String): Option[LichessMobileUa] = ua.toLowerCase match
      case Regex(version, build, as, osName, osVersion, device) =>
        val user = (as != "anon") option UserStr(as).id
        LichessMobileUa(version, ~build.toIntOption, user, osName, osVersion, device).some
      case _ => none
