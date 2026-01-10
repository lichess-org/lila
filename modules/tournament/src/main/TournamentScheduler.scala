package lila.tournament

import chess.StartingPosition
import chess.IntRating

import java.time.DayOfWeek.*
import java.time.Month.*
import java.time.temporal.TemporalAdjusters
import java.time.{ LocalDate, LocalDateTime, LocalTime }

import lila.common.LilaScheduler
import lila.core.i18n.Translator
import lila.gathering.Condition

final private class TournamentScheduler(tournamentRepo: TournamentRepo)(using
    Executor,
    Scheduler,
    Translator
):

  LilaScheduler("TournamentScheduler", _.Every(5.minutes), _.AtMost(1.minute), _.Delay(1.minute)):
    given play.api.i18n.Lang = lila.core.i18n.defaultLang
    tournamentRepo.scheduledUnfinished.flatMap: dbScheds =>
      try
        val newPlans = TournamentScheduler.allWithConflicts()
        val tourneysToAdd = PlanBuilder.getNewTourneys(dbScheds, newPlans)
        tournamentRepo.insert(tourneysToAdd).logFailure(logger)
      catch
        case e: Exception =>
          logger.error(s"failed to schedule all: ${e.getMessage}")
          funit

private object TournamentScheduler:

  import Schedule.Freq.*
  import Schedule.Speed.*
  import Schedule.Plan
  import chess.variant.*

  /* Month plan:
   * First week: Shield standard tournaments
   * Second week: Yearly tournament
   * Third week: Shield variant tournaments
   * Last week: Monthly tournaments
   */

  // def marathonDates = List(
  // Spring -> Saturday of the weekend after Orthodox Easter Sunday
  // Summer -> first Saturday of August
  // Autumn -> Saturday of weekend before the weekend Halloween falls on (c.f. half-term holidays)
  // Winter -> 28 December, convenient day in the space between Boxing Day and New Year's Day
  // )
  def allWithConflicts(rightNow: LocalDateTime = nowDateTime): List[Plan] =
    val today = rightNow.date
    val tomorrow = today.plusDays(1)
    val startOfYear = today.withDayOfYear(1)

    class OfMonth(fromNow: Int):
      val firstDay = today.plusMonths(fromNow).withDayOfMonth(1)
      val lastDay = firstDay.adjust(TemporalAdjusters.lastDayOfMonth)
      val firstWeek = firstDay.plusDays(7 - (firstDay.getDayOfWeek.getValue - 1))
      val secondWeek = firstWeek.plusDays(7)
      val thirdWeek = secondWeek.plusDays(7)
      val lastWeek = lastDay.minusDays(lastDay.getDayOfWeek.getValue - 1)
    val thisMonth = OfMonth(0)
    val nextMonth = OfMonth(1)

    def nextDayOfWeek(n: Int) = today.plusDays((n + 7 - today.getDayOfWeek.getValue) % 7)
    val nextMonday = nextDayOfWeek(1)
    val nextTuesday = nextDayOfWeek(2)
    val nextWednesday = nextDayOfWeek(3)
    val nextThursday = nextDayOfWeek(4)
    val nextFriday = nextDayOfWeek(5)
    val nextSaturday = nextDayOfWeek(6)
    val nextSunday = nextDayOfWeek(7)

    def secondWeekOf(month: java.time.Month): LocalDate =
      val start = startOfYear.withMonth(month.getValue).pipe(orNextYearDate)
      start.plusDays(15 - start.getDayOfWeek.getValue)

    def orTomorrow(date: LocalDateTime) = if date.isBefore(rightNow) then date.plusDays(1) else date
    def orNextWeek(date: LocalDateTime) = if date.isBefore(rightNow) then date.plusWeeks(1) else date
    def orNextYear(date: LocalDateTime) = if date.isBefore(rightNow) then date.plusYears(1) else date
    def orNextYearDate(date: LocalDate) = if date.isBefore(today) then date.plusYears(1) else date

    val isHalloween = today.getDayOfMonth == 31 && today.getMonth == OCTOBER

    def openingAt(offset: Int): StartingPosition =
      val positions = StartingPosition.featurable
      positions((today.getDayOfYear + offset) % positions.size)

    val farFuture = today.plusMonths(7).atStartOfDay

    val birthday = LocalDate.of(2010, 6, 20)

    extension (date: LocalDate)
      def withDayOfWeek(day: java.time.DayOfWeek): LocalDate =
        date.adjust(TemporalAdjusters.nextOrSame(day))

    // all dates UTC
    List(
      List( // legendary tournaments!
        at(birthday.withYear(today.getYear), 12).pipe(orNextYear).pipe { date =>
          val yo = date.getYear - 2010
          Schedule(Unique, Rapid, Standard, none, date).plan {
            _.copy(
              name = s"${date.getYear} Lichess Anniversary",
              minutes = 12 * 60,
              description = s"""
We've had $yo great chess years together!

Thank you all, you rock!""".some,
              spotlight = Spotlight(
                headline = s"$yo years of free chess!",
                homepageHours = 24.some
              ).some
            )
          }
        }
      ),
      List( // yearly tournaments!
        secondWeekOf(JANUARY).withDayOfWeek(MONDAY) -> Bullet,
        secondWeekOf(FEBRUARY).withDayOfWeek(TUESDAY) -> SuperBlitz,
        secondWeekOf(MARCH).withDayOfWeek(WEDNESDAY) -> Blitz,
        secondWeekOf(APRIL).withDayOfWeek(THURSDAY) -> Rapid,
        secondWeekOf(MAY).withDayOfWeek(FRIDAY) -> Classical,
        secondWeekOf(JUNE).withDayOfWeek(SATURDAY) -> HyperBullet,
        secondWeekOf(JULY).withDayOfWeek(MONDAY) -> Bullet,
        secondWeekOf(AUGUST).withDayOfWeek(TUESDAY) -> SuperBlitz,
        secondWeekOf(SEPTEMBER).withDayOfWeek(WEDNESDAY) -> Blitz,
        secondWeekOf(OCTOBER).withDayOfWeek(THURSDAY) -> Rapid,
        secondWeekOf(NOVEMBER).withDayOfWeek(FRIDAY) -> Classical,
        secondWeekOf(DECEMBER).withDayOfWeek(SATURDAY) -> HyperBullet
      ).flatMap: (day, speed) =>
        at(day, 17).some.filter(farFuture.isAfter).map { date =>
          Schedule(Yearly, speed, Standard, none, date).plan
        },
      List( // yearly variant tournaments!
        secondWeekOf(JANUARY).withDayOfWeek(WEDNESDAY) -> Chess960,
        secondWeekOf(FEBRUARY).withDayOfWeek(THURSDAY) -> Crazyhouse,
        secondWeekOf(MARCH).withDayOfWeek(FRIDAY) -> KingOfTheHill,
        secondWeekOf(APRIL).withDayOfWeek(SATURDAY) -> RacingKings,
        secondWeekOf(MAY).withDayOfWeek(MONDAY) -> Antichess,
        secondWeekOf(JUNE).withDayOfWeek(TUESDAY) -> Atomic,
        secondWeekOf(JULY).withDayOfWeek(WEDNESDAY) -> Horde,
        secondWeekOf(AUGUST).withDayOfWeek(THURSDAY) -> ThreeCheck
      ).flatMap: (day, variant) =>
        at(day, 17).some.filter(farFuture.isAfter).map { date =>
          Schedule(Yearly, SuperBlitz, variant, none, date).plan
        },
      List(thisMonth, nextMonth).flatMap { month =>
        List(
          List( // monthly standard tournaments!
            month.lastWeek.withDayOfWeek(MONDAY) -> Bullet,
            month.lastWeek.withDayOfWeek(TUESDAY) -> SuperBlitz,
            month.lastWeek.withDayOfWeek(WEDNESDAY) -> Blitz,
            month.lastWeek.withDayOfWeek(THURSDAY) -> Rapid,
            month.lastWeek.withDayOfWeek(FRIDAY) -> Classical,
            month.lastWeek.withDayOfWeek(SATURDAY) -> HyperBullet,
            month.lastWeek.withDayOfWeek(SUNDAY) -> UltraBullet
          ).map: (day, speed) =>
            at(day, 17).pipe: date =>
              Schedule(Monthly, speed, Standard, none, date).plan,
          List( // monthly variant tournaments!
            month.lastWeek.withDayOfWeek(MONDAY) -> Chess960,
            month.lastWeek.withDayOfWeek(TUESDAY) -> Crazyhouse,
            month.lastWeek.withDayOfWeek(WEDNESDAY) -> KingOfTheHill,
            month.lastWeek.withDayOfWeek(THURSDAY) -> RacingKings,
            month.lastWeek.withDayOfWeek(FRIDAY) -> Antichess,
            month.lastWeek.withDayOfWeek(SATURDAY) -> Atomic,
            month.lastWeek.withDayOfWeek(SUNDAY) -> Horde,
            month.lastWeek.withDayOfWeek(SUNDAY) -> ThreeCheck
          ).map: (day, variant) =>
            at(day, 19).pipe: date =>
              Schedule(
                Monthly,
                variant match
                  case Chess960 => ChillBlitz
                  case Crazyhouse => Blitz
                  case _ => SuperBlitz,
                variant,
                none,
                date
              ).plan,
          List( // shield tournaments!
            month.firstWeek.withDayOfWeek(MONDAY) -> Bullet,
            month.firstWeek.withDayOfWeek(TUESDAY) -> SuperBlitz,
            month.firstWeek.withDayOfWeek(WEDNESDAY) -> Blitz,
            month.firstWeek.withDayOfWeek(THURSDAY) -> Rapid,
            month.firstWeek.withDayOfWeek(FRIDAY) -> Classical,
            month.firstWeek.withDayOfWeek(SATURDAY) -> HyperBullet,
            month.firstWeek.withDayOfWeek(SUNDAY) -> UltraBullet
          ).map: (day, speed) =>
            at(day, 16).pipe: date =>
              Schedule(Shield, speed, Standard, none, date).plan(TournamentShield.make(speed.toString)),
          List( // shield variant tournaments!
            month.secondWeek.withDayOfWeek(SUNDAY) -> Chess960,
            month.thirdWeek.withDayOfWeek(MONDAY) -> Crazyhouse,
            month.thirdWeek.withDayOfWeek(TUESDAY) -> KingOfTheHill,
            month.thirdWeek.withDayOfWeek(WEDNESDAY) -> RacingKings,
            month.thirdWeek.withDayOfWeek(THURSDAY) -> Antichess,
            month.thirdWeek.withDayOfWeek(FRIDAY) -> Atomic,
            month.thirdWeek.withDayOfWeek(SATURDAY) -> Horde,
            month.thirdWeek.withDayOfWeek(SUNDAY) -> ThreeCheck
          ).map: (day, variant) =>
            at(day, 16).pipe: date =>
              Schedule(Shield, Blitz, variant, none, date).plan(TournamentShield.make(variant.name))
        ).flatten
      },
      List( // weekly standard tournaments!
        nextMonday -> Bullet,
        nextTuesday -> SuperBlitz,
        nextWednesday -> Blitz,
        nextThursday -> Rapid,
        nextFriday -> Classical,
        nextSaturday -> HyperBullet,
        nextSunday -> UltraBullet
      ).map: (day, speed) =>
        at(day, 17).pipe: date =>
          Schedule(Weekly, speed, Standard, none, date.pipe(orNextWeek)).plan,
      List( // weekly variant tournaments!
        nextMonday -> ThreeCheck,
        nextTuesday -> Crazyhouse,
        nextWednesday -> KingOfTheHill,
        nextThursday -> RacingKings,
        nextFriday -> Antichess,
        nextSaturday -> Atomic,
        nextSunday -> Horde,
        nextSunday -> Chess960
      ).map: (day, variant) =>
        at(day, 19).pipe: date =>
          Schedule(
            Weekly,
            variant match
              case Chess960 => Rapid
              case Crazyhouse => Blitz
              case _ => SuperBlitz,
            variant,
            none,
            date.pipe(orNextWeek)
          ).plan,
      List( // week-end elite tournaments!
        nextSaturday -> SuperBlitz,
        nextSunday -> Bullet
      ).map: (day, speed) =>
        at(day, 17).pipe: date =>
          Schedule(Weekend, speed, Standard, none, date).plan,
      // Note: these should be scheduled close to the hour of weekly or weekend tournaments
      // to avoid two dailies being cancelled in a row from a single higher importance tourney
      List( // daily tournaments!
        at(today, 16).pipe: date =>
          Schedule(Daily, Bullet, Standard, none, date.pipe(orTomorrow)).plan,
        at(today, 17).pipe: date =>
          Schedule(Daily, SuperBlitz, Standard, none, date.pipe(orTomorrow)).plan,
        at(today, 18).pipe: date =>
          Schedule(Daily, Blitz, Standard, none, date.pipe(orTomorrow)).plan,
        at(today, 19).pipe: date =>
          Schedule(Daily, Rapid, Standard, none, date.pipe(orTomorrow)).plan,
        at(today, 20).pipe: date =>
          Schedule(Daily, HyperBullet, Standard, none, date.pipe(orTomorrow)).plan,
        at(today, 21).pipe: date =>
          Schedule(Daily, UltraBullet, Standard, none, date.pipe(orTomorrow)).plan
      ),
      // Note: these should be scheduled close to the hour of weekly variant tournaments
      // to avoid two dailies being cancelled in a row from a single higher importance tourney
      List( // daily variant tournaments!
        at(today, 18).pipe: date =>
          Schedule(Daily, Blitz, Crazyhouse, none, date.pipe(orTomorrow)).plan,
        at(today, 18).pipe: date =>
          Schedule(Daily, SuperBlitz, Atomic, none, date.pipe(orTomorrow)).plan,
        at(today, 19).pipe: date =>
          Schedule(Daily, Blitz, Chess960, none, date.pipe(orTomorrow)).plan,
        at(today, 19).pipe: date =>
          Schedule(Daily, SuperBlitz, Antichess, none, date.pipe(orTomorrow)).plan,
        at(tomorrow, 20).pipe: date =>
          Schedule(Daily, SuperBlitz, ThreeCheck, none, date).plan,
        at(tomorrow, 20).pipe: date =>
          Schedule(Daily, SuperBlitz, Horde, none, date).plan,
        at(today, 21).pipe: date =>
          Schedule(Daily, SuperBlitz, KingOfTheHill, none, date.pipe(orTomorrow)).plan,
        at(today, 21).pipe: date =>
          Schedule(Daily, SuperBlitz, RacingKings, none, date).plan
      ),
      List( // eastern tournaments!
        at(today, 4).pipe: date =>
          Schedule(Eastern, Bullet, Standard, none, date.pipe(orTomorrow)).plan,
        at(today, 5).pipe: date =>
          Schedule(Eastern, SuperBlitz, Standard, none, date.pipe(orTomorrow)).plan,
        at(today, 6).pipe: date =>
          Schedule(Eastern, Blitz, Standard, none, date.pipe(orTomorrow)).plan
      ), {
        {
          for
            halloween <- StartingPosition.presets.halloween
            frankenstein <- StartingPosition.presets.frankenstein
            if isHalloween // replace more thematic tournaments on halloween
          yield List(
            1 -> halloween,
            5 -> frankenstein,
            9 -> halloween,
            13 -> frankenstein,
            17 -> halloween,
            21 -> frankenstein
          )
        } |
          List( // random opening replaces hourly 3 times a day
            3 -> openingAt(offset = 2),
            11 -> openingAt(offset = 1),
            19 -> openingAt(offset = 0)
          )
      }.flatMap: (hour, opening) =>
        List(
          atOption(today, hour).map: date =>
            Schedule(Hourly, Bullet, Standard, opening.fen.some, date.pipe(orTomorrow)).plan,
          atOption(today, hour + 1).map: date =>
            Schedule(Hourly, SuperBlitz, Standard, opening.fen.some, date.pipe(orTomorrow)).plan,
          atOption(today, hour + 2).map: date =>
            Schedule(Hourly, Blitz, Standard, opening.fen.some, date.pipe(orTomorrow)).plan,
          (hour < 21).so(atOption(today, hour + 3)).map { date =>
            Schedule(Hourly, Rapid, Standard, opening.fen.some, date.pipe(orTomorrow)).plan
          }
        ).flatten,
      // hourly standard tournaments!
      (-1 to 6).toList
        .flatMap { hourDelta =>
          val when = atTopOfHour(rightNow, hourDelta)
          List(
            Schedule(Hourly, HyperBullet, Standard, none, when),
            Schedule(Hourly, UltraBullet, Standard, none, when.withMinute(30)),
            Schedule(Hourly, Bullet, Standard, none, when),
            Schedule(Hourly, Bullet, Standard, none, when.withMinute(30)),
            Schedule(Hourly, SuperBlitz, Standard, none, when),
            Schedule(Hourly, Blitz, Standard, none, when)
          ) ::: {
            (when.getHour % 2 == 0).so(List(Schedule(Hourly, Rapid, Standard, none, when)))
          }
        }
        .map(_.plan),
      // hourly limited tournaments!
      (-1 to 6).toList
        .flatMap { hourDelta =>
          val when = atTopOfHour(rightNow, hourDelta)
          val speed = when.getHour % 4 match
            case 0 => Bullet
            case 1 => SuperBlitz
            case 2 => Blitz
            case _ => Rapid
          List(1300, 1500, 1700, 2000).map(IntRating(_)).zipWithIndex.flatMap { (rating, hourDelay) =>
            val conditions = TournamentCondition.All(
              nbRatedGame = Condition.NbRatedGame(20).some,
              maxRating = Condition.MaxRating(rating).some,
              minRating = none,
              titled = none,
              teamMember = none,
              accountAge = none,
              allowList = none,
              bots = none
            )
            val finalWhen = when.plusHours(hourDelay)
            List(Schedule(Hourly, speed, Standard, none, finalWhen, conditions).plan) :::
              (speed == Bullet).so:
                List(Schedule(Hourly, Bullet, Standard, none, finalWhen.plusMinutes(30), conditions).plan)
          }
        }
        .map {
          // No berserk for rating-limited tournaments
          // Because berserking lowers the player rating
          _.map { _.copy(noBerserk = true) }
        },

      // fast popular variant tournaments -- 2/3 of the time
      for
        (variant, hourOffset) <- List(Antichess -> 2)
        hourDelta <- (-1 to 6).toList
        when = atTopOfHour(rightNow, hourDelta)
        // Offsets should be balanced mod 3 between all 6 variants at 2/3 hours
        variantCycle = when.getHour + hourOffset
        if variantCycle % 3 != 0
        // assignments for TCs when variantCycle % 3 == 0 don't currently
        // matter, as those hours are excluded from the schedule.
        // Meaning values of 0, 3, 6, 9 do not occur, and there are
        // just 8 relevant hours in the repeating cycle.
        speed = variantCycle % 12 match
          // 8 should be assigned to bullet, so that the hyperbullet check succeeds
          case 2 | 5 | 8 | 11 => Bullet
          case 1 => HippoBullet
          case 4 | 7 => Blitz
          case _ => SuperBlitz
        first = Schedule(Hourly, speed, variant, none, when)
        second = Option.when(speed == Bullet):
          val speed = if variantCycle % 12 == 8 then HyperBullet else Bullet
          Schedule(Hourly, speed, variant, none, when.plusMinutes(30))
        schedule <- first :: second.toList
      yield schedule.plan,

      // medium speed variants
      for
        (variant, hourOffset) <- List(Atomic -> 0, ThreeCheck -> 2)
        hourDelta <- -1 to 6
        when = atTopOfHour(rightNow, hourDelta)
        // Offsets should be balanced mod 3 between all 6 variants at 2/3 hours
        variantCycle = when.getHour + hourOffset
        if variantCycle % 3 != 0
        // assignments for TCs when variantCycle % 3 == 0 don't currently
        // matter, as those hours are excluded from the schedule.
        // Meaning values of 0, 3, 6, 9 do not occur, and there are
        // just 8 relevant hours in the repeating cycle.
        speed = variantCycle % 12 match
          case 2 | 5 => Blitz
          case 1 | 8 => SuperBlitz
          case 4 => HippoBullet
          case 7 | 11 | _ => Bullet
        first = Schedule(Hourly, speed, variant, none, when)
        second = Option.when(speed == Bullet):
          val speed = if variantCycle % 12 == 7 then HyperBullet else Bullet
          Schedule(Hourly, speed, variant, none, when.plusMinutes(30))
        schedule <- first :: second.toList
      yield schedule.plan,

      // slow variant hourlies
      for
        variant <- List(Crazyhouse, KingOfTheHill)
        hourDelta <- -1 to 6
        when = atTopOfHour(rightNow, hourDelta)
        // Offsets should be balanced mod 3 between all 6 variants at 2/3 hours
        if when.getHour % 3 != 2
        // assignments for TCs when variantCycle % 3 == 0 don't currently
        // matter, as those hours are excluded from the schedule.
        // Meaning values of 0, 3, 6, 9 do not occur, and there are
        // just 8 relevant hours in the repeating cycle.
        speed = when.getHour % 12 match
          case 1 | 6 | 9 => Blitz
          case 0 | 4 => SuperBlitz
          case 3 => HippoBullet
          case _ => Bullet
        first = Schedule(Hourly, speed, variant, none, when)
        second = Option.when(speed == Bullet):
          val speed = if when.getHour % 12 == 7 then HyperBullet else Bullet
          Schedule(Hourly, speed, variant, none, when.plusMinutes(30))
        schedule <- first :: second.toList
      yield schedule.plan,

      // just Chess960 ... cause they special
      for
        hourDelta <- -1 to 6
        when = atTopOfHour(rightNow, hourDelta)
        if when.getHour % 3 != 0

        speed = when.getHour % 12 match
          case 1 | 7 => Blitz
          case 2 | 5 | 10 => ChillBlitz
          case 4 => SuperBlitz
          case _ => Bullet
        first = Schedule(Hourly, speed, Chess960, none, when)
        second = Option.when(speed == Bullet):
          Schedule(Hourly, Bullet, Chess960, none, when.plusMinutes(30))
        schedule <- first :: second.toList
      yield schedule.plan,

      // hourly rare variant tournaments
      for
        hourDelta <- -1 to 6
        when = atTopOfHour(rightNow, hourDelta)
        // Avoid grouping TCs by mod 2, so that the distribution doesn't
        // get skewed as we alternate between 2 variants.
        speed = when.getHour % 5 match
          case 0 => SuperBlitz
          case 1 => Blitz
          case 2 => HippoBullet
          case 3 | _ => Bullet
        variant = when.getHour % 2 match
          case 0 => Horde
          case _ => RacingKings
        first = Schedule(Hourly, speed, variant, none, when)
        second = Option.when(speed == Bullet):
          val speed = if when.getHour == 19 then HyperBullet else Bullet
          Schedule(Hourly, speed, variant, none, when.plusMinutes(30))
        schedule <- first :: second.toList
      yield schedule.plan
    ).flatten.filter(_.schedule.at.isAfter(rightNow))

  private def atTopOfHour(rightNow: LocalDateTime, hourDelta: Int): LocalDateTime =
    val withHours = rightNow.plusHours(hourDelta)
    LocalDateTime.of(withHours.date, LocalTime.of(withHours.getHour, 0))

  private type ValidHour = 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 | 15 | 16 | 17 |
    18 | 19 | 20 | 21 | 22 | 23

  /** Get a [[LocalDateTime]].
    *
    * Note: This is safe -- impl throws only when hour is outside 0-23 or if day is null, and neither
    * condition can occur here.
    * {{{
    * assert {
    *   val hourRange = java.time.temporal.ChronoField.HOUR_OF_DAY.range()
    *   hourRange.getMinimum == 0 && hourRange.getMaximum == 23
    * }
    * }}}
    */
  private def at(day: LocalDate, hour: ValidHour): LocalDateTime =
    LocalDateTime.of(day, LocalTime.of(hour, 0))

  /** Get a [[LocalDateTime]].
    *
    * Returns None exactly when hour is outside 0-23 or if minutes is outside 0-59.
    */
  private def atOption(day: LocalDate, hour: Int, minute: Int = 0): Option[LocalDateTime] =
    try LocalDateTime.of(day, LocalTime.of(hour, minute)).some
    catch
      case e: Exception =>
        logger.error(s"Failed to schedule due to invalid time '$hour:$minute'", e)
        None
