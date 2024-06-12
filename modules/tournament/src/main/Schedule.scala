package lila.tournament

import shogi.format.forsyth.Sfen
import shogi.variant.Variant
import org.joda.time.DateTime
import play.api.i18n.Lang

import lila.rating.PerfType

case class Schedule(
    freq: Schedule.Freq,
    speed: Schedule.Speed,
    variant: Variant,
    position: Option[Sfen],
    at: DateTime,
    conditions: Condition.All = Condition.All.empty
) {

  def name(full: Boolean = true)(implicit lang: Lang): String = {
    import Schedule.Freq._
    import Schedule.Speed._
    import lila.i18n.I18nKeys.tourname._
    if (variant.standard && position.isEmpty)
      (conditions.minRating, conditions.maxRating) match {
        case (None, None) =>
          (freq, speed) match {
            case (Hourly, Rapid) if full      => hourlyRapidArena.txt()
            case (Hourly, Rapid)              => hourlyRapid.txt()
            case (Hourly, speed) if full      => hourlyXArena.txt(speed.name)
            case (Hourly, speed)              => hourlyX.txt(speed.name)
            case (Daily, Rapid) if full       => dailyRapidArena.txt()
            case (Daily, Rapid)               => dailyRapid.txt()
            case (Daily, Classical) if full   => dailyClassicalArena.txt()
            case (Daily, Classical)           => dailyClassical.txt()
            case (Daily, speed) if full       => dailyXArena.txt(speed.name)
            case (Daily, speed)               => dailyX.txt(speed.name)
            case (Eastern, Rapid) if full     => easternRapidArena.txt()
            case (Eastern, Rapid)             => easternRapid.txt()
            case (Eastern, Classical) if full => easternClassicalArena.txt()
            case (Eastern, Classical)         => easternClassical.txt()
            case (Eastern, speed) if full     => easternXArena.txt(speed.name)
            case (Eastern, speed)             => easternX.txt(speed.name)
            case (Weekly, Rapid) if full      => weeklyRapidArena.txt()
            case (Weekly, Rapid)              => weeklyRapid.txt()
            case (Weekly, Classical) if full  => weeklyClassicalArena.txt()
            case (Weekly, Classical)          => weeklyClassical.txt()
            case (Weekly, speed) if full      => weeklyXArena.txt(speed.name)
            case (Weekly, speed)              => weeklyX.txt(speed.name)
            case (Monthly, Rapid) if full     => monthlyRapidArena.txt()
            case (Monthly, Rapid)             => monthlyRapid.txt()
            case (Monthly, Classical) if full => monthlyClassicalArena.txt()
            case (Monthly, Classical)         => monthlyClassical.txt()
            case (Monthly, speed) if full     => monthlyXArena.txt(speed.name)
            case (Monthly, speed)             => monthlyX.txt(speed.name)
            case (Yearly, Rapid) if full      => yearlyRapidArena.txt()
            case (Yearly, Rapid)              => yearlyRapid.txt()
            case (Yearly, Classical) if full  => yearlyClassicalArena.txt()
            case (Yearly, Classical)          => yearlyClassical.txt()
            case (Yearly, speed) if full      => yearlyXArena.txt(speed.name)
            case (Yearly, speed)              => yearlyX.txt(speed.name)
            case (Shield, Rapid) if full      => rapidShieldArena.txt()
            case (Shield, Rapid)              => rapidShield.txt()
            case (Shield, Classical) if full  => classicalShieldArena.txt()
            case (Shield, Classical)          => classicalShield.txt()
            case (Shield, speed) if full      => xShieldArena.txt(speed.name)
            case (Shield, speed)              => xShield.txt(speed.name)
            case _ if full                    => xArena.txt(s"${freq.toString} ${speed.name}")
            case _                            => s"${freq.toString} ${speed.name}"
          }
        case (Some(_), _) if full   => eliteXArena.txt(speed.name)
        case (Some(_), _)           => eliteX.txt(speed.name)
        case (_, Some(max)) if full => s"<${max.rating} ${xArena.txt(speed.name)}"
        case (_, Some(max))         => s"<${max.rating} ${speed.name}"
      }
    else if (variant.standard) {
      val n = position.flatMap(sfen => Thematic.bySfen(sfen, variant)).fold(speed.name) { pos =>
        s"${pos.fullName} ${speed.name}"
      }
      if (full) xArena.txt(n) else n
    } else
      freq match {
        case Hourly if full  => hourlyXArena.txt(variant.name)
        case Hourly          => hourlyX.txt(variant.name)
        case Daily if full   => dailyXArena.txt(variant.name)
        case Daily           => dailyX.txt(variant.name)
        case Eastern if full => easternXArena.txt(variant.name)
        case Eastern         => easternX.txt(variant.name)
        case Weekly if full  => weeklyXArena.txt(variant.name)
        case Weekly          => weeklyX.txt(variant.name)
        case Monthly if full => monthlyXArena.txt(variant.name)
        case Monthly         => monthlyX.txt(variant.name)
        case Yearly if full  => yearlyXArena.txt(variant.name)
        case Yearly          => yearlyX.txt(variant.name)
        case Shield if full  => xShieldArena.txt(variant.name)
        case Shield          => xShield.txt(variant.name)
        case _ =>
          val n = s"${freq.name} ${variant.name}"
          if (full) xArena.txt(n) else n
      }
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

  def plan                                  = Schedule.Plan(this, None)
  def plan(build: Tournament => Tournament) = Schedule.Plan(this, build.some)

  override def toString = s"$freq $variant $speed $conditions $at"
}

object Schedule {

  case class Plan(schedule: Schedule, buildFunc: Option[Tournament => Tournament]) {

    def build: Tournament = {
      val t = Tournament.scheduleAs(addCondition(schedule), durationFor(schedule))
      buildFunc.foldRight(t) { _(_) }
    }

    def map(f: Tournament => Tournament) =
      copy(
        buildFunc = buildFunc.fold(f)(f.compose).some
      )
  }

  sealed abstract class Freq(val id: Int, val importance: Int) extends Ordered[Freq] {

    val name = toString.toLowerCase

    def compare(other: Freq) = Integer.compare(importance, other.importance)

    def isDaily          = this == Schedule.Freq.Daily
    def isDailyOrBetter  = this >= Schedule.Freq.Daily
    def isWeeklyOrBetter = this >= Schedule.Freq.Weekly
  }
  object Freq {
    case object Hourly   extends Freq(10, 10)
    case object Daily    extends Freq(20, 20)
    case object Eastern  extends Freq(30, 15)
    case object Weekly   extends Freq(40, 40)
    case object Weekend  extends Freq(41, 41)
    case object Monthly  extends Freq(50, 50)
    case object Shield   extends Freq(51, 51)
    case object Marathon extends Freq(60, 60)
    case object ExperimentalMarathon extends Freq(61, 55) { // for DB BC
      override val name = "Experimental Marathon"
    }
    case object Yearly extends Freq(70, 70)
    case object Unique extends Freq(90, 59)
    val all: List[Freq] = List(
      Hourly,
      Daily,
      Eastern,
      Weekly,
      Weekend,
      Monthly,
      Shield,
      Marathon,
      ExperimentalMarathon,
      Yearly,
      Unique
    )
    def apply(name: String) = all.find(_.name == name)
    def byId(id: Int)       = all.find(_.id == id)
  }

  sealed abstract class Speed(val id: Int) {
    val name = toString
    val key  = lila.common.String lcfirst name
  }
  object Speed {
    case object UltraBullet extends Speed(5)
    case object HyperBullet extends Speed(10)
    case object Bullet      extends Speed(20)
    case object SuperBlitz  extends Speed(25)
    case object Blitz       extends Speed(30)
    case object HyperRapid  extends Speed(40)
    case object Rapid       extends Speed(50)
    case object Classical   extends Speed(60)
    val all: List[Speed] =
      List(UltraBullet, HyperBullet, Bullet, SuperBlitz, Blitz, HyperRapid, Rapid, Classical)
    val mostPopular: List[Speed] = List(Bullet, Blitz, Rapid, Classical)
    def apply(key: String) = all.find(_.key == key) orElse all.find(_.key.toLowerCase == key.toLowerCase)
    def byId(id: Int)      = all find (_.id == id)
    def similar(s1: Speed, s2: Speed) =
      (s1, s2) match {
        // Similar speed tournaments should not be simultaneously scheduled
        case (a, b) if a == b                            => true
        case (Bullet, SuperBlitz) | (SuperBlitz, Bullet) => true
        case (Blitz, HyperRapid) | (HyperRapid, Blitz)   => true
        case _                                           => false
      }
    def fromClock(clock: shogi.Clock.Config) = {
      val time = clock.estimateTotalSeconds
      if (time < 60) UltraBullet
      else if (time < 180) HyperBullet
      else if (time < 300) Bullet
      else if (time < 450) SuperBlitz
      else if (time < 600) Blitz
      else if (time < 1000) HyperRapid
      else if (time < 1500) Rapid
      else Classical
    }
    def toPerfType(speed: Speed) =
      speed match {
        case UltraBullet          => PerfType.UltraBullet
        case HyperBullet | Bullet => PerfType.Bullet
        case SuperBlitz | Blitz   => PerfType.Blitz
        case HyperRapid | Rapid   => PerfType.Rapid
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

  private[tournament] def durationFor(s: Schedule): Int = {
    import Freq._, Speed._
    import shogi.variant._

    (s.freq, s.variant, s.speed) match {

      case (Hourly, _, UltraBullet | HyperBullet | Bullet) => 27
      case (Hourly, _, SuperBlitz | Blitz | HyperRapid)    => 57
      case (Hourly, _, Rapid) if s.hasMaxRating            => 57
      case (Hourly, _, Rapid | Classical)                  => 117

      case (Daily | Eastern, Standard, Blitz)      => 120
      case (Daily | Eastern, Standard, HyperRapid) => 150
      case (Daily | Eastern, _, HyperRapid)        => 120
      case (Daily | Eastern, _, Rapid | Classical) => 180
      case (Daily | Eastern, _, _)                 => 75

      case (Weekly, Minishogi | Kyotoshogi, _)             => 60
      case (Weekly, _, UltraBullet | HyperBullet | Bullet) => 60 * 1 + 30
      case (Weekly, _, SuperBlitz | Blitz)                 => 60 * 2
      case (Weekly, _, HyperRapid | Rapid)                 => 60 * 3
      case (Weekly, _, Classical)                          => 60 * 4

      case (Weekend, _, UltraBullet | HyperBullet | Bullet) => 60 * 1 + 30
      case (Weekend, _, SuperBlitz | Blitz)                 => 60 * 2
      case (Weekend, _, HyperRapid)                         => 60 * 2
      case (Weekend, _, Rapid)                              => 60 * 3
      case (Weekend, _, Classical)                          => 60 * 4

      case (Monthly, Minishogi | Kyotoshogi, _) => 60 * 2
      case (Monthly, _, UltraBullet)            => 60 * 2
      case (Monthly, _, HyperBullet | Bullet)   => 60 * 3
      case (Monthly, _, SuperBlitz | Blitz)     => 60 * 3 + 30
      case (Monthly, _, HyperRapid)             => 60 * 4
      case (Monthly, _, Rapid)                  => 60 * 5
      case (Monthly, _, Classical)              => 60 * 6

      case (Shield, _, UltraBullet)          => 60 * 1 + 30
      case (Shield, _, HyperBullet | Bullet) => 60 * 2
      case (Shield, _, SuperBlitz | Blitz)   => 60 * 3
      case (Shield, _, HyperRapid)           => 60 * 4
      case (Shield, _, Rapid)                => 60 * 6
      case (Shield, _, Classical)            => 60 * 8

      case (Yearly, Minishogi | Kyotoshogi, _)             => 60 * 2
      case (Yearly, _, UltraBullet | HyperBullet | Bullet) => 60 * 2
      case (Yearly, _, SuperBlitz | Blitz)                 => 60 * 3
      case (Yearly, _, HyperRapid)                         => 60 * 4
      case (Yearly, _, Rapid)                              => 60 * 6
      case (Yearly, _, Classical)                          => 60 * 8

      case (Marathon, _, _)             => 60 * 24 // lol
      case (ExperimentalMarathon, _, _) => 60 * 3

      case (Unique, _, _) => 60 * 6
      case (_, _, _)      => 60 * 1
    }
  }

  private val standardIncHours         = Set(1, 7, 13, 19)
  private def standardInc(s: Schedule) = standardIncHours(s.at.getHourOfDay)

  private[tournament] def clockFor(s: Schedule) = {
    import Freq._, Speed._
    import shogi.variant._

    val TC = shogi.Clock.Config

    (s.freq, s.variant, s.speed) match {
      // Special cases.
      case (Hourly, Standard, Blitz) if standardInc(s) => TC(3 * 60, 2, 0, 1)

      case (Shield, variant, Blitz) if !variant.standard => TC(5 * 60, 0, 10, 1)

      case (_, _, UltraBullet) => TC(30, 0, 0, 1)       //      30
      case (_, _, HyperBullet) => TC(0, 0, 5, 1)        //            5 * 25
      case (_, _, Bullet)      => TC(0, 0, 10, 1)       //           10 * 25
      case (_, _, SuperBlitz)  => TC(3 * 60, 0, 10, 1)  //  3 * 60 + 10 * 25
      case (_, _, Blitz)       => TC(5 * 60, 0, 10, 1)  //  5 * 60 + 10 * 25
      case (_, _, HyperRapid)  => TC(5 * 60, 0, 15, 1)  //  5 * 60 + 15 * 25
      case (_, _, Rapid)       => TC(10 * 60, 0, 15, 1) // 10 * 60 + 15 * 25
      case (_, _, Classical)   => TC(15 * 60, 0, 30, 1) // 15 * 60 + 30 * 25
    }
  }
  private[tournament] def addCondition(s: Schedule) =
    s.copy(conditions = conditionFor(s))

  private[tournament] def conditionFor(s: Schedule) =
    if (s.conditions.relevant) s.conditions
    else {
      import Freq._, Speed._

      val nbRatedGame = (s.freq, s.speed) match {

        case (Hourly | Daily | Eastern, HyperBullet | Bullet) => 0
        case (Hourly | Daily | Eastern, SuperBlitz | Blitz)   => 0
        case (Hourly | Daily | Eastern, HyperRapid | Rapid)   => 0

        case (Weekly | Weekend | Monthly, HyperBullet | Bullet) => 1
        case (Weekly | Weekend | Monthly, SuperBlitz | Blitz)   => 1
        case (Weekly | Weekend | Monthly, HyperRapid | Rapid)   => 1

        case (Shield, _) => 3

        case _ => 0
      }

      val minRating = (s.freq, s.variant) match {
        // case (Weekend, _)                        => 2200
        case _ => 0
      }

      Condition.All(
        nbRatedGame = nbRatedGame.some.filter(0 <).map {
          Condition.NbRatedGame(s.perfType.some, _)
        },
        minRating = minRating.some.filter(0 <).map {
          Condition.MinRating(s.perfType, _)
        },
        maxRating = none,
        titled = none,
        teamMember = none
      )
    }
}
