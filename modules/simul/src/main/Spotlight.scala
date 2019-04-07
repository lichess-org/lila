package lidraughts.simul

import org.joda.time.DateTime

case class Spotlight(
    headline: String,
    description: String,
    startsAt: DateTime,
    homepageHours: Option[Int] = None, // feature on homepage hours before start
    iconFont: Option[String] = None,
    iconImg: Option[String] = None
)
