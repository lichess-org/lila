package lila.event

import org.joda.time.DateTime
import play.api.i18n.Lang

import lila.user.User

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
    finishesAt: DateTime,
    hostedBy: Option[User.ID] = None,
    icon: Option[String] = None,
    countdown: Boolean
) {

  def willStartLater = startsAt.isAfterNow

  def secondsToStart =
    willStartLater option {
      (startsAt.getSeconds - nowSeconds).toInt
    }

  def featureSince = startsAt minusHours homepageHours

  def featureNow = featureSince.isBeforeNow && !isFinishedSoon

  def isFinishedSoon = finishesAt.isBefore(DateTime.now plusMinutes 5)

  def isFinished = finishesAt.isBeforeNow

  def isNow = startsAt.isBeforeNow && !isFinished

  def isNowOrSoon = startsAt.isBefore(DateTime.now plusMinutes 10) && !isFinished

  def id = _id
}

object Event {

  def makeId = lila.common.ThreadLocalRandom nextString 8

  case class UserId(value: String) extends AnyVal
}
