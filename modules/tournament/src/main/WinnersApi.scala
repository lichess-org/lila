package lila.tournament

import scala.concurrent.duration._

import play.api.Mode

import org.joda.time.DateTime
import reactivemongo.api.ReadPreference
import reactivemongo.api.bson.BSONDocumentHandler

import shogi.variant.Variant

import lila.db.dsl._
import lila.tournament.Schedule.Freq
import lila.tournament.Schedule.Speed

case class Winner(
    tourId: String,
    userId: String,
    tourName: String,
    schedule: Option[String],
    date: DateTime,
)

case class FreqWinners(
    yearly: Option[Winner],
    monthly: Option[Winner],
    weekly: Option[Winner],
    daily: Option[Winner],
) {

  lazy val top: Option[Winner] =
    daily.filter(_.date isAfter DateTime.now.minusHours(2)) orElse
      weekly.filter(_.date isAfter DateTime.now.minusDays(1)) orElse
      monthly.filter(_.date isAfter DateTime.now.minusDays(3)) orElse
      yearly orElse monthly orElse weekly orElse daily

  lazy val userIds = List(yearly, monthly, weekly, daily).flatten.map(_.userId)
}

case class AllWinners(
    // hyperbullet: FreqWinners,
    bullet: FreqWinners,
    superblitz: FreqWinners,
    blitz: FreqWinners,
    hyperrapid: FreqWinners,
    rapid: FreqWinners,
    classical: FreqWinners,
    elite: List[Winner],
    // marathon: List[Winner],
    variants: Map[String, FreqWinners],
) {

  lazy val top: List[Winner] = List(
    List(bullet, superblitz, blitz, hyperrapid, rapid, classical).flatMap(_.top),
    List(elite.headOption).flatten,
    WinnersApi.variants.flatMap { v =>
      variants get v.key flatMap (_.top)
    },
  ).flatten

  def userIds =
    List(bullet, superblitz, blitz, hyperrapid, rapid, classical).flatMap(_.userIds) :::
      // elite.map(_.userId) ::: marathon.map(_.userId) :::
      elite.map(_.userId) :::
      variants.values.toList.flatMap(_.userIds)
}

final class WinnersApi(
    tournamentRepo: TournamentRepo,
    mongoCache: lila.memo.MongoCache.Api,
    scheduler: akka.actor.Scheduler,
    mode: play.api.Mode,
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._
  implicit private val WinnerHandler: BSONDocumentHandler[Winner] =
    reactivemongo.api.bson.Macros.handler[Winner]
  implicit private val FreqWinnersHandler: BSONDocumentHandler[FreqWinners] =
    reactivemongo.api.bson.Macros.handler[FreqWinners]
  implicit private val AllWinnersHandler: BSONDocumentHandler[AllWinners] =
    reactivemongo.api.bson.Macros.handler[AllWinners]

  private def fetchLastFreq(freq: Freq, since: DateTime): Fu[List[Tournament]] =
    tournamentRepo.coll.ext
      .find(
        $doc(
          "schedule.freq" -> freq.key,
          "startsAt" $gt since.minusHours(12),
          "winner" $exists true,
        ),
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

  private def sinceDays(days: Int) =
    if (mode == Mode.Prod) DateTime.now.minusDays(days)
    else new DateTime(0) // since the dawn of time

  private def fetchAll: Fu[AllWinners] =
    for {
      yearlies  <- fetchLastFreq(Freq.Yearly, sinceDays(2 * 365))
      monthlies <- fetchLastFreq(Freq.Monthly, sinceDays(3 * 30))
      weeklies  <- fetchLastFreq(Freq.Weekly, sinceDays(3 * 7))
      dailies   <- fetchLastFreq(Freq.Daily, sinceDays(3))
      elites    <- fetchLastFreq(Freq.Weekend, sinceDays(3 * 7))
      // marathons <- fetchLastFreq(Freq.Marathon, sinceDays(2 * 365))
    } yield {
      def standardFreqWinners(speed: Speed): FreqWinners =
        FreqWinners(
          yearly = firstStandardWinner(yearlies, speed),
          monthly = firstStandardWinner(monthlies, speed),
          weekly = firstStandardWinner(weeklies, speed),
          daily = firstStandardWinner(dailies, speed),
        )
      AllWinners(
        // hyperbullet = standardFreqWinners(Speed.HyperBullet),
        bullet = standardFreqWinners(Speed.Bullet),
        superblitz = standardFreqWinners(Speed.SuperBlitz),
        blitz = standardFreqWinners(Speed.Blitz),
        hyperrapid = standardFreqWinners(Speed.HyperRapid),
        rapid = standardFreqWinners(Speed.Rapid),
        classical = standardFreqWinners(Speed.Classical),
        elite = elites flatMap (_.winner) take 4,
        // marathon = marathons flatMap (_.winner) take 4,
        variants = WinnersApi.variants.view.map { v =>
          v.key -> FreqWinners(
            yearly = firstVariantWinner(yearlies, v),
            monthly = firstVariantWinner(monthlies, v),
            weekly = firstVariantWinner(weeklies, v),
            daily = firstVariantWinner(dailies, v),
          )
        }.toMap,
      )
    }

  private val allCache = mongoCache.unit[AllWinners](
    "tournament:winner:all",
    59 minutes,
  ) { loader =>
    _.refreshAfterWrite(1 hour)
      .buildAsyncFuture(loader(_ => fetchAll))
  }

  def all: Fu[AllWinners] = allCache.get {}

  // because we read on secondaries, delay cache clear
  def clearCache(tour: Tournament): Unit =
    if (tour.schedule.exists(_.freq.isDailyOrBetter))
      scheduler.scheduleOnce(5.seconds) { allCache.invalidate {}.unit }.unit

}

object WinnersApi {

  val variants = Variant.all.filterNot(_.standard)
}
