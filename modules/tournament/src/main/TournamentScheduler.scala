package lila.tournament

import chess.StartingPosition

import java.time.DayOfWeek.*
import java.time.Month.*
import java.time.temporal.TemporalAdjusters
import java.time.{ LocalDate, LocalDateTime }

import lila.common.LilaScheduler
import lila.core.i18n.Translator
import lila.gathering.Condition

/** This case class (and underlying trait) exists to ensure conflicts are checked against a tournament's true
  * interval, rather than the interval which could be inferred from the tournament's schedule parameter via
  * [[Schedule.durationFor]]
  *
  * Such a mismatch could occur if durationFor is modified and existing tournaments are rehydrated from db.
  * Another source of mismatch is that tourney actual start is tweaked from scheduled start by a random number
  * of seconds (see [[Tournament.scheduleAs]])
  */
private[tournament] case class ConcreteSchedule(
    schedule: Schedule,
    startsAt: Instant,
    duration: java.time.Duration
) extends Schedule.ScheduleWithInterval

private[tournament] case class ConcreteTourney(
    tournament: Tournament,
    schedule: Schedule,
    startsAt: Instant,
    duration: java.time.Duration
) extends Schedule.ScheduleWithInterval

final private class TournamentScheduler(tournamentRepo: TournamentRepo)(using
    Executor,
    Scheduler,
    Translator
):

  LilaScheduler("TournamentScheduler", _.Every(5 minutes), _.AtMost(1 minute), _.Delay(1 minute)):
    given play.api.i18n.Lang = lila.core.i18n.defaultLang
    tournamentRepo.scheduledUnfinished.flatMap: dbScheds =>
      try
        val plans = TournamentScheduler.allWithConflicts()
        val possibleTourneys = plans.map(p => (p.schedule, p.build)).map { case (s, t) =>
          ConcreteTourney(t, s, t.startsAt, t.duration)
        }

        val existingSchedules = dbScheds.flatMap { t =>
          // Ignore tournaments with schedule=None - they never conflict.
          t.schedule.map { ConcreteSchedule(_, t.startsAt, t.duration) }
        }

        val prunedTourneys = Schedule.pruneConflicts(existingSchedules, possibleTourneys)

        tournamentRepo.insert(prunedTourneys.map(_.tournament)).logFailure(logger)
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
  def allWithConflicts(rightNow: Instant = nowInstant): List[Plan] =
    val today       = rightNow.date
    val tomorrow    = today.plusDays(1)
    val startOfYear = today.withDayOfYear(1)

    class OfMonth(fromNow: Int):
      val firstDay   = today.plusMonths(fromNow).withDayOfMonth(1)
      val lastDay    = firstDay.adjust(TemporalAdjusters.lastDayOfMonth)
      val firstWeek  = firstDay.plusDays(7 - (firstDay.getDayOfWeek.getValue - 1) % 7)
      val secondWeek = firstWeek.plusDays(7)
      val thirdWeek  = secondWeek.plusDays(7)
      val lastWeek   = lastDay.minusDays((lastDay.getDayOfWeek.getValue - 1) % 7)
    val thisMonth = OfMonth(0)
    val nextMonth = OfMonth(1)

    def nextDayOfWeek(n: Int) = today.plusDays((n + 7 - today.getDayOfWeek.getValue) % 7)
    val nextMonday            = nextDayOfWeek(1)
    val nextTuesday           = nextDayOfWeek(2)
    val nextWednesday         = nextDayOfWeek(3)
    val nextThursday          = nextDayOfWeek(4)
    val nextFriday            = nextDayOfWeek(5)
    val nextSaturday          = nextDayOfWeek(6)
    val nextSunday            = nextDayOfWeek(7)

    def secondWeekOf(month: java.time.Month): LocalDate =
      val start = orNextYear(startOfYear.withMonth(month.getValue).atStartOfDay).date
      start.plusDays(15 - start.getDayOfWeek.getValue)

    def orTomorrow(date: LocalDateTime) = if date.instant.isBefore(rightNow) then date.plusDays(1) else date
    def orNextWeek(date: LocalDateTime) = if date.instant.isBefore(rightNow) then date.plusWeeks(1) else date
    def orNextYear(date: LocalDateTime) = if date.instant.isBefore(rightNow) then date.plusYears(1) else date

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
        at(birthday.withYear(today.getYear), 12).map(orNextYear).map { date =>
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
      ).flatten,
      List( // yearly tournaments!
        secondWeekOf(JANUARY).withDayOfWeek(MONDAY)      -> Bullet,
        secondWeekOf(FEBRUARY).withDayOfWeek(TUESDAY)    -> SuperBlitz,
        secondWeekOf(MARCH).withDayOfWeek(WEDNESDAY)     -> Blitz,
        secondWeekOf(APRIL).withDayOfWeek(THURSDAY)      -> Rapid,
        secondWeekOf(MAY).withDayOfWeek(FRIDAY)          -> Classical,
        secondWeekOf(JUNE).withDayOfWeek(SATURDAY)       -> HyperBullet,
        secondWeekOf(JULY).withDayOfWeek(MONDAY)         -> Bullet,
        secondWeekOf(AUGUST).withDayOfWeek(TUESDAY)      -> SuperBlitz,
        secondWeekOf(SEPTEMBER).withDayOfWeek(WEDNESDAY) -> Blitz,
        secondWeekOf(OCTOBER).withDayOfWeek(THURSDAY)    -> Rapid,
        secondWeekOf(NOVEMBER).withDayOfWeek(FRIDAY)     -> Classical,
        secondWeekOf(DECEMBER).withDayOfWeek(SATURDAY)   -> HyperBullet
      ).flatMap { (day, speed) =>
        at(day, 17).filter(farFuture.isAfter).map { date =>
          Schedule(Yearly, speed, Standard, none, date).plan
        }
      },
      List( // yearly variant tournaments!
        secondWeekOf(JANUARY).withDayOfWeek(WEDNESDAY) -> Chess960,
        secondWeekOf(FEBRUARY).withDayOfWeek(THURSDAY) -> Crazyhouse,
        secondWeekOf(MARCH).withDayOfWeek(FRIDAY)      -> KingOfTheHill,
        secondWeekOf(APRIL).withDayOfWeek(SATURDAY)    -> RacingKings,
        secondWeekOf(MAY).withDayOfWeek(MONDAY)        -> Antichess,
        secondWeekOf(JUNE).withDayOfWeek(TUESDAY)      -> Atomic,
        secondWeekOf(JULY).withDayOfWeek(WEDNESDAY)    -> Horde,
        secondWeekOf(AUGUST).withDayOfWeek(THURSDAY)   -> ThreeCheck
      ).flatMap { (day, variant) =>
        at(day, 17).filter(farFuture.isAfter).map { date =>
          Schedule(Yearly, SuperBlitz, variant, none, date).plan
        }
      },
      List(thisMonth, nextMonth).flatMap { month =>
        List(
          List( // monthly standard tournaments!
            month.lastWeek.withDayOfWeek(MONDAY)    -> Bullet,
            month.lastWeek.withDayOfWeek(TUESDAY)   -> SuperBlitz,
            month.lastWeek.withDayOfWeek(WEDNESDAY) -> Blitz,
            month.lastWeek.withDayOfWeek(THURSDAY)  -> Rapid,
            month.lastWeek.withDayOfWeek(FRIDAY)    -> Classical,
            month.lastWeek.withDayOfWeek(SATURDAY)  -> HyperBullet,
            month.lastWeek.withDayOfWeek(SUNDAY)    -> UltraBullet
          ).flatMap { (day, speed) =>
            at(day, 17).map { date =>
              Schedule(Monthly, speed, Standard, none, date).plan
            }
          },
          List( // monthly variant tournaments!
            month.lastWeek.withDayOfWeek(MONDAY)    -> Chess960,
            month.lastWeek.withDayOfWeek(TUESDAY)   -> Crazyhouse,
            month.lastWeek.withDayOfWeek(WEDNESDAY) -> KingOfTheHill,
            month.lastWeek.withDayOfWeek(THURSDAY)  -> RacingKings,
            month.lastWeek.withDayOfWeek(FRIDAY)    -> Antichess,
            month.lastWeek.withDayOfWeek(SATURDAY)  -> Atomic,
            month.lastWeek.withDayOfWeek(SUNDAY)    -> Horde,
            month.lastWeek.withDayOfWeek(SUNDAY)    -> ThreeCheck
          ).flatMap { (day, variant) =>
            at(day, 19).map { date =>
              Schedule(
                Monthly,
                if variant == Chess960 || variant == Crazyhouse then Blitz else SuperBlitz,
                variant,
                none,
                date
              ).plan
            }
          },
          List( // shield tournaments!
            month.firstWeek.withDayOfWeek(MONDAY)    -> Bullet,
            month.firstWeek.withDayOfWeek(TUESDAY)   -> SuperBlitz,
            month.firstWeek.withDayOfWeek(WEDNESDAY) -> Blitz,
            month.firstWeek.withDayOfWeek(THURSDAY)  -> Rapid,
            month.firstWeek.withDayOfWeek(FRIDAY)    -> Classical,
            month.firstWeek.withDayOfWeek(SATURDAY)  -> HyperBullet,
            month.firstWeek.withDayOfWeek(SUNDAY)    -> UltraBullet
          ).flatMap: (day, speed) =>
            at(day, 16).map: date =>
              Schedule(Shield, speed, Standard, none, date).plan(TournamentShield.make(speed.toString)),
          List( // shield variant tournaments!
            month.secondWeek.withDayOfWeek(SUNDAY)   -> Chess960,
            month.thirdWeek.withDayOfWeek(MONDAY)    -> Crazyhouse,
            month.thirdWeek.withDayOfWeek(TUESDAY)   -> KingOfTheHill,
            month.thirdWeek.withDayOfWeek(WEDNESDAY) -> RacingKings,
            month.thirdWeek.withDayOfWeek(THURSDAY)  -> Antichess,
            month.thirdWeek.withDayOfWeek(FRIDAY)    -> Atomic,
            month.thirdWeek.withDayOfWeek(SATURDAY)  -> Horde,
            month.thirdWeek.withDayOfWeek(SUNDAY)    -> ThreeCheck
          ).flatMap: (day, variant) =>
            at(day, 16).map: date =>
              Schedule(Shield, Blitz, variant, none, date).plan(TournamentShield.make(variant.name))
        ).flatten
      },
      List( // weekly standard tournaments!
        nextMonday    -> Bullet,
        nextTuesday   -> SuperBlitz,
        nextWednesday -> Blitz,
        nextThursday  -> Rapid,
        nextFriday    -> Classical,
        nextSaturday  -> HyperBullet,
        nextSunday    -> UltraBullet
      ).flatMap { (day, speed) =>
        at(day, 17).map { date =>
          Schedule(Weekly, speed, Standard, none, date.pipe(orNextWeek)).plan
        }
      },
      List( // weekly variant tournaments!
        nextMonday    -> ThreeCheck,
        nextTuesday   -> Crazyhouse,
        nextWednesday -> KingOfTheHill,
        nextThursday  -> RacingKings,
        nextFriday    -> Antichess,
        nextSaturday  -> Atomic,
        nextSunday    -> Horde,
        nextSunday    -> Chess960
      ).flatMap { (day, variant) =>
        at(day, 19).map { date =>
          Schedule(
            Weekly,
            if variant == Chess960 || variant == Crazyhouse then Blitz else SuperBlitz,
            variant,
            none,
            date.pipe(orNextWeek)
          ).plan
        }
      },
      List( // week-end elite tournaments!
        nextSaturday -> SuperBlitz,
        nextSunday   -> Bullet
      ).flatMap { (day, speed) =>
        at(day, 17).map { date =>
          Schedule(Weekend, speed, Standard, none, date.pipe(orNextWeek)).plan
        }
      },
      // Note: these should be scheduled close to the hour of weekly or weekend tournaments
      // to avoid two dailies being cancelled in a row from a single higher importance tourney
      List( // daily tournaments!
        at(today, 16).map { date =>
          Schedule(Daily, Bullet, Standard, none, date.pipe(orTomorrow)).plan
        },
        at(today, 17).map { date =>
          Schedule(Daily, SuperBlitz, Standard, none, date.pipe(orTomorrow)).plan
        },
        at(today, 18).map { date =>
          Schedule(Daily, Blitz, Standard, none, date.pipe(orTomorrow)).plan
        },
        at(today, 19).map { date =>
          Schedule(Daily, Rapid, Standard, none, date.pipe(orTomorrow)).plan
        },
        at(today, 20).map { date =>
          Schedule(Daily, HyperBullet, Standard, none, date.pipe(orTomorrow)).plan
        },
        at(today, 21).map { date =>
          Schedule(Daily, UltraBullet, Standard, none, date.pipe(orTomorrow)).plan
        }
      ).flatten,
      // Note: these should be scheduled close to the hour of weekly variant tournaments
      // to avoid two dailies being cancelled in a row from a single higher importance tourney
      List( // daily variant tournaments!
        at(today, 20).map { date =>
          Schedule(Daily, Blitz, Crazyhouse, none, date.pipe(orTomorrow)).plan
        },
        at(today, 21).map { date =>
          Schedule(Daily, Blitz, Chess960, none, date.pipe(orTomorrow)).plan
        },
        at(today, 22).map { date =>
          Schedule(Daily, SuperBlitz, KingOfTheHill, none, date.pipe(orTomorrow)).plan
        },
        at(today, 23).map { date =>
          Schedule(Daily, SuperBlitz, Atomic, none, date.pipe(orTomorrow)).plan
        },
        at(today, 0).map { date =>
          Schedule(Daily, SuperBlitz, Antichess, none, date.pipe(orTomorrow)).plan
        },
        at(tomorrow, 1).map { date =>
          Schedule(Daily, SuperBlitz, ThreeCheck, none, date).plan
        },
        at(tomorrow, 2).map { date =>
          Schedule(Daily, SuperBlitz, Horde, none, date).plan
        },
        at(tomorrow, 3).map { date =>
          Schedule(Daily, SuperBlitz, RacingKings, none, date).plan
        }
      ).flatten,
      List( // eastern tournaments!
        at(today, 4).map { date =>
          Schedule(Eastern, Bullet, Standard, none, date.pipe(orTomorrow)).plan
        },
        at(today, 5).map { date =>
          Schedule(Eastern, SuperBlitz, Standard, none, date.pipe(orTomorrow)).plan
        },
        at(today, 6).map { date =>
          Schedule(Eastern, Blitz, Standard, none, date.pipe(orTomorrow)).plan
        }
      ).flatten, {
        {
          for
            halloween    <- StartingPosition.presets.halloween
            frankenstein <- StartingPosition.presets.frankenstein
            if isHalloween // replace more thematic tournaments on halloween
          yield List(
            1  -> halloween,
            5  -> frankenstein,
            9  -> halloween,
            13 -> frankenstein,
            17 -> halloween,
            21 -> frankenstein
          )
        } |
          List( // random opening replaces hourly 3 times a day
            3  -> openingAt(offset = 2),
            11 -> openingAt(offset = 1),
            19 -> openingAt(offset = 0)
          )
      }.flatMap { (hour, opening) =>
        List(
          at(today, hour).map { date =>
            Schedule(Hourly, Bullet, Standard, opening.fen.some, date.pipe(orTomorrow)).plan
          },
          at(today, hour + 1).map { date =>
            Schedule(Hourly, SuperBlitz, Standard, opening.fen.some, date.pipe(orTomorrow)).plan
          },
          at(today, hour + 2).map { date =>
            Schedule(Hourly, Blitz, Standard, opening.fen.some, date.pipe(orTomorrow)).plan
          },
          at(today, hour + 3).map { date =>
            Schedule(Hourly, Rapid, Standard, opening.fen.some, date.pipe(orTomorrow)).plan
          }
        ).flatten
      },
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
            import chess.Clock
            val conditions = TournamentCondition.All(
              nbRatedGame = Condition.NbRatedGame(20).some,
              maxRating = Condition.MaxRating(rating).some,
              minRating = none,
              titled = none,
              teamMember = none,
              accountAge = none,
              allowList = none
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
      // hourly crazyhouse/chess960/KingOfTheHill tournaments!
      (0 to 6).toList.flatMap { hourDelta =>
        val when = atTopOfHour(rightNow, hourDelta)
        val speed = when.getHour % 7 match
          case 0     => HippoBullet
          case 1 | 4 => Bullet
          case 2 | 5 => SuperBlitz
          case 3 | 6 => Blitz
        val variant = when.getHour % 3 match
          case 0 => Chess960
          case 1 => KingOfTheHill
          case _ => Crazyhouse
        List(Schedule(Hourly, speed, variant, none, when).plan) :::
          (speed == Bullet).so:
            List(
              Schedule(
                Hourly,
                if when.getHour == 17 then HyperBullet else Bullet,
                variant,
                none,
                when.plusMinutes(30)
              ).plan
            )
      },
      // hourly atomic/antichess variant tournaments!
      (0 to 6).toList.flatMap { hourDelta =>
        val when = atTopOfHour(rightNow, hourDelta)
        val speed = when.getHour % 7 match
          case 0 | 4 => Blitz
          case 1     => HippoBullet
          case 2 | 5 => Bullet
          case 3 | 6 => SuperBlitz
        val variant = if when.getHour % 2 == 0 then Atomic else Antichess
        List(Schedule(Hourly, speed, variant, none, when).plan) :::
          (speed == Bullet).so:
            List(
              Schedule(
                Hourly,
                if when.getHour == 18 then HyperBullet else Bullet,
                variant,
                none,
                when.plusMinutes(30)
              ).plan
            )
      },
      // hourly threecheck/horde/racing variant tournaments!
      (0 to 6).toList.flatMap { hourDelta =>
        val when = atTopOfHour(rightNow, hourDelta)
        val speed = when.getHour % 7 match
          case 0 | 4 => SuperBlitz
          case 1 | 5 => Blitz
          case 2     => HippoBullet
          case 3 | 6 => Bullet
        val variant = when.getHour % 3 match
          case 0 => ThreeCheck
          case 1 => Horde
          case _ => RacingKings
        List(Schedule(Hourly, speed, variant, none, when).plan) :::
          (speed == Bullet).so:
            List(
              Schedule(
                Hourly,
                if when.getHour == 19 then HyperBullet else Bullet,
                variant,
                none,
                when.plusMinutes(30)
              ).plan
            )
      }
    ).flatten.filter(_.schedule.atInstant.isAfter(rightNow))

  private def atTopOfHour(rightNow: Instant, hourDelta: Int): LocalDateTime =
    rightNow.plusHours(hourDelta).dateTime.withMinute(0)

  private def at(day: LocalDate, hour: Int, minute: Int = 0): Option[LocalDateTime] =
    try Some(day.atStartOfDay.plusHours(hour).plusMinutes(minute))
    catch
      case e: Exception =>
        logger.error("failed to schedule", e)
        None
