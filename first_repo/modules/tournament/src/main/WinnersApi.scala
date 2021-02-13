package lila.tournament

import org.joda.time.DateTime
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import chess.variant.{ FromPosition, Standard, Variant }
import lila.db.dsl._
import Schedule.{ Freq, Speed }
import lila.user.User

case class Winner(
    tourId: String,
    userId: String,
    tourName: String,
    date: DateTime
)

case class FreqWinners(
    yearly: Option[Winner],
    monthly: Option[Winner],
    weekly: Option[Winner],
    daily: Option[Winner]
) {

  lazy val top: Option[Winner] =
    daily.filter(_.date isAfter DateTime.now.minusHours(2)) orElse
      weekly.filter(_.date isAfter DateTime.now.minusDays(1)) orElse
      monthly.filter(_.date isAfter DateTime.now.minusDays(3)) orElse
      yearly orElse monthly orElse weekly orElse daily

  def userIds = List(yearly, monthly, weekly, daily).flatten.map(_.userId)
}

case class AllWinners(
    hyperbullet: FreqWinners,
    bullet: FreqWinners,
    superblitz: FreqWinners,
    blitz: FreqWinners,
    rapid: FreqWinners,
    elite: List[Winner],
    marathon: List[Winner],
    variants: Map[String, FreqWinners]
) {

  lazy val top: List[Winner] = List(
    List(hyperbullet, bullet, superblitz, blitz, rapid).flatMap(_.top),
    List(elite.headOption, marathon.headOption).flatten,
    WinnersApi.variants.flatMap { v =>
      variants get v.key flatMap (_.top)
    }
  ).flatten

  lazy val userIds =
    List(hyperbullet, bullet, superblitz, blitz, rapid).flatMap(_.userIds) :::
      elite.map(_.userId) ::: marathon.map(_.userId) :::
      variants.values.toList.flatMap(_.userIds)
}

final class WinnersApi(
    tournamentRepo: TournamentRepo,
    mongoCache: lila.memo.MongoCache.Api,
    scheduler: akka.actor.Scheduler
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._
  implicit private val WinnerHandler      = reactivemongo.api.bson.Macros.handler[Winner]
  implicit private val FreqWinnersHandler = reactivemongo.api.bson.Macros.handler[FreqWinners]
  implicit private val AllWinnersHandler  = reactivemongo.api.bson.Macros.handler[AllWinners]

  private def fetchLastFreq(freq: Freq, since: DateTime): Fu[List[Tournament]] =
    tournamentRepo.coll
      .find(
        $doc(
          "schedule.freq" -> freq.name,
          "startsAt" $gt since.minusHours(12),
          "winner" $exists true
        )
      )
      .sort($sort desc "startsAt")
      .cursor[Tournament](ReadPreference.secondaryPreferred)
      .list(Int.MaxValue)

  private def firstStandardWinner(tours: List[Tournament], speed: Speed): Option[Winner] =
    tours
      .find { t =>
        t.variant.standard && t.schedule.exists(_.speed == speed)
      }
      .flatMap(_.winner)

  private def firstVariantWinner(tours: List[Tournament], variant: Variant): Option[Winner] =
    tours.find(_.variant == variant).flatMap(_.winner)

  private def fetchAll: Fu[AllWinners] =
    for {
      yearlies  <- fetchLastFreq(Freq.Yearly, DateTime.now.minusYears(1))
      monthlies <- fetchLastFreq(Freq.Monthly, DateTime.now.minusMonths(2))
      weeklies  <- fetchLastFreq(Freq.Weekly, DateTime.now.minusWeeks(2))
      dailies   <- fetchLastFreq(Freq.Daily, DateTime.now.minusDays(2))
      elites    <- fetchLastFreq(Freq.Weekend, DateTime.now.minusWeeks(3))
      marathons <- fetchLastFreq(Freq.Marathon, DateTime.now.minusMonths(13))
    } yield {
      def standardFreqWinners(speed: Speed): FreqWinners =
        FreqWinners(
          yearly = firstStandardWinner(yearlies, speed),
          monthly = firstStandardWinner(monthlies, speed),
          weekly = firstStandardWinner(weeklies, speed),
          daily = firstStandardWinner(dailies, speed)
        )
      AllWinners(
        hyperbullet = standardFreqWinners(Speed.HyperBullet),
        bullet = standardFreqWinners(Speed.Bullet),
        superblitz = standardFreqWinners(Speed.SuperBlitz),
        blitz = standardFreqWinners(Speed.Blitz),
        rapid = standardFreqWinners(Speed.Rapid),
        elite = elites flatMap (_.winner) take 4,
        marathon = marathons flatMap (_.winner) take 4,
        variants = WinnersApi.variants.view.map { v =>
          v.key -> FreqWinners(
            yearly = firstVariantWinner(yearlies, v),
            monthly = firstVariantWinner(monthlies, v),
            weekly = firstVariantWinner(weeklies, v),
            daily = firstVariantWinner(dailies, v)
          )
        }.toMap
      )
    }

  private val allCache = mongoCache.unit[AllWinners](
    "tournament:winner:all",
    59 minutes
  ) { loader =>
    _.refreshAfterWrite(1 hour)
      .buildAsyncFuture(loader(_ => fetchAll))
  }

  def all: Fu[AllWinners] = allCache.get {}

  // because we read on secondaries, delay cache clear
  def clearCache(tour: Tournament): Unit =
    if (tour.schedule.exists(_.freq.isDailyOrBetter))
      scheduler.scheduleOnce(5.seconds) { allCache.invalidate {}.unit }.unit

  private[tournament] def clearAfterMarking(userId: User.ID): Funit = all map { winners =>
    if (winners.userIds contains userId) allCache.invalidate {}.unit
  }
}

object WinnersApi {

  val variants = Variant.all.filter {
    case Standard | FromPosition => false
    case _                       => true
  }
}
