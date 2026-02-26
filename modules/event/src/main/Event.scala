package lila.event
import scalalib.model.Language

case class Event(
    _id: String,
    title: String,
    headline: String,
    description: Option[Markdown],
    homepageHours: Double,
    url: String,
    lang: Language,
    enabled: Boolean,
    createdBy: UserId,
    createdAt: Instant,
    updatedBy: Option[UserId],
    updatedAt: Option[Instant],
    startsAt: Instant,
    finishesAt: Instant,
    hostedBy: Option[UserId] = None,
    icon: Option[String] = None,
    countdown: Boolean
):

  def willStartLater = startsAt.isAfterNow

  def secondsToStart = willStartLater.option:
    (startsAt.toSeconds - nowSeconds).toInt

  def featureSince = startsAt.minusMinutes((homepageHours * 60).toInt)
  def featureUntil = finishesAt.minusMinutes(10)

  def featureDates = (featureSince, featureUntil)

  def featureNow = featureSince.isBeforeNow && featureUntil.isAfterNow

  def isFinished = finishesAt.isBeforeNow

  def isNow = startsAt.isBeforeNow && !isFinished

  def isNowOrSoon = startsAt.isBefore(nowInstant.plusMinutes(10)) && !isFinished

  inline def id = _id

object Event:

  import scalalib.ThreadLocalRandom
  def makeId = ThreadLocalRandom.nextString(8)
