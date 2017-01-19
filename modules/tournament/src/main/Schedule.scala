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
      (conditions.minRating, conditions.maxRating) match {
        case (None, None)   => s"${freq.toString} ${speed.toString}"
        case (Some(min), _) => s"Elite ${speed.toString}"
        case (_, Some(max)) => s"U${max.rating} ${speed.toString}"
      }
    case _ if variant.standard => s"${position.shortName} ${speed.toString}"
    case _                     => s"${freq.toString} ${variant.name}"
  }

  def day = at.withTimeAtStartOfDay

  def sameSpeed(other: Schedule) = speed == other.speed

  def similarSpeed(other: Schedule) = Schedule.Speed.similar(speed, other.speed)

  def sameVariant(other: Schedule) = variant.id == other.variant.id

  def sameVariantAndSpeed(other: Schedule) = sameVariant(other) && sameSpeed(other)

  def sameFreq(other: Schedule) = freq == other.freq

  def sameConditions(other: Schedule) = conditions == other.conditions

  def sameMaxRating(other: Schedule) = conditions sameMaxRating other.conditions

  def sameDay(other: Schedule) = day == other.day

  def hasMaxRating = conditions.maxRating.isDefined

  def similarTo(other: Schedule) =
    similarSpeed(other) && sameVariant(other) && sameFreq(other) && sameConditions(other)

  def perfType = PerfType.byVariant(variant) | Schedule.Speed.toPerfType(speed)

  override def toString = s"$freq $variant $speed $conditions $at"
}

object Schedule {

  sealed abstract class Freq(val id: Int, val importance: Int) extends Ordered[Freq] {

    val name = toString.toLowerCase

    def compare(other: Freq) = importance compare other.importance

    def isDaily = this == Schedule.Freq.Daily
    def isDailyOrBetter = this >= Schedule.Freq.Daily
    def isWeeklyOrBetter = this >= Schedule.Freq.Weekly
  }
  object Freq {
    case object Hourly extends Freq(10, 10)
    case object Daily extends Freq(20, 20)
    case object Eastern extends Freq(30, 15)
    case object Weekly extends Freq(40, 40)
    case object Weekend extends Freq(41, 41)
    case object Monthly extends Freq(50, 50)
    case object Marathon extends Freq(60, 60)
    case object ExperimentalMarathon extends Freq(61, 55) { // for DB BC
      override val name = "Experimental Marathon"
    }
    case object Yearly extends Freq(70, 70)
    case object Unique extends Freq(90, 59)
    val all: List[Freq] = List(Hourly, Daily, Eastern, Weekly, Weekend, Monthly, Marathon, ExperimentalMarathon, Yearly, Unique)
    def apply(name: String) = all.find(_.name == name)
    def byId(id: Int) = all.find(_.id == id)
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
    def fromClock(clock: chess.Clock.Config) = {
      val time = clock.estimateTotalTime
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
    Some((s.freq, s.variant, s.speed) match {

      case (Hourly, _, HyperBullet | Bullet)          => 27
      case (Hourly, _, SuperBlitz)                    => 57
      case (Hourly, _, Blitz)                         => 57
      case (Hourly, _, Classical) if s.hasMaxRating   => 57
      case (Hourly, _, Classical)                     => 117

      case (Daily | Eastern, _, HyperBullet | Bullet) => 60
      case (Daily | Eastern, _, SuperBlitz)           => 90
      case (Daily | Eastern, Standard, Blitz)         => 120
      case (Daily | Eastern, _, Classical)            => 150

      case (Daily | Eastern, Crazyhouse, Blitz)       => 90
      case (Daily | Eastern, _, Blitz)                => 60 // variant daily is shorter

      case (Weekly, _, HyperBullet | Bullet)          => 60 * 2
      case (Weekly, _, SuperBlitz)                    => 60 * 3
      case (Weekly, _, Blitz)                         => 60 * 3
      case (Weekly, _, Classical)                     => 60 * 4

      case (Weekend, _, HyperBullet | Bullet)         => 90
      case (Weekend, _, SuperBlitz)                   => 60 * 2
      case (Weekend, _, Blitz)                        => 60 * 3
      case (Weekend, _, Classical)                    => 60 * 4

      case (Monthly, _, HyperBullet | Bullet)         => 60 * 3
      case (Monthly, _, SuperBlitz)                   => 60 * 3 + 30
      case (Monthly, _, Blitz)                        => 60 * 4
      case (Monthly, _, Classical)                    => 60 * 5

      case (Yearly, _, HyperBullet | Bullet)          => 60 * 4
      case (Yearly, _, SuperBlitz)                    => 60 * 5
      case (Yearly, _, Blitz)                         => 60 * 6
      case (Yearly, _, Classical)                     => 60 * 8

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

    val TC = chess.Clock.Config

    (s.freq, s.variant, s.speed) match {
      // Special cases.
      case (Hourly, Crazyhouse, SuperBlitz) if zhInc(s) => TC(3 * 60, 1)
      case (Hourly, Crazyhouse, Blitz) if zhInc(s)      => TC(4 * 60, 2)
      case (Hourly, Standard, Blitz) if standardInc(s)  => TC(3 * 60, 2)

      case (_, _, HyperBullet)                          => TC(30, 0)
      case (_, _, Bullet)                               => TC(60, 0)
      case (_, _, SuperBlitz)                           => TC(3 * 60, 0)
      case (_, _, Blitz)                                => TC(5 * 60, 0)
      case (_, _, Classical)                            => TC(10 * 60, 0)
    }
  }

  private[tournament] def conditionFor(s: Schedule) =
    if (s.conditions.relevant) s.conditions
    else {
      import Freq._, Speed._

      val nbRatedGame = (s.freq, s.speed) match {
        case (Hourly, HyperBullet | Bullet)           => 20
        case (Hourly, SuperBlitz | Blitz)             => 15
        case (Hourly, Classical)                      => 10

        case (Daily | Eastern, HyperBullet | Bullet)  => 20
        case (Daily | Eastern, SuperBlitz | Blitz)    => 15
        case (Daily | Eastern, Classical)             => 10

        case (Weekly | Monthly, HyperBullet | Bullet) => 30
        case (Weekly | Monthly, SuperBlitz | Blitz)   => 20
        case (Weekly | Monthly, Classical)            => 15

        case (Weekend, HyperBullet | Bullet)          => 30
        case (Weekend, SuperBlitz | Blitz)            => 20

        case _                                        => 0
      }

      val minRating = s.freq match {
        case Weekend => 2200
        case _       => 0
      }

      Condition.All(
        nbRatedGame = nbRatedGame.some.filter(0<).map {
          Condition.NbRatedGame(s.perfType.some, _)
        },
        minRating = minRating.some.filter(0<).map {
          Condition.MinRating(s.perfType, _)
        },
        maxRating = none)
    }
}
