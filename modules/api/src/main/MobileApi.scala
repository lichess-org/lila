package lila.api

import org.joda.time.DateTime

object MobileApi {

  case class Old(
    version: Int,
    // date when a newer version was released
    deprecatedAt: DateTime,
    // date when the server stops accepting requests
    unsupportedAt: DateTime)

  def currentVersion = 1

  def oldVersions: List[Old] = List(
    // old version 0 is just an example, so the list is never empty :)
    // nobody ever used version 0.
    Old(
      version = 0,
      deprecatedAt = new DateTime("2014-08-01"),
      unsupportedAt = new DateTime("2014-12-01"))
  )
}
