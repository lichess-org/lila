package lila.event

import org.joda.time.DateTime

import lila.db.dsl._

case class Event(
    _id: String,
    title: String,
    headline: String,
    homepageHours: Int,
    url: String,
    enabled: Boolean,
    createdBy: Event.UserId,
    createdAt: DateTime,
    startsAt: DateTime,
    finishesAt: DateTime) {

  def featureSince = startsAt minusHours homepageHours

  def isNow = featureSince.isBefore(DateTime.now) && finishesAt.isAfter(DateTime.now)

  def id = _id
}

object Event {

  def makeId = ornicar.scalalib.Random nextStringUppercase 8

  case class UserId(value: String) extends AnyVal
}
