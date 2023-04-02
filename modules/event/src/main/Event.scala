package lila.event

import play.api.i18n.Lang
import ornicar.scalalib.ThreadLocalRandom

case class Event(
    _id: String,
    title: String,
    headline: String,
    description: Option[Markdown],
    homepageHours: Double,
    url: String,
    lang: Lang,
    enabled: Boolean,
    createdBy: UserId,
    createdAt: DateTime,
    updatedBy: Option[UserId],
    updatedAt: Option[DateTime],
    startsAt: DateTime,
    finishesAt: DateTime,
    hostedBy: Option[UserId] = None,
    icon: Option[String] = None,
    countdown: Boolean
):

  def willStartLater = startsAt.isAfterNow

  def secondsToStart =
    willStartLater option {
      (startsAt.toSeconds - nowSeconds).toInt
    }

  def featureSince = startsAt minusMinutes (homepageHours * 60).toInt

  def featureNow = featureSince.isBeforeNow && !isFinishedSoon

  def isFinishedSoon = finishesAt.isBefore(nowDate plusMinutes 5)

  def isFinished = finishesAt.isBeforeNow

  def isNow = startsAt.isBeforeNow && !isFinished

  def isNowOrSoon = startsAt.isBefore(nowDate plusMinutes 10) && !isFinished

  inline def id = _id

object Event:

  def makeId = ThreadLocalRandom nextString 8
