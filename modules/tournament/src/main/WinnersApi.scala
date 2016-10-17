package lila.tournament

import org.joda.time.DateTime
import reactivemongo.api.ReadPreference
import scala.concurrent.duration.FiniteDuration

import chess.variant.{ Variant, Standard, FromPosition }
import lila.db.dsl._
import lila.user.{ User, UserRepo }
import Schedule.{ Freq, Speed }

case class Winner(
  tourId: String,
  userId: String,
  tourName: String,
  date: DateTime)

case class FreqWinners(
    yearly: Option[Winner],
    monthly: Option[Winner],
    weekly: Option[Winner],
    daily: Option[Winner]) {

  lazy val top: Option[Winner] =
    daily.filter(_.date isAfter DateTime.now.minusHours(2)) orElse
      weekly.filter(_.date isAfter DateTime.now.minusDays(1)) orElse
      monthly.filter(_.date isAfter DateTime.now.minusDays(3)) orElse
      yearly orElse monthly orElse weekly orElse daily
}

case class AllWinners(
    hyperbullet: FreqWinners,
    bullet: FreqWinners,
    superblitz: FreqWinners,
    blitz: FreqWinners,
    classical: FreqWinners,
    variants: Map[String, FreqWinners]) {

  lazy val top: List[Winner] = {
    List(hyperbullet, bullet, superblitz, blitz, classical) :::
      WinnersApi.variants.flatMap { v =>
        variants get v.key
      }
  }.flatMap(_.top)
}

final class WinnersApi(
    coll: Coll,
    mongoCache: lila.memo.MongoCache.Builder,
    ttl: FiniteDuration) {

  import BSONHandlers._
  import lila.db.BSON.MapDocument.MapHandler
  private implicit val WinnerHandler = reactivemongo.bson.Macros.handler[Winner]
  private implicit val FreqWinnersHandler = reactivemongo.bson.Macros.handler[FreqWinners]
  private implicit val AllWinnersHandler = reactivemongo.bson.Macros.handler[AllWinners]

  private def fetchLastFreq(freq: Freq, since: DateTime): Fu[List[Tournament]] = coll.find($doc(
    "schedule.freq" -> freq.name,
    "startsAt" $gt since.minusHours(12),
    "winner" $exists true
  )).sort($sort desc "startsAt")
    .cursor[Tournament](readPreference = ReadPreference.secondaryPreferred)
    .gather[List]()

  private def firstStandardWinner(tours: List[Tournament], speed: Speed): Option[Winner] =
    tours.find { t =>
      t.variant.standard && t.schedule.exists(_.speed == speed)
    }.flatMap(_.winner)

  private def firstVariantWinner(tours: List[Tournament], variant: Variant): Option[Winner] =
    tours.find(_.variant == variant).flatMap(_.winner)

  private def fetchAll: Fu[AllWinners] = for {
    yearlies <- fetchLastFreq(Freq.Yearly, DateTime.now.minusYears(1))
    monthlies <- fetchLastFreq(Freq.Monthly, DateTime.now.minusMonths(2))
    weeklies <- fetchLastFreq(Freq.Weekly, DateTime.now.minusWeeks(2))
    dailies <- fetchLastFreq(Freq.Daily, DateTime.now.minusDays(2))
  } yield {
    def standardFreqWinners(speed: Speed): FreqWinners = FreqWinners(
      yearly = firstStandardWinner(yearlies, speed),
      monthly = firstStandardWinner(monthlies, speed),
      weekly = firstStandardWinner(weeklies, speed),
      daily = firstStandardWinner(dailies, speed))
    AllWinners(
      hyperbullet = standardFreqWinners(Speed.HyperBullet),
      bullet = standardFreqWinners(Speed.Bullet),
      superblitz = standardFreqWinners(Speed.SuperBlitz),
      blitz = standardFreqWinners(Speed.Blitz),
      classical = standardFreqWinners(Speed.Classical),
      variants = WinnersApi.variants.map { v =>
        v.key -> FreqWinners(
          yearly = firstVariantWinner(yearlies, v),
          monthly = firstVariantWinner(monthlies, v),
          weekly = firstVariantWinner(weeklies, v),
          daily = firstVariantWinner(dailies, v))
      }.toMap)
  }

  private val allCache = mongoCache.single[AllWinners](
    prefix = "tournament:winner:all",
    f = fetchAll,
    timeToLive = ttl)

  def all: Fu[AllWinners] = allCache(true)
}

object WinnersApi {

  val variants = Variant.all.filter {
    case Standard | FromPosition => false
    case _                       => true
  }
}
