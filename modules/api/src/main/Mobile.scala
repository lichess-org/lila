package lila.api

import org.joda.time.DateTime
import play.api.http.HeaderNames
import play.api.mvc.RequestHeader

import lila.common.ApiVersion

object Mobile {

  object Api {

    case class Old(
      version: ApiVersion,
      // date when a newer version was released
      deprecatedAt: DateTime,
      // date when the server stops accepting requests
      unsupportedAt: DateTime)

    val currentVersion = ApiVersion(2)

    val acceptedVersions: Set[ApiVersion] = Set(1, 2) map ApiVersion.apply

    val oldVersions: List[Old] = List(
      Old( // chat messages are html escaped
        version = ApiVersion(1),
        deprecatedAt = new DateTime("2016-08-13"),
        unsupportedAt = new DateTime("2016-11-13"))
    )

    private val PathPattern = """^.+/socket/v(\d+)$""".r

    def requestVersion(req: RequestHeader): Option[ApiVersion] = {
      val accepts = ~req.headers.get(HeaderNames.ACCEPT)
      if (accepts contains "application/vnd.lichess.v2+json") Some(ApiVersion(2))
      else if (accepts contains "application/vnd.lichess.v1+json") Some(ApiVersion(1))
      else req.path match {
        case PathPattern(version) => parseIntOption(version) map ApiVersion.apply
        case _                    => None
      }
    } filter acceptedVersions.contains

    def requested(req: RequestHeader) = requestVersion(req).isDefined
  }
}
