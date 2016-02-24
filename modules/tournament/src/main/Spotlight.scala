package lila.tournament

import org.joda.time.DateTime

case class Spotlight(
  headline: String,
  description: String,
  homepageHours: Option[Int] = None, // feature on homepage hours before start (max 24)
  iconFont: Option[String] = None,
  iconImg: Option[String] = None)
