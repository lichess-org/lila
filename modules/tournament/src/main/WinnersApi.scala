package lila.tournament

import chess.variant.{ FromPosition, Standard, Variant }

import lila.db.dsl.{ *, given }

import Schedule.{ Freq, Speed }

case class Winner(
    tourId: TourId,
    userId: UserId,
    tourName: String,
    date: Instant
)

case class FreqWinners(
    yearly: Option[Winner],
    monthly: Option[Winner],
    weekly: Option[Winner],
    daily: Option[Winner]
):

  lazy val top: Option[Winner] =
    daily
      .filter(_.date.isAfter(nowInstant.minusHours(2)))
      .orElse(weekly.filter(_.date.isAfter(nowInstant.minusDays(1))))
      .orElse(monthly.filter(_.date.isAfter(nowInstant.minusDays(3))))
      .orElse(yearly)
      .orElse(monthly)
      .orElse(weekly)
      .orElse(daily)

  def userIds = List(yearly, monthly, weekly, daily).flatten.map(_.userId)

case class AllWinners(
    hyperbullet: FreqWinners,
    bullet: FreqWinners,
    superblitz: FreqWinners,
    blitz: FreqWinners,
    rapid: FreqWinners,
    elite: List[Winner],
    marathon: List[Winner],
    variants: Map[Variant.LilaKey, FreqWinners]
):

  lazy val top: List[Winner] = List(
    List(hyperbullet, bullet, superblitz, blitz, rapid).flatMap(_.top),
    List(elite.headOption, marathon.headOption).flatten,
    WinnersApi.variants.flatMap { v =>
      variants.get(v.key).flatMap(_.top)
    }
  ).flatten

  lazy val userIds =
    List(hyperbullet, bullet, superblitz, blitz, rapid).flatMap(_.userIds) :::
      elite.map(_.userId) ::: marathon.map(_.userId) :::
      variants.values.toList.flatMap(_.userIds)

final class WinnersApi(
    tournamentRepo: TournamentRepo,
    mongoCache: lila.memo.MongoCache.Api,
    scheduler: Scheduler
)(using Executor):

  import BSONHandlers.given
  import reactivemongo.api.bson.*
  private given BSONDocumentHandler[Winner] = Macros.handler
  private given BSONDocumentHandler[FreqWinners] = Macros.handler
  private given BSONHandler[Map[Variant.LilaKey, FreqWinners]] = typedMapHandler[Variant.LilaKey, FreqWinners]
  private given BSONDocumentHandler[AllWinners] = Macros.handler

  private def fetchLastFreq(freq: Freq, since: Instant): Fu[List[Tournament]] =
    tournamentRepo.coll
      .find:
        $doc(
          "schedule.freq" -> freq.name,
          "startsAt".$gt(since.minusHours(12)),
          "winner".$exists(true)
        )
      .sort($sort.desc("startsAt"))
      .cursor[Tournament](ReadPref.sec)
      .list(Int.MaxValue)

  private def firstStandardWinner(tours: List[Tournament], speed: Speed): Option[Winner] =
    tours
      .find: t =>
        t.variant.standard && t.scheduleSpeed.has(speed)
      .flatMap(_.winner)

  private def firstVariantWinner(tours: List[Tournament], variant: Variant): Option[Winner] =
    tours.find(_.variant == variant).flatMap(_.winner)

  private def fetchAll: Fu[AllWinners] =
    for
      yearlies <- fetchLastFreq(Freq.Yearly, nowInstant.minusYears(1))
      monthlies <- fetchLastFreq(Freq.Monthly, nowInstant.minusMonths(2))
      weeklies <- fetchLastFreq(Freq.Weekly, nowInstant.minusWeeks(2))
      dailies <- fetchLastFreq(Freq.Daily, nowInstant.minusDays(2))
      elites <- fetchLastFreq(Freq.Weekend, nowInstant.minusWeeks(3))
      marathons <- fetchLastFreq(Freq.Marathon, nowInstant.minusMonths(13))
    yield
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
        elite = elites.flatMap(_.winner).take(4),
        marathon = marathons.flatMap(_.winner).take(4),
        variants = WinnersApi.variants.view.map { v =>
          v.key -> FreqWinners(
            yearly = firstVariantWinner(yearlies, v),
            monthly = firstVariantWinner(monthlies, v),
            weekly = firstVariantWinner(weeklies, v),
            daily = firstVariantWinner(dailies, v)
          )
        }.toMap
      )

  private val allCache = mongoCache.unit[AllWinners](
    "tournament:winner:all",
    59.minutes
  ): loader =>
    _.refreshAfterWrite(1.hour).buildAsyncFuture(loader(_ => fetchAll))

  def all: Fu[AllWinners] = allCache.get {}

  // because we read on secondaries, delay cache clear
  def clearCache(tour: Tournament): Unit =
    if tour.scheduleFreq.exists(_.isDailyOrBetter) then
      scheduler.scheduleOnce(5.seconds) { allCache.invalidate {} }

  private[tournament] def clearAfterMarking(userId: UserId): Funit = all.map: winners =>
    if winners.userIds contains userId then allCache.invalidate {}

object WinnersApi:

  val variants = Variant.list.all.filter:
    case Standard | FromPosition => false
    case _ => true
