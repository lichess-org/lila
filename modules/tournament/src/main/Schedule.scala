package lila.tournament

import chess.StartingPosition
import chess.variant.Variant
import org.joda.time.DateTime

import lila.rating.PerfType

case class Schedule(
    freq: Schedule.Freq,
    speed: Schedule.Speed,
    variant: Variant,
    position: StartingPosition,
    at: DateTime,
    conditions: Condition.All = Condition.All.empty) {

  def name = freq match {
    case m@Schedule.Freq.ExperimentalMarathon => m.name
    case _ if variant.standard && position.initial =>
      conditions.maxRating.fold(s"${freq.toString} ${speed.toString}") {
        case Condition.MaxRating(_, rating) => s"U$rating ${speed.toString}"
      }
    case _ if variant.standard => s"${position.shortName} ${speed.toString}"
    case _                     => s"${freq.toString} ${variant.name}"
  }

  def similarSpeed(other: Schedule) = Schedule.Speed.similar(speed, other.speed)

  def sameVariant(other: Schedule) = variant.id == other.variant.id

  def sameFreq(other: Schedule) = freq == other.freq

  def sameConditions(other: Schedule) = conditions == other.conditions

  def sameMaxRating(other: Schedule) = conditions sameMaxRating other.conditions

  def hasMaxRating = conditions.maxRating.isDefined

  def similarTo(other: Schedule) =
    similarSpeed(other) && sameVariant(other) && sameFreq(other) && sameConditions(other)

  override def toString = s"$freq $variant $speed $conditions $at"
}

object Schedule {

  sealed abstract class Freq(val id: Int, val importance: Int) extends Ordered[Freq] {

    val name = toString.toLowerCase

    def compare(other: Freq) = importance compare other.importance

    def isWeeklyOrBetter = this >= Schedule.Freq.Weekly
  }
  object Freq {
    case object Hourly extends Freq(10, 10)
    case object Daily extends Freq(20, 20)
    case object Eastern extends Freq(30, 15)
    case object Weekly extends Freq(40, 40)
    case object Monthly extends Freq(50, 50)
    case object Marathon extends Freq(60, 60)
    case object ExperimentalMarathon extends Freq(61, 55) { // for DB BC
      override val name = "Experimental Marathon"
    }
    case object Unique extends Freq(90, 59)
    val all: List[Freq] = List(Hourly, Daily, Eastern, Weekly, Monthly, Marathon, ExperimentalMarathon, Unique)
    def apply(name: String) = all find (_.name == name)
    def byId(id: Int) = all find (_.id == id)
  }

  sealed abstract class Speed(val id: Int) {
    def name = toString.toLowerCase
  }
  object Speed {
    case object HyperBullet extends Speed(10)
    case object Bullet extends Speed(20)
    case object SuperBlitz extends Speed(30)
    case object Blitz extends Speed(40)
    case object Classical extends Speed(50)
    val all: List[Speed] = List(HyperBullet, Bullet, SuperBlitz, Blitz, Classical)
    val mostPopular: List[Speed] = List(Bullet, Blitz, Classical)
    def apply(name: String) = all find (_.name == name)
    def byId(id: Int) = all find (_.id == id)
    def similar(s1: Speed, s2: Speed) = (s1, s2) match {
      case (a, b) if a == b      => true
      case (HyperBullet, Bullet) => true
      case (Bullet, HyperBullet) => true
      case _                     => false
    }
    def fromClock(clock: TournamentClock) = {
      val time = clock.chessClock.estimateTotalTime
      if (time < 60) HyperBullet
      else if (time < 180) Bullet
      else if (time < 480) Blitz
      else Classical
    }
    def toPerfType(speed: Speed) = speed match {
      case HyperBullet | Bullet => PerfType.Bullet
      case SuperBlitz | Blitz   => PerfType.Blitz
      case Classical            => PerfType.Classical
    }
  }

  sealed trait Season
  object Season {
    case object Spring extends Season
    case object Summer extends Season
    case object Autumn extends Season
    case object Winter extends Season
  }

  private[tournament] def durationFor(s: Schedule): Option[Int] = {
    import Freq._, Speed._
    import chess.variant._
    Some((s.freq, s.speed, s.variant) match {

      case (Hourly, HyperBullet | Bullet, _)          => 27
      case (Hourly, SuperBlitz, _)                    => 57
      case (Hourly, Blitz, _)                         => 57
      case (Hourly, Classical, _) if s.hasMaxRating   => 57
      case (Hourly, Classical, _)                     => 117

      case (Daily | Eastern, HyperBullet | Bullet, _) => 60
      case (Daily | Eastern, SuperBlitz, _)           => 90
      case (Daily | Eastern, Blitz, Standard)         => 120
      case (Daily | Eastern, Classical, _)            => 150

      case (Daily | Eastern, Blitz, Crazyhouse)       => 120
      case (Daily | Eastern, Blitz, _)                => 60 // variant daily is shorter

      case (Weekly, HyperBullet | Bullet, _)          => 60 * 2
      case (Weekly, SuperBlitz, _)                    => 60 * 3
      case (Weekly, Blitz, _)                         => 60 * 3
      case (Weekly, Classical, _)                     => 60 * 4

      case (Monthly, HyperBullet | Bullet, _)         => 60 * 3
      case (Monthly, SuperBlitz, _)                   => 60 * 3 + 30
      case (Monthly, Blitz, _)                        => 60 * 4
      case (Monthly, Classical, _)                    => 60 * 5

      case (Marathon, _, _)                           => 60 * 24 // lol
      case (ExperimentalMarathon, _, _)               => 60 * 4

      case (Unique, _, _)                             => 0

    }) filter (0!=)
  }

  private val standardIncHours = Set(1, 7, 13, 19)
  private def standardInc(s: Schedule) = standardIncHours(s.at.getHourOfDay)
  private def zhInc(s: Schedule) = s.at.getHourOfDay % 2 == 0

  private[tournament] def clockFor(s: Schedule) = {
    import Freq._, Speed._
    import chess.variant._

    val TC = TournamentClock

    (s.speed, s.variant, s.freq) match {
      // Special cases.
      case (SuperBlitz, Crazyhouse, Hourly) if zhInc(s) => TC(3 * 60, 1)
      case (Blitz, Crazyhouse, Hourly) if zhInc(s)      => TC(4 * 60, 2)
      case (Blitz, Standard, Hourly) if standardInc(s)  => TC(3 * 60, 2)

      case (HyperBullet, _, _)                          => TC(30, 0)
      case (Bullet, _, _)                               => TC(60, 0)
      case (SuperBlitz, _, _)                           => TC(3 * 60, 0)
      case (Blitz, _, _)                                => TC(5 * 60, 0)
      case (Classical, _, _)                            => TC(10 * 60, 0)
    }
  }
}
