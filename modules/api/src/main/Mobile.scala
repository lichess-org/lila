package lila.api

import org.joda.time.DateTime
import play.api.mvc.RequestHeader

import lila.common.{ ApiVersion, HTTPRequest }

object Mobile {

  object AppVersion {

    def mustUpgrade(v: String) = mustUpgradeFromVersions(v)

    // only call if a more recent version is available in both stores!
    private val mustUpgradeFromVersions = Set.empty[String]

  }

  object Api {

    case class Old(
        version: ApiVersion,
        // date when a newer version was released
        deprecatedAt: DateTime,
        // date when the server stops accepting requests
        unsupportedAt: DateTime
    )

    val currentVersion = ApiVersion(5)

    val acceptedVersions: Set[ApiVersion] = Set(1, 2, 3, 4, 5) map ApiVersion.apply

    val oldVersions: List[Old] = List.empty

    def requestVersion(req: RequestHeader): Option[ApiVersion] =
      HTTPRequest apiVersion req filter acceptedVersions.contains

    def requested(req: RequestHeader) = requestVersion(req).isDefined
  }
}
