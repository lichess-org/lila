package lila.api

import org.joda.time.DateTime
import play.api.mvc.RequestHeader

import lila.common.{ ApiVersion, HTTPRequest }

object Mobile {

  object AppVersion {

    def mustUpgrade(v: String) = mustUpgradeFromVersions(v)

    // only call if a more recent version is available in both stores!
    private val mustUpgradeFromVersions = Set(
      "5.1.0",
      "5.1.1",
      "5.2.0"
    )

  }

  object Api {

    val currentVersion = ApiVersion(6)

    val acceptedVersions: Set[ApiVersion] = Set(1, 2, 3, 4, 5, 6) map ApiVersion.apply

    def requestVersion(req: RequestHeader): Option[ApiVersion] =
      HTTPRequest apiVersion req filter acceptedVersions.contains

    def requested(req: RequestHeader) = requestVersion(req).isDefined
  }
}
