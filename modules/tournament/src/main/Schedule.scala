package lila.tournament

import cats.derived.*
import chess.Clock.{ IncrementSeconds, LimitSeconds }
import chess.format.Fen
import chess.variant.Variant

import lila.core.i18n.{ I18nKey, Translate }
import lila.gathering.Condition

case class Schedule(
    freq: Schedule.Freq,
    speed: Schedule.Speed,
    variant: Variant,
    position: Option[Fen.Standard],
    at: LocalDateTime,
    conditions: TournamentCondition.All = TournamentCondition.All.empty
):

  def name(full: Boolean = true)(using Translate): String =
    import Schedule.Freq.*
    import Schedule.Speed.*
    import lila.core.i18n.I18nKey.tourname.*
    if variant.standard && position.isEmpty then
      (conditions.minRating, conditions.maxRating) match
        case (None, None) =>
          (freq, speed) match
            case (Hourly, Rapid) if full      => hourlyRapidArena.txt()
            case (Hourly, Rapid)              => hourlyRapid.txt()
            case (Hourly, speed) if full      => hourlyXArena.txt(speed.trans)
            case (Hourly, speed)              => hourlyX.txt(speed.trans)
            case (Daily, Rapid) if full       => dailyRapidArena.txt()
            case (Daily, Rapid)               => dailyRapid.txt()
            case (Daily, Classical) if full   => dailyClassicalArena.txt()
            case (Daily, Classical)           => dailyClassical.txt()
            case (Daily, speed) if full       => dailyXArena.txt(speed.trans)
            case (Daily, speed)               => dailyX.txt(speed.trans)
            case (Eastern, Rapid) if full     => easternRapidArena.txt()
            case (Eastern, Rapid)             => easternRapid.txt()
            case (Eastern, Classical) if full => easternClassicalArena.txt()
            case (Eastern, Classical)         => easternClassical.txt()
            case (Eastern, speed) if full     => easternXArena.txt(speed.trans)
            case (Eastern, speed)             => easternX.txt(speed.trans)
            case (Weekly, Rapid) if full      => weeklyRapidArena.txt()
            case (Weekly, Rapid)              => weeklyRapid.txt()
            case (Weekly, Classical) if full  => weeklyClassicalArena.txt()
            case (Weekly, Classical)          => weeklyClassical.txt()
            case (Weekly, speed) if full      => weeklyXArena.txt(speed.trans)
            case (Weekly, speed)              => weeklyX.txt(speed.trans)
            case (Monthly, Rapid) if full     => monthlyRapidArena.txt()
            case (Monthly, Rapid)             => monthlyRapid.txt()
            case (Monthly, Classical) if full => monthlyClassicalArena.txt()
            case (Monthly, Classical)         => monthlyClassical.txt()
            case (Monthly, speed) if full     => monthlyXArena.txt(speed.trans)
            case (Monthly, speed)             => monthlyX.txt(speed.trans)
            case (Yearly, Rapid) if full      => yearlyRapidArena.txt()
            case (Yearly, Rapid)              => yearlyRapid.txt()
            case (Yearly, Classical) if full  => yearlyClassicalArena.txt()
            case (Yearly, Classical)          => yearlyClassical.txt()
            case (Yearly, speed) if full      => yearlyXArena.txt(speed.trans)
            case (Yearly, speed)              => yearlyX.txt(speed.trans)
            case (Shield, Rapid) if full      => rapidShieldArena.txt()
            case (Shield, Rapid)              => rapidShield.txt()
            case (Shield, Classical) if full  => classicalShieldArena.txt()
            case (Shield, Classical)          => classicalShield.txt()
            case (Shield, speed) if full      => xShieldArena.txt(speed.trans)
            case (Shield, speed)              => xShield.txt(speed.trans)
            case _ if full                    => xArena.txt(s"${freq.toString} ${speed.trans}")
            case _                            => s"${freq.toString} ${speed.trans}"
        case (Some(_), _) if full   => eliteXArena.txt(speed.trans)
        case (Some(_), _)           => eliteX.txt(speed.trans)
        case (_, Some(max)) if full => s"≤${max.rating} ${xArena.txt(speed.trans)}"
        case (_, Some(max))         => s"≤${max.rating} ${speed.trans}"
    else if variant.standard then
      val n = position.flatMap(lila.gathering.Thematic.byFen).fold(speed.trans) { pos =>
        s"${pos.family.name} ${speed.trans}"
      }
      if full then xArena.txt(n) else n
    else
      freq match
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
          if full then xArena.txt(n) else n

  def day = at.withTimeAtStartOfDay

  /** Absolute start time of the schedule.
    */
  def atInstant = at.instant

  def sameSpeed(other: Schedule) = speed == other.speed

  def similarSpeed(other: Schedule) = Schedule.Speed.similar(speed, other.speed)

  def sameVariant(other: Schedule) = variant.id == other.variant.id

  def sameVariantAndSpeed(other: Schedule) = sameVariant(other) && sameSpeed(other)

  def sameFreq(other: Schedule) = freq == other.freq

  def sameConditions(other: Schedule) = conditions == other.conditions

  def sameMaxRating(other: Schedule) = conditions.sameMaxRating(other.conditions)

  def similarConditions(other: Schedule) = conditions.similar(other.conditions)

  def sameDay(other: Schedule) = day == other.day

  def hasMaxRating = conditions.maxRating.isDefined

  def similarTo(other: Schedule) =
    similarSpeed(other) && sameVariant(other) && sameFreq(other) && sameConditions(other)

  def perfKey: PerfKey = PerfKey.byVariant(variant) | Schedule.Speed.toPerfKey(speed)

  def plan                                  = Schedule.Plan(this, None)
  def plan(build: Tournament => Tournament) = Schedule.Plan(this, build.some)

  override def toString =
    s"${atInstant} $freq ${variant.key} ${speed.key}(${Schedule.clockFor(this)}) $conditions $position"

object Schedule:

  def uniqueFor(tour: Tournament) =
    Schedule(
      freq = Freq.Unique,
      speed = Speed.fromClock(tour.clock),
      variant = tour.variant,
      position = tour.position,
      at = tour.startsAt.dateTime
    )

  /** Max window for daily or better schedules to be considered overlapping (i.e. if one starts within X hrs
    * of the other ending). Using 11.5 hrs here ensures that at least one daily is always cancelled for the
    * more important event. But, if a higher importance tourney is scheduled nearly opposite of the daily
    * (i.e. 11:00 for a monthly and 00:00 for its daily), two dailies will be cancelled... so don't do this!
    */
  private[tournament] val SCHEDULE_DAILY_OVERLAP_MINS = 690 // 11.5 * 60

  private[tournament] trait ScheduleWithInterval:
    def schedule: Schedule
    def startsAt: Instant
    def duration: java.time.Duration

    def endsAt = startsAt.plus(duration)

    def interval = TimeInterval(startsAt, duration)

    def overlaps(other: ScheduleWithInterval) = interval.overlaps(other.interval)

    // Note: must be kept in sync with [[SchedulerTestHelpers.ExperimentalPruner.pruneConflictsFailOnUsurp]]
    // In particular, pruneConflictsFailOnUsurp filters tourneys that couldn't possibly conflict based
    // on their hours -- so the same logic (overlaps and daily windows) must be used here.
    def conflictsWith(si2: ScheduleWithInterval) =
      val s1 = schedule
      val s2 = si2.schedule
      s1.variant == s2.variant && (
        // prevent daily && weekly within X hours of each other
        if s1.freq.isDailyOrBetter && s2.freq.isDailyOrBetter && s1.sameSpeed(s2) then
          si2.startsAt.minusMinutes(SCHEDULE_DAILY_OVERLAP_MINS).isBefore(endsAt) &&
          startsAt.minusMinutes(SCHEDULE_DAILY_OVERLAP_MINS).isBefore(si2.endsAt)
        else
          (
            s1.variant.exotic ||  // overlapping exotic variant
              s1.hasMaxRating ||  // overlapping same rating limit
              s1.similarSpeed(s2) // overlapping similar
          ) && s1.similarConditions(s2) && overlaps(si2)
      )

    /** Kept in sync with [[conflictsWithFailOnUsurp]].
      */
    def conflictsWith(scheds: Iterable[ScheduleWithInterval]): Boolean =
      scheds.exists(conflictsWith)

    /** Kept in sync with [[conflictsWith]].
      *
      * Raises an exception if a tourney is incorrectly usurped.
      */
    @throws[IllegalStateException]("if a tourney is incorrectly usurped")
    def conflictsWithFailOnUsurp(scheds: Iterable[ScheduleWithInterval]) =
      val conflicts   = scheds.filter(conflictsWith).toSeq
      val okConflicts = conflicts.filter(_.schedule.freq >= schedule.freq)
      if conflicts.nonEmpty && okConflicts.isEmpty then
        throw new IllegalStateException(s"Schedule [$schedule] usurped by ${conflicts}")
      conflicts.nonEmpty

  case class Plan(schedule: Schedule, buildFunc: Option[Tournament => Tournament])
      extends ScheduleWithInterval:

    def build(using Translate): Tournament =
      val t = Tournament.scheduleAs(withConditions(schedule), minutes)
      buildFunc.fold(t) { _(t) }

    def map(f: Tournament => Tournament) = copy(
      buildFunc = buildFunc.fold(f)(f.compose).some
    )

    override def startsAt = schedule.atInstant

    def minutes = durationFor(schedule)

    override def duration = java.time.Duration.ofMinutes(minutes)

  enum Freq(val id: Int, val importance: Int) extends Ordered[Freq] derives Eq:
    case Hourly               extends Freq(10, 10)
    case Daily                extends Freq(20, 20)
    case Eastern              extends Freq(30, 15)
    case Weekly               extends Freq(40, 40)
    case Weekend              extends Freq(41, 41)
    case Monthly              extends Freq(50, 50)
    case Shield               extends Freq(51, 51)
    case Marathon             extends Freq(60, 60)
    case ExperimentalMarathon extends Freq(61, 55) // for DB BC
    case Yearly               extends Freq(70, 70)
    case Unique               extends Freq(90, 59)

    val name = Freq.this.toString.toLowerCase

    def compare(other: Freq) = Integer.compare(importance, other.importance)

    def isDaily          = this == Freq.Daily
    def isDailyOrBetter  = this >= Freq.Daily
    def isWeeklyOrBetter = this >= Freq.Weekly

  object Freq:
    val list: List[Freq] = values.toList
    val byName           = values.mapBy(_.name)
    val byId             = values.mapBy(_.id)

  enum Speed(val id: Int):
    val name = Speed.this.toString
    val key  = lila.common.String.lcfirst(name)
    def trans(using Translate): String = this match
      case Speed.Bullet    => I18nKey.site.bullet.txt()
      case Speed.Blitz     => I18nKey.site.blitz.txt()
      case Speed.Rapid     => I18nKey.site.rapid.txt()
      case Speed.Classical => I18nKey.site.classical.txt()
      case _               => name
    case UltraBullet extends Speed(5)
    case HyperBullet extends Speed(10)
    case Bullet      extends Speed(20)
    case HippoBullet extends Speed(25)
    case SuperBlitz  extends Speed(30)
    case Blitz       extends Speed(40)
    case Rapid       extends Speed(50)
    case Classical   extends Speed(60)
  object Speed:
    val all                      = values.toList
    val mostPopular: List[Speed] = List(Bullet, Blitz, Rapid, Classical)
    val byId                     = values.mapBy(_.id)
    def apply(key: String) = all.find(_.key == key).orElse(all.find(_.key.toLowerCase == key.toLowerCase))
    def similar(s1: Speed, s2: Speed) =
      (s1, s2) match
        case (a, b) if a == b                                        => true
        case (Bullet, HippoBullet) | (HippoBullet, Bullet)           => true
        case (HyperBullet, UltraBullet) | (UltraBullet, HyperBullet) => true
        case _                                                       => false
    def fromClock(clock: chess.Clock.Config) =
      val time = clock.estimateTotalSeconds
      if time < 30 then UltraBullet
      else if time < 60 then HyperBullet
      else if time < 120 then Bullet
      else if time < 180 then HippoBullet
      else if time < 480 then Blitz
      else if time < 1500 then Rapid
      else Classical
    def toPerfKey(speed: Speed) = speed match
      case UltraBullet                        => PerfKey.ultraBullet
      case HyperBullet | Bullet | HippoBullet => PerfKey.bullet
      case SuperBlitz | Blitz                 => PerfKey.blitz
      case Rapid                              => PerfKey.rapid
      case Classical                          => PerfKey.classical

  enum Season:
    case Spring, Summer, Autumn, Winter

  private[tournament] def durationFor(s: Schedule): Int =
    import Freq.*, Speed.*
    import chess.variant.*

    (s.freq, s.variant, s.speed) match

      case (Hourly, _, UltraBullet | HyperBullet | Bullet) => 27
      case (Hourly, _, HippoBullet | SuperBlitz | Blitz)   => 57
      case (Hourly, _, Rapid) if s.hasMaxRating            => 57
      case (Hourly, _, Rapid | Classical)                  => 117

      case (Daily | Eastern, Standard, SuperBlitz) => 90
      case (Daily | Eastern, Standard, Blitz)      => 120
      case (Daily | Eastern, _, Blitz)             => 90
      case (Daily | Eastern, _, Rapid | Classical) => 150
      case (Daily | Eastern, _, _)                 => 60

      case (Weekly, _, UltraBullet | HyperBullet | Bullet) => 60 * 2
      case (Weekly, _, HippoBullet | SuperBlitz | Blitz)   => 60 * 3
      case (Weekly, _, Rapid)                              => 60 * 4
      case (Weekly, _, Classical)                          => 60 * 5

      case (Weekend, Crazyhouse, _)                         => 60 * 2
      case (Weekend, _, UltraBullet | HyperBullet | Bullet) => 90
      case (Weekend, _, HippoBullet | SuperBlitz)           => 60 * 2
      case (Weekend, _, Blitz)                              => 60 * 3
      case (Weekend, _, Rapid)                              => 60 * 4
      case (Weekend, _, Classical)                          => 60 * 5

      case (Monthly, _, UltraBullet)              => 60 * 2
      case (Monthly, _, HyperBullet | Bullet)     => 60 * 3
      case (Monthly, _, HippoBullet | SuperBlitz) => 60 * 3 + 30
      case (Monthly, _, Blitz)                    => 60 * 4
      case (Monthly, _, Rapid)                    => 60 * 5
      case (Monthly, _, Classical)                => 60 * 6

      case (Shield, _, UltraBullet)              => 60 * 3
      case (Shield, _, HyperBullet | Bullet)     => 60 * 4
      case (Shield, _, HippoBullet | SuperBlitz) => 60 * 5
      case (Shield, _, Blitz)                    => 60 * 6
      case (Shield, _, Rapid)                    => 60 * 8
      case (Shield, _, Classical)                => 60 * 10

      case (Yearly, _, UltraBullet | HyperBullet | Bullet) => 60 * 4
      case (Yearly, _, HippoBullet | SuperBlitz)           => 60 * 5
      case (Yearly, _, Blitz)                              => 60 * 6
      case (Yearly, _, Rapid)                              => 60 * 8
      case (Yearly, _, Classical)                          => 60 * 10

      case (Marathon, _, _)             => 60 * 24 // lol
      case (ExperimentalMarathon, _, _) => 60 * 4

      case (Unique, _, _) => 60 * 6

  private val standardIncHours          = Set(1, 7, 13, 19)
  private def standardInc(s: Schedule)  = standardIncHours(s.at.getHour)
  private def zhInc(s: Schedule)        = s.at.getHour % 2 == 0
  private def bottomOfHour(s: Schedule) = s.at.getMinute > 29

  private given Conversion[Int, LimitSeconds]     = LimitSeconds(_)
  private given Conversion[Int, IncrementSeconds] = IncrementSeconds(_)

  private def zhEliteTc(s: Schedule) =
    val TC = chess.Clock.Config
    s.at.getDayOfMonth / 7 match
      case 0 => TC(3 * 60, 0)
      case 1 => TC(1 * 60, 1)
      case 2 => TC(3 * 60, 2)
      case 3 => TC(1 * 60, 0)
      case _ => TC(2 * 60, 0) // for the sporadic 5th Saturday

  private[tournament] def clockFor(s: Schedule) =
    import Freq.*, Speed.*
    import chess.variant.*

    val TC = chess.Clock.Config

    (s.freq, s.variant, s.speed) match
      // Special cases.
      case (Weekend, Crazyhouse, Blitz)                                    => zhEliteTc(s)
      case (Hourly, Crazyhouse, SuperBlitz) if zhInc(s)                    => TC(3 * 60, 1)
      case (Hourly, Crazyhouse, Blitz) if zhInc(s)                         => TC(4 * 60, 2)
      case (Hourly, Standard, Blitz) if standardInc(s)                     => TC(3 * 60, 2)
      case (Hourly, Standard, Bullet) if s.hasMaxRating && bottomOfHour(s) => TC(60, 1)

      case (Shield, variant, Blitz) if variant.exotic => TC(3 * 60, 2)

      case (_, _, UltraBullet) => TC(15, 0)
      case (_, _, HyperBullet) => TC(30, 0)
      case (_, _, Bullet)      => TC(60, 0)
      case (_, _, HippoBullet) => TC(2 * 60, 0)
      case (_, _, SuperBlitz)  => TC(3 * 60, 0)
      case (_, _, Blitz)       => TC(5 * 60, 0)
      case (_, _, Rapid)       => TC(10 * 60, 0)
      case (_, _, Classical)   => TC(20 * 60, 10)

  private[tournament] def withConditions(s: Schedule) = s.copy(conditions = conditionFor(s))

  private[tournament] def conditionFor(s: Schedule) =
    if s.conditions.nonEmpty then s.conditions
    else
      import Freq.*, Speed.*

      val nbRatedGame = ((s.freq, s.variant, s.speed) match
        case (Hourly, variant, _) if variant.exotic => 0

        case (Hourly | Daily | Eastern, _, HyperBullet | Bullet)             => 20
        case (Hourly | Daily | Eastern, _, HippoBullet | SuperBlitz | Blitz) => 15
        case (Hourly | Daily | Eastern, _, Rapid)                            => 10

        case (Weekly | Weekend | Monthly | Shield, _, HyperBullet | Bullet)             => 30
        case (Weekly | Weekend | Monthly | Shield, _, HippoBullet | SuperBlitz | Blitz) => 20
        case (Weekly | Weekend | Monthly | Shield, _, Rapid)                            => 15
        case (Weekly | Weekend | Monthly | Shield, _, Classical)                        => 5

        case _ => 0
      ).some.filter(0 <).map(Condition.NbRatedGame.apply)

      val minRating = ((s.freq, s.variant) match
        case (Weekend, chess.variant.Crazyhouse) => 2100
        case (Weekend, _)                        => 2200
        case _                                   => 0
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
          allowList = none
        )

  /** Given a list of existing schedules and a list of possible new plans, returns a subset of the possible
    * plans that do not conflict with either the existing schedules or with themselves. Intended to produce
    * identical output to [[SchedulerTestHelpers.ExperimentalPruner.pruneConflictsFailOnUsurp]], but this
    * variant is more readable and has lower potential for bugs.
    */
  private[tournament] def pruneConflicts[A <: ScheduleWithInterval](
      existingSchedules: Iterable[ScheduleWithInterval],
      possibleNewPlans: Iterable[A]
  ): List[A] =
    var allPlannedSchedules = existingSchedules.toList
    possibleNewPlans
      .foldLeft(Nil): (newPlans, p) =>
        if p.conflictsWith(allPlannedSchedules) then newPlans
        else
          allPlannedSchedules ::= p
          p :: newPlans
      .reverse
