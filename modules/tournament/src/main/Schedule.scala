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
    conditions: Condition.All = Condition.All.empty
) {

  def name = freq match {
    case m @ Schedule.Freq.ExperimentalMarathon => m.name
    case _ if variant == chess.variant.Crazyhouse && conditions.minRating.isDefined => "Elite Crazyhouse"
    case _ if variant.standard && position.initial =>
      (conditions.minRating, conditions.maxRating) match {
        case (None, None) => s"${freq.toString} ${speed.toString}"
        case (Some(min), _) => s"Elite ${speed.toString}"
        case (_, Some(max)) => s"U${max.rating} ${speed.toString}"
      }
    case _ if variant.standard => s"${position.shortName} ${speed.toString}"
    case Schedule.Freq.Hourly => s"${variant.name} ${speed.toString}"
    case _ => s"${freq.toString} ${variant.name}"
  }

  def day = at.withTimeAtStartOfDay

  def sameSpeed(other: Schedule) = speed == other.speed

  def similarSpeed(other: Schedule) = Schedule.Speed.similar(speed, other.speed)

  def sameVariant(other: Schedule) = variant.id == other.variant.id

  def sameVariantAndSpeed(other: Schedule) = sameVariant(other) && sameSpeed(other)

  def sameFreq(other: Schedule) = freq == other.freq

  def sameConditions(other: Schedule) = conditions == other.conditions

  def sameMaxRating(other: Schedule) = conditions sameMaxRating other.conditions

  def similarConditions(other: Schedule) = conditions similar other.conditions

  def sameDay(other: Schedule) = day == other.day

  def hasMaxRating = conditions.maxRating.isDefined

  def similarTo(other: Schedule) =
    similarSpeed(other) && sameVariant(other) && sameFreq(other) && sameConditions(other)

  def perfType = PerfType.byVariant(variant) | Schedule.Speed.toPerfType(speed)

  def plan = Schedule.Plan(this, None)
  def plan(build: Tournament => Tournament) = Schedule.Plan(this, build.some)

  override def toString = s"$freq $variant $speed $conditions $at"
}

object Schedule {

  case class Plan(schedule: Schedule, buildFunc: Option[Tournament => Tournament]) {

    def build: Tournament = {
      val t = Tournament.schedule(addCondition(schedule), durationFor(schedule))
      buildFunc.foldRight(t) { _(_) }
    }

    def map(f: Tournament => Tournament) = copy(
      buildFunc = buildFunc.fold(f)(f.compose).some
    )
  }

  sealed abstract class Freq(val id: Int, val importance: Int) extends Ordered[Freq] {

    val name = toString.toLowerCase

    def compare(other: Freq) = Integer.compare(importance, other.importance)

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
    case object Shield extends Freq(51, 51)
    case object Marathon extends Freq(60, 60)
    case object ExperimentalMarathon extends Freq(61, 55) { // for DB BC
      override val name = "Experimental Marathon"
    }
    case object Yearly extends Freq(70, 70)
    case object Unique extends Freq(90, 59)
    val all: List[Freq] = List(Hourly, Daily, Eastern, Weekly, Weekend, Monthly, Shield, Marathon, ExperimentalMarathon, Yearly, Unique)
    def apply(name: String) = all.find(_.name == name)
    def byId(id: Int) = all.find(_.id == id)
  }

  sealed abstract class Speed(val id: Int) {
    def name = toString.toLowerCase
  }
  object Speed {
    case object UltraBullet extends Speed(5)
    case object HyperBullet extends Speed(10)
    case object Bullet extends Speed(20)
    case object HippoBullet extends Speed(25)
    case object SuperBlitz extends Speed(30)
    case object Blitz extends Speed(40)
    case object Rapid extends Speed(50)
    case object Classical extends Speed(60)
    val all: List[Speed] = List(UltraBullet, HyperBullet, Bullet, HippoBullet, SuperBlitz, Blitz, Rapid, Classical)
    val mostPopular: List[Speed] = List(Bullet, Blitz, Rapid, Classical)
    def apply(name: String) = all find (_.name == name)
    def byId(id: Int) = all find (_.id == id)
    def similar(s1: Speed, s2: Speed) = (s1, s2) match {
      case (a, b) if a == b => true
      case (HyperBullet, Bullet) | (Bullet, HyperBullet) => true
      case (Bullet, HippoBullet) | (HippoBullet, Bullet) => true
      case _ => false
    }
    def fromClock(clock: chess.Clock.Config) = {
      val time = clock.estimateTotalSeconds
      if (time < 30) UltraBullet
      else if (time < 60) HyperBullet
      else if (time < 120) Bullet
      else if (time < 180) HippoBullet
      else if (time < 480) Blitz
      else if (time < 1500) Rapid
      else Classical
    }
    def toPerfType(speed: Speed) = speed match {
      case UltraBullet => PerfType.UltraBullet
      case HyperBullet | Bullet | HippoBullet => PerfType.Bullet
      case SuperBlitz | Blitz => PerfType.Blitz
      case Rapid => PerfType.Rapid
      case Classical => PerfType.Classical
    }
  }

  sealed trait Season
  object Season {
    case object Spring extends Season
    case object Summer extends Season
    case object Autumn extends Season
    case object Winter extends Season
  }

  private[tournament] def durationFor(s: Schedule): Int = {
    import Freq._, Speed._
    import chess.variant._

    (s.freq, s.variant, s.speed) match {

      case (Hourly, _, UltraBullet | HyperBullet | Bullet) => 27
      case (Hourly, _, HippoBullet | SuperBlitz | Blitz) => 57
      case (Hourly, _, Rapid) if s.hasMaxRating => 57
      case (Hourly, _, Rapid | Classical) => 117

      case (Daily | Eastern, Standard, SuperBlitz) => 90
      case (Daily | Eastern, Standard, Blitz) => 120
      case (Daily | Eastern, _, Blitz) => 90
      case (Daily | Eastern, _, Rapid | Classical) => 150
      case (Daily | Eastern, _, _) => 60

      case (Weekly, _, UltraBullet | HyperBullet | Bullet) => 60 * 2
      case (Weekly, _, HippoBullet | SuperBlitz | Blitz) => 60 * 3
      case (Weekly, _, Rapid) => 60 * 4
      case (Weekly, _, Classical) => 60 * 5

      case (Weekend, _, UltraBullet | HyperBullet | Bullet) => 90
      case (Weekend, _, HippoBullet | SuperBlitz) => 60 * 2
      case (Weekend, _, Blitz) => 60 * 3
      case (Weekend, _, Rapid) => 60 * 4
      case (Weekend, _, Classical) => 60 * 5

      case (Monthly, _, UltraBullet) => 60 * 2
      case (Monthly, _, HyperBullet | Bullet) => 60 * 3
      case (Monthly, _, HippoBullet | SuperBlitz) => 60 * 3 + 30
      case (Monthly, _, Blitz) => 60 * 4
      case (Monthly, _, Rapid) => 60 * 5
      case (Monthly, _, Classical) => 60 * 6

      case (Shield, _, UltraBullet) => 60 * 3
      case (Shield, _, HyperBullet | Bullet) => 60 * 4
      case (Shield, _, HippoBullet | SuperBlitz) => 60 * 5
      case (Shield, _, Blitz) => 60 * 6
      case (Shield, _, Rapid) => 60 * 8
      case (Shield, _, Classical) => 60 * 10

      case (Yearly, _, UltraBullet | HyperBullet | Bullet) => 60 * 4
      case (Yearly, _, HippoBullet | SuperBlitz) => 60 * 5
      case (Yearly, _, Blitz) => 60 * 6
      case (Yearly, _, Rapid) => 60 * 8
      case (Yearly, _, Classical) => 60 * 10

      case (Marathon, _, _) => 60 * 24 // lol
      case (ExperimentalMarathon, _, _) => 60 * 4

      case (Unique, _, _) => 60 * 6
    }
  }

  private val standardIncHours = Set(1, 7, 13, 19)
  private def standardInc(s: Schedule) = standardIncHours(s.at.getHourOfDay)
  private def zhInc(s: Schedule) = s.at.getHourOfDay % 2 == 0

  private def zhEliteTc(s: Schedule) = {
    val TC = chess.Clock.Config
    s.at.getDayOfMonth / 7 match {
      case 0 => TC(3 * 60, 0)
      case 1 => TC(1 * 60, 1)
      case 2 => TC(3 * 60, 2)
      case 3 => TC(1 * 60, 0)
      case _ => TC(2 * 60, 0) // for the sporadic 5th Saturday
    }
  }

  private[tournament] def clockFor(s: Schedule) = {
    import Freq._, Speed._
    import chess.variant._

    val TC = chess.Clock.Config

    (s.freq, s.variant, s.speed) match {
      // Special cases.
      case (Weekend, Crazyhouse, Blitz) => zhEliteTc(s)
      case (Hourly, Crazyhouse, SuperBlitz) if zhInc(s) => TC(3 * 60, 1)
      case (Hourly, Crazyhouse, Blitz) if zhInc(s) => TC(4 * 60, 2)
      case (Hourly, Standard, Blitz) if standardInc(s) => TC(3 * 60, 2)

      case (Shield, variant, Blitz) if variant.exotic => TC(3 * 60, 2)

      case (_, _, UltraBullet) => TC(15, 0)
      case (_, _, HyperBullet) => TC(30, 0)
      case (_, _, Bullet) => TC(60, 0)
      case (_, _, HippoBullet) => TC(2 * 60, 0)
      case (_, _, SuperBlitz) => TC(3 * 60, 0)
      case (_, _, Blitz) => TC(5 * 60, 0)
      case (_, _, Rapid) => TC(10 * 60, 0)
      case (_, _, Classical) => TC(20 * 60, 10)
    }
  }
  private[tournament] def addCondition(s: Schedule) =
    s.copy(conditions = conditionFor(s))

  private[tournament] def conditionFor(s: Schedule) =
    if (s.conditions.relevant) s.conditions
    else {
      import Freq._, Speed._

      val nbRatedGame = (s.freq, s.speed) match {

        case (_, UltraBullet) => 0

        case (Hourly, UltraBullet | HyperBullet | Bullet) => 20
        case (Hourly, HippoBullet | SuperBlitz | Blitz) => 15
        case (Hourly, Rapid) => 10

        case (Daily | Eastern, UltraBullet | HyperBullet | Bullet) => 20
        case (Daily | Eastern, HippoBullet | SuperBlitz | Blitz) => 15
        case (Daily | Eastern, Rapid) => 15

        case (Weekly | Monthly | Shield, UltraBullet | HyperBullet | Bullet) => 30
        case (Weekly | Monthly | Shield, HippoBullet | SuperBlitz | Blitz) => 20
        case (Weekly | Monthly | Shield, Rapid) => 15

        case (Weekend, UltraBullet | HyperBullet | Bullet) => 30
        case (Weekend, HippoBullet | SuperBlitz | Blitz) => 20

        case _ => 0
      }

      val minRating = (s.freq, s.variant) match {
        case (Weekend, chess.variant.Crazyhouse) => 2100
        case (Weekend, _) => 2200
        case _ => 0
      }

      Condition.All(
        nbRatedGame = nbRatedGame.some.filter(0<).map {
          Condition.NbRatedGame(s.perfType.some, _)
        },
        minRating = minRating.some.filter(0<).map {
          Condition.MinRating(s.perfType, _)
        },
        maxRating = none,
        titled = none,
        teamMember = none
      )
    }
}
