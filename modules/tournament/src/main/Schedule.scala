package lila.tournament

import cats.derived.*
import chess.Clock.{ IncrementSeconds, LimitSeconds }
import chess.format.Fen
import chess.variant.Variant
import chess.IntRating

import lila.core.i18n.{ I18nKey, Translate }
import lila.gathering.Condition

case class Scheduled(freq: Schedule.Freq, at: LocalDateTime)

case class Schedule(
    freq: Schedule.Freq,
    speed: Schedule.Speed,
    variant: Variant,
    position: Option[Fen.Standard],
    at: LocalDateTime,
    conditions: TournamentCondition.All = TournamentCondition.All.empty
):
  /** Absolute start time of the schedule. */
  def atInstant = at.instant

  def sameSpeed(other: Schedule) = speed == other.speed

  def sameMaxRating(other: Schedule) = conditions.sameMaxRating(other.conditions)

  def sameDay(other: Schedule) = day == other.day
  private def day = at.withTimeAtStartOfDay

  def hasMaxRating = conditions.maxRating.isDefined

  def perfKey: PerfKey = PerfKey.byVariant(variant) | Schedule.Speed.toPerfKey(speed)

  def plan = Schedule.Plan(this, atInstant, None)
  def plan(build: Tournament => Tournament) = Schedule.Plan(this, atInstant, build.some)

  override def toString =
    val initial = if position.isEmpty then "standard" else "position"
    s"${atInstant} $freq ${variant.key} ${speed.key}(${Schedule.clockFor(this)}) $conditions $initial"

object Schedule:

  case class Plan(schedule: Schedule, startsAt: Instant, buildFunc: Option[Tournament => Tournament])
      extends PlanBuilder.ScheduleWithInterval:

    def build(using Translate): Tournament =
      val t = Tournament.scheduleAs(withConditions(schedule), startsAt, minutes)
      buildFunc.fold(t) { _(t) }

    def map(f: Tournament => Tournament) = copy(
      buildFunc = buildFunc.fold(f)(f.compose).some
    )

    def minutes = durationFor(schedule)

    override def duration = java.time.Duration.ofMinutes(minutes)

  enum Freq(val id: Int, val importance: Int) extends Ordered[Freq] derives Eq:
    case Hourly extends Freq(10, 10)
    case Daily extends Freq(20, 20)
    case Eastern extends Freq(30, 15)
    case Weekly extends Freq(40, 40)
    case Weekend extends Freq(41, 41)
    case Monthly extends Freq(50, 50)
    case Shield extends Freq(51, 51)
    case Marathon extends Freq(60, 60)
    case ExperimentalMarathon extends Freq(61, 55) // for DB BC
    case Yearly extends Freq(70, 70)
    case Unique extends Freq(90, 59)

    val name = Freq.this.toString.toLowerCase

    def compare(other: Freq) = Integer.compare(importance, other.importance)

    def isDaily = this == Freq.Daily
    def isDailyOrBetter = this >= Freq.Daily
    def isWeeklyOrBetter = this >= Freq.Weekly

  object Freq:
    val list: List[Freq] = values.toList
    val byName = values.mapBy(_.name)
    val byId = values.mapBy(_.id)

  enum Speed(val id: Int) derives Eq:
    val name = Speed.this.toString
    val key = lila.common.String.lcfirst(name)
    def trans(using Translate): String = this match
      case Speed.Bullet => I18nKey.site.bullet.txt()
      case Speed.Blitz => I18nKey.site.blitz.txt()
      case Speed.Rapid => I18nKey.site.rapid.txt()
      case Speed.Classical => I18nKey.site.classical.txt()
      case _ => name
    case UltraBullet extends Speed(5)
    case HyperBullet extends Speed(10)
    case Bullet extends Speed(20)
    case HippoBullet extends Speed(25)
    case SuperBlitz extends Speed(30)
    case Blitz extends Speed(40)
    case ChillBlitz extends Speed(45)
    case Rapid extends Speed(50)
    case Classical extends Speed(60)
  object Speed:
    val all = values.toList
    val mostPopular: List[Speed] = List(Bullet, Blitz, Rapid, Classical)
    val byId = values.mapBy(_.id)
    def apply(key: String) = all.find(_.key == key).orElse(all.find(_.key.toLowerCase == key.toLowerCase))
    def similar(s1: Speed, s2: Speed) =
      (s1, s2) match
        case (a, b) if a == b => true
        case (Bullet, HippoBullet) | (HippoBullet, Bullet) => true
        case (HyperBullet, UltraBullet) | (UltraBullet, HyperBullet) => true
        case _ => false
    def fromClock(clock: chess.Clock.Config) =
      val time = clock.estimateTotalSeconds
      if time < 30 then UltraBullet
      else if time < 60 then HyperBullet
      else if time < 120 then Bullet
      else if time == 120 then HippoBullet
      else if time <= 220 then SuperBlitz // 3 + 1
      else if time < 480 then Blitz
      else if time < 1500 then Rapid
      else Classical
    def toPerfKey(speed: Speed) = speed match
      case UltraBullet => PerfKey.ultraBullet
      case HyperBullet | Bullet | HippoBullet => PerfKey.bullet
      case SuperBlitz | Blitz | ChillBlitz => PerfKey.blitz
      case Rapid => PerfKey.rapid
      case Classical => PerfKey.classical

  enum Season:
    case Spring, Summer, Autumn, Winter

  private[tournament] def durationFor(s: Schedule): Int =
    import Freq.*, Speed.*
    import chess.variant.*

    (s.freq, s.variant, s.speed) match

      case (Hourly, _, UltraBullet | HyperBullet | Bullet) => 27
      case (Hourly, _, HippoBullet | SuperBlitz | Blitz | ChillBlitz) => 57
      case (Hourly, _, Rapid) if s.hasMaxRating => 57
      case (Hourly, _, Rapid | Classical) => 117

      case (Daily | Eastern, Standard, SuperBlitz) => 90
      case (Daily | Eastern, Standard, Blitz) => 120
      case (Daily | Eastern, _, Blitz | ChillBlitz) => 90
      case (Daily | Eastern, _, Rapid | Classical) => 150
      case (Daily | Eastern, _, _) => 60

      case (Weekly, _, UltraBullet | HyperBullet | Bullet) => 60 * 2
      case (Weekly, _, HippoBullet | SuperBlitz | Blitz | ChillBlitz) => 60 * 3
      case (Weekly, _, Rapid) => 60 * 4
      case (Weekly, _, Classical) => 60 * 5

      case (Weekend, _, UltraBullet | HyperBullet | Bullet) => 90
      case (Weekend, _, HippoBullet | SuperBlitz) => 60 * 2
      case (Weekend, _, Blitz | ChillBlitz) => 60 * 3
      case (Weekend, _, Rapid) => 60 * 4
      case (Weekend, _, Classical) => 60 * 5

      case (Monthly, _, UltraBullet) => 60 * 2
      case (Monthly, _, HyperBullet | Bullet) => 60 * 3
      case (Monthly, _, HippoBullet | SuperBlitz) => 60 * 3 + 30
      case (Monthly, _, Blitz | ChillBlitz) => 60 * 4
      case (Monthly, _, Rapid) => 60 * 5
      case (Monthly, _, Classical) => 60 * 6

      case (Shield, _, UltraBullet) => 60 * 3
      case (Shield, _, HyperBullet | Bullet) => 60 * 4
      case (Shield, _, HippoBullet | SuperBlitz) => 60 * 5
      case (Shield, _, Blitz | ChillBlitz) => 60 * 6
      case (Shield, _, Rapid) => 60 * 8
      case (Shield, _, Classical) => 60 * 10

      case (Yearly, _, UltraBullet | HyperBullet | Bullet) => 60 * 4
      case (Yearly, _, HippoBullet | SuperBlitz) => 60 * 5
      case (Yearly, _, Blitz | ChillBlitz) => 60 * 6
      case (Yearly, _, Rapid) => 60 * 8
      case (Yearly, _, Classical) => 60 * 10

      case (Marathon, _, _) => 60 * 24 // lol
      case (ExperimentalMarathon, _, _) => 60 * 4

      case (Unique, _, _) => 60 * 6

  private val blitzIncHours = Set(1, 13)
  private val rapidIncHours = Set(2)
  private def blitzInc(s: Schedule) = blitzIncHours(s.at.getHour)
  private def rapidInc(s: Schedule) = rapidIncHours(s.at.getHour)
  private def bottomOfHour(s: Schedule) = s.at.getMinute > 29

  private given Conversion[Int, LimitSeconds] = LimitSeconds(_)
  private given Conversion[Int, IncrementSeconds] = IncrementSeconds(_)

  private[tournament] def clockFor(s: Schedule) =
    import Freq.*, Speed.*
    import chess.variant.*

    val TC = chess.Clock.Config

    (s.freq, s.variant, s.speed) match
      // Special cases.
      case (Hourly, Standard, Blitz) if blitzInc(s) => TC(3 * 60, 2)
      case (Hourly, Standard, Rapid) if rapidInc(s) => TC(8 * 60, 2)
      case (Hourly, Standard, Bullet) if s.hasMaxRating && bottomOfHour(s) => TC(60, 1)
      case (_, Chess960, ChillBlitz) => TC(5 * 60, 3)
      case (_, Chess960, Rapid) => TC(10 * 60, 2)
      case (_, variant, Blitz) if variant.exotic => TC(3 * 60, 2)
      case (Hourly, Antichess | Atomic, Bullet) if bottomOfHour(s) => TC(0, 2)
      case (Hourly, variant, HippoBullet) if variant.exotic => TC(60, 2)

      case (_, _, UltraBullet) => TC(15, 0)
      case (_, _, HyperBullet) => TC(30, 0)
      case (_, _, Bullet) => TC(60, 0)
      case (_, _, HippoBullet) => TC(2 * 60, 0)
      case (_, _, SuperBlitz) => TC(3 * 60, 0)
      case (_, _, Blitz) => TC(5 * 60, 0)
      case (_, _, ChillBlitz) => TC(450, 0) // 7.5 * 60
      case (_, _, Rapid) => TC(10 * 60, 0)
      case (_, _, Classical) => TC(20 * 60, 10)

  private[tournament] def withConditions(s: Schedule) = s.copy(conditions = conditionFor(s))

  private[tournament] def conditionFor(s: Schedule) =
    if s.conditions.nonEmpty then s.conditions
    else
      import Freq.*, Speed.*

      val nbRatedGame = ((s.freq, s.variant, s.speed) match
        case (Hourly, variant, _) if variant.exotic => 0

        case (Hourly | Daily | Eastern, _, HyperBullet | Bullet) => 20
        case (Hourly | Daily | Eastern, _, HippoBullet | SuperBlitz | Blitz | ChillBlitz) => 15
        case (Hourly | Daily | Eastern, _, Rapid) => 10

        case (Weekly | Weekend | Monthly | Shield, _, HyperBullet | Bullet) => 30
        case (Weekly | Weekend | Monthly | Shield, _, HippoBullet | SuperBlitz | Blitz | ChillBlitz) => 20
        case (Weekly | Weekend | Monthly | Shield, _, Rapid) => 15
        case (Weekly | Weekend | Monthly | Shield, _, Classical) => 5

        case _ => 0
      ).some.filter(0 <).map(Condition.NbRatedGame.apply)

      val minRating = ((s.freq, s.variant) match
        case (Weekend, chess.variant.Crazyhouse) => 2100
        case (Weekend, _) => 2200
        case _ => 0
      ).some.filter(0 <).map(v => Condition.MinRating(IntRating(v)))

      if nbRatedGame.isEmpty && minRating.isEmpty then TournamentCondition.All.empty
      else
        TournamentCondition.All(
          nbRatedGame = nbRatedGame,
          minRating = minRating,
          maxRating = none,
          titled = none,
          teamMember = none,
          accountAge = none,
          allowList = none,
          bots = none
        )
