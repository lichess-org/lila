package lila.event

import org.joda.time.DateTime

import lila.common.Lang

case class Event(
    _id: String,
    title: String,
    headline: String,
    description: Option[String],
    homepageHours: Int,
    url: String,
    lang: Lang,
    enabled: Boolean,
    createdBy: Event.UserId,
    createdAt: DateTime,
    startsAt: DateTime,
    finishesAt: DateTime
) {

  def willStartLater = startsAt isAfter DateTime.now

  def secondsToStart = willStartLater option {
    (startsAt.getSeconds - nowSeconds).toInt
  }

  def featureSince = startsAt minusHours homepageHours

  def featureNow = featureSince.isBefore(DateTime.now) && !isFinishedSoon

  def isFinishedSoon = finishesAt.isBefore(DateTime.now plusMinutes 5)

  def isFinished = finishesAt.isBefore(DateTime.now)

  def isNow = startsAt.isBefore(DateTime.now) && !isFinished

  def isNowOrSoon = startsAt.isBefore(DateTime.now plusMinutes 10) && !isFinished

  def id = _id
}

object Event {

  def makeId = ornicar.scalalib.Random nextString 8

  case class UserId(value: String) extends AnyVal
}
