package lila.api

import org.joda.time.DateTime
import play.api.http.HeaderNames
import play.api.mvc.RequestHeader

object Mobile {

  object Api {

    case class Old(
      version: Int,
      // date when a newer version was released
      deprecatedAt: DateTime,
      // date when the server stops accepting requests
      unsupportedAt: DateTime)

    val currentVersion = 2

    val acceptedVersionNumbers = Set(1, 2)

    val oldVersions: List[Old] = List(
      Old( // chat messages are html escaped
        version = 1,
        deprecatedAt = new DateTime("2016-07-13"),
        unsupportedAt = new DateTime("2016-10-13"))
    )

    private val PathPattern = """^.+/socket/v(\d+)$""".r

    def requestVersion(req: RequestHeader): Option[Int] = {
      val accepts = ~req.headers.get(HeaderNames.ACCEPT)
      if (accepts contains "application/vnd.lichess.v2+json") Some(2)
      else if (accepts contains "application/vnd.lichess.v1+json") Some(1)
      else req.path match {
        case PathPattern(version) => parseIntOption(version)
        case _                    => None
      }
    } filter acceptedVersionNumbers.contains

    def requested(req: RequestHeader) = requestVersion(req).isDefined
  }
}
