package lila.event

import org.joda.time.DateTime

import lila.db.dsl._

case class Event(
    _id: String,
    title: String,
    headline: String,
    description: Option[String],
    homepageHours: Int,
    url: String,
    enabled: Boolean,
    createdBy: Event.UserId,
    createdAt: DateTime,
    startsAt: DateTime,
    finishesAt: DateTime) {

  def featureSince = startsAt minusHours homepageHours

  def featureNow = featureSince.isBefore(DateTime.now) && !isFinished

  def isFinished = finishesAt.isBefore(DateTime.now)

  def isNow = startsAt.isBefore(DateTime.now) && !isFinished

  def isNowOrSoon = startsAt.isBefore(DateTime.now plusMinutes 10) && !isFinished

  def id = _id
}

object Event {

  def makeId = ornicar.scalalib.Random nextStringUppercase 8

  case class UserId(value: String) extends AnyVal
}
