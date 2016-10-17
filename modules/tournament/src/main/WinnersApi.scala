package lila.tournament

import org.joda.time.DateTime
import reactivemongo.api.ReadPreference
import scala.concurrent.duration.FiniteDuration

import chess.variant.{ Variant, Standard, FromPosition }
import lila.db.dsl._
import lila.user.{ User, UserRepo }
import Schedule.{ Freq, Speed }

case class FreqWinners(
  yearly: Option[Winner],
  monthly: Option[Winner],
  weekly: Option[Winner],
  daily: Option[Winner])
case class AllWinners(
  hyperbullet: FreqWinners,
  bullet: FreqWinners,
  superblitz: FreqWinners,
  blitz: FreqWinners,
  classical: FreqWinners,
  variants: Map[String, FreqWinners])

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
    }.flatMap(_.pp.winner.pp)

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

  private val scheduledCache = mongoCache[Int, List[Winner]](
    prefix = "tournament:winner",
    f = fetchScheduled,
    timeToLive = ttl,
    keyToString = _.toString)

  private def fetchScheduled(nb: Int): Fu[List[Winner]] = {
    val since = DateTime.now minusMonths 1
    List(Freq.Monthly, Freq.Weekly, Freq.Daily).map { freq =>
      TournamentRepo.lastFinishedScheduledByFreq(freq, since)
    }.sequenceFu.map(_.flatten) flatMap { stds =>
      TournamentRepo.lastFinishedDaily(chess.variant.Crazyhouse) map (stds ::: _.toList)
    } flatMap toursToWinners
  }

  private def toursToWinners(tours: List[Tournament]): Fu[List[Winner]] =
    tours.sortBy(_.schedule.map(_.freq)).reverse.map { tour =>
      PlayerRepo winner tour.id flatMap {
        case Some(player) => UserRepo isEngine player.userId map { engine =>
          !engine option Winner(tour.id, player.userId)
        }
        case _ => fuccess(none)
      }
    }.sequenceFu.map(_.flatten take 10)

  def scheduled(nb: Int): Fu[List[Winner]] = scheduledCache apply nb
}

object WinnersApi {

  val variants = Variant.all.filter {
    case Standard | FromPosition => false
    case _                       => true
  }
}
