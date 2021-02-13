package lila.tournament

import akka.actor._
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants._
import scala.util.chaining._

import chess.StartingPosition

final private class TournamentScheduler(
    api: TournamentApi,
    tournamentRepo: TournamentRepo
) extends Actor {

  import Schedule.Freq._
  import Schedule.Speed._
  import Schedule.Plan
  import chess.variant._

  implicit def ec = context.dispatcher

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
  private[tournament] def allWithConflicts(rightNow: DateTime): List[Plan] = {
    val today       = rightNow.withTimeAtStartOfDay
    val tomorrow    = rightNow plusDays 1
    val startOfYear = today.dayOfYear.withMinimumValue

    class OfMonth(fromNow: Int) {
      val firstDay = today.plusMonths(fromNow).dayOfMonth.withMinimumValue
      val lastDay  = firstDay.dayOfMonth.withMaximumValue

      val firstWeek  = firstDay.plusDays(7 - (firstDay.getDayOfWeek - 1) % 7)
      val secondWeek = firstWeek plusDays 7
      val thirdWeek  = secondWeek plusDays 7
      val lastWeek   = lastDay.minusDays((lastDay.getDayOfWeek - 1) % 7)
    }
    val thisMonth = new OfMonth(0)
    val nextMonth = new OfMonth(1)

    def nextDayOfWeek(number: Int) = today.plusDays((number + 7 - today.getDayOfWeek) % 7)
    val nextMonday                 = nextDayOfWeek(1)
    val nextTuesday                = nextDayOfWeek(2)
    val nextWednesday              = nextDayOfWeek(3)
    val nextThursday               = nextDayOfWeek(4)
    val nextFriday                 = nextDayOfWeek(5)
    val nextSaturday               = nextDayOfWeek(6)
    val nextSunday                 = nextDayOfWeek(7)

    def secondWeekOf(month: Int) = {
      val start = orNextYear(startOfYear.withMonthOfYear(month))
      start.plusDays(15 - start.getDayOfWeek)
    }

    def orTomorrow(date: DateTime) = if (date isBefore rightNow) date plusDays 1 else date
    def orNextWeek(date: DateTime) = if (date isBefore rightNow) date plusWeeks 1 else date
    def orNextYear(date: DateTime) = if (date isBefore rightNow) date plusYears 1 else date

    val isHalloween = today.getDayOfMonth == 31 && today.getMonthOfYear == OCTOBER

    val std = StartingPosition.initial
    def opening(offset: Int) = {
      val positions = StartingPosition.featurable
      positions((today.getDayOfYear + offset) % positions.size)
    }

    val farFuture = today plusMonths 7

    val birthday = new DateTime(2010, 6, 20, 12, 0, 0)

    // all dates UTC
    List(
      List( // legendary tournaments!
        at(birthday.withYear(today.getYear), 12) map orNextYear map { date =>
          val yo = date.getYear - 2010
          Schedule(Unique, Rapid, Standard, std, date) plan {
            _.copy(
              name = s"${date.getYear} Lichess Anniversary",
              minutes = 12 * 60,
              spotlight = Spotlight(
                headline = s"$yo years of free chess!",
                description = s"""
We've had $yo great chess years together!

Thank you all, you rock!"""
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
      ).flatMap {
        case (day, speed) =>
          at(day, 17) filter farFuture.isAfter map { date =>
            Schedule(Yearly, speed, Standard, std, date).plan
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
      ).flatMap {
        case (day, variant) =>
          at(day, 17) filter farFuture.isAfter map { date =>
            Schedule(Yearly, SuperBlitz, variant, std, date).plan
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
          ).flatMap {
            case (day, speed) =>
              at(day, 17) map { date =>
                Schedule(Monthly, speed, Standard, std, date).plan
              }
          },
          List( // monthly variant tournaments!
            month.lastWeek.withDayOfWeek(MONDAY)    -> Chess960,
            month.lastWeek.withDayOfWeek(TUESDAY)   -> Crazyhouse,
            month.lastWeek.withDayOfWeek(WEDNESDAY) -> KingOfTheHill,
            month.lastWeek.withDayOfWeek(THURSDAY)  -> RacingKings,
            month.lastWeek.withDayOfWeek(FRIDAY)    -> Antichess,
            month.lastWeek.withDayOfWeek(SATURDAY)  -> Atomic,
            month.lastWeek.withDayOfWeek(SUNDAY)    -> Horde
          ).flatMap {
            case (day, variant) =>
              at(day, 19) map { date =>
                Schedule(
                  Monthly,
                  if (variant == Chess960 || variant == Crazyhouse) Blitz else SuperBlitz,
                  variant,
                  std,
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
          ).flatMap {
            case (day, speed) =>
              at(day, 16) map { date =>
                Schedule(Shield, speed, Standard, std, date) plan {
                  _.copy(
                    name = s"${speed.toString} Shield",
                    spotlight = Some(TournamentShield spotlight speed.toString)
                  )
                }
              }
          },
          List( // shield variant tournaments!
            month.secondWeek.withDayOfWeek(SUNDAY)   -> Chess960,
            month.thirdWeek.withDayOfWeek(MONDAY)    -> Crazyhouse,
            month.thirdWeek.withDayOfWeek(TUESDAY)   -> KingOfTheHill,
            month.thirdWeek.withDayOfWeek(WEDNESDAY) -> RacingKings,
            month.thirdWeek.withDayOfWeek(THURSDAY)  -> Antichess,
            month.thirdWeek.withDayOfWeek(FRIDAY)    -> Atomic,
            month.thirdWeek.withDayOfWeek(SATURDAY)  -> Horde,
            month.thirdWeek.withDayOfWeek(SUNDAY)    -> ThreeCheck
          ).flatMap {
            case (day, variant) =>
              at(day, 16) map { date =>
                Schedule(Shield, Blitz, variant, std, date) plan {
                  _.copy(
                    name = s"${variant.name} Shield",
                    spotlight = Some(TournamentShield spotlight variant.name)
                  )
                }
              }
          }
        ).flatten
      },
      List( // weekly standard tournaments!
        nextMonday    -> Bullet,
        nextTuesday   -> SuperBlitz,
        nextWednesday -> Blitz,
        nextThursday  -> Rapid,
        nextFriday    -> Classical,
        nextSaturday  -> HyperBullet
      ).flatMap {
        case (day, speed) =>
          at(day, 17) map { date =>
            Schedule(Weekly, speed, Standard, std, date pipe orNextWeek).plan
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
      ).flatMap {
        case (day, variant) =>
          at(day, 19) map { date =>
            Schedule(
              Weekly,
              if (variant == Chess960 || variant == Crazyhouse) Blitz else SuperBlitz,
              variant,
              std,
              date pipe orNextWeek
            ).plan
          }
      },
      List( // week-end elite tournaments!
        nextSaturday -> SuperBlitz,
        nextSunday   -> Bullet
      ).flatMap {
        case (day, speed) =>
          at(day, 17) map { date =>
            Schedule(Weekend, speed, Standard, std, date pipe orNextWeek).plan
          }
      },
      List( // daily tournaments!
        at(today, 16) map { date =>
          Schedule(Daily, Bullet, Standard, std, date pipe orTomorrow).plan
        },
        at(today, 17) map { date =>
          Schedule(Daily, SuperBlitz, Standard, std, date pipe orTomorrow).plan
        },
        at(today, 18) map { date =>
          Schedule(Daily, Blitz, Standard, std, date pipe orTomorrow).plan
        },
        at(today, 19) map { date =>
          Schedule(Daily, Rapid, Standard, std, date pipe orTomorrow).plan
        },
        at(today, 20) map { date =>
          Schedule(Daily, HyperBullet, Standard, std, date pipe orTomorrow).plan
        },
        at(today, 21) map { date =>
          Schedule(Daily, UltraBullet, Standard, std, date pipe orTomorrow).plan
        }
      ).flatten,
      List( // daily variant tournaments!
        at(today, 20) map { date =>
          Schedule(Daily, Blitz, Crazyhouse, std, date pipe orTomorrow).plan
        },
        at(today, 21) map { date =>
          Schedule(Daily, Blitz, Chess960, std, date pipe orTomorrow).plan
        },
        at(today, 22) map { date =>
          Schedule(Daily, SuperBlitz, KingOfTheHill, std, date pipe orTomorrow).plan
        },
        at(today, 23) map { date =>
          Schedule(Daily, SuperBlitz, Atomic, std, date pipe orTomorrow).plan
        },
        at(today, 0) map { date =>
          Schedule(Daily, SuperBlitz, Antichess, std, date pipe orTomorrow).plan
        },
        at(tomorrow, 1) map { date =>
          Schedule(Daily, SuperBlitz, ThreeCheck, std, date).plan
        },
        at(tomorrow, 2) map { date =>
          Schedule(Daily, SuperBlitz, Horde, std, date).plan
        },
        at(tomorrow, 3) map { date =>
          Schedule(Daily, SuperBlitz, RacingKings, std, date).plan
        }
      ).flatten,
      List( // eastern tournaments!
        at(today, 4) map { date =>
          Schedule(Eastern, Bullet, Standard, std, date pipe orTomorrow).plan
        },
        at(today, 5) map { date =>
          Schedule(Eastern, SuperBlitz, Standard, std, date pipe orTomorrow).plan
        },
        at(today, 6) map { date =>
          Schedule(Eastern, Blitz, Standard, std, date pipe orTomorrow).plan
        },
        at(today, 7) map { date =>
          Schedule(Eastern, Rapid, Standard, std, date pipe orTomorrow).plan
        }
      ).flatten,
      (if (isHalloween) // replace more thematic tournaments on halloween
         List(
           1  -> StartingPosition.presets.halloween,
           5  -> StartingPosition.presets.frankenstein,
           9  -> StartingPosition.presets.halloween,
           13 -> StartingPosition.presets.frankenstein,
           17 -> StartingPosition.presets.halloween,
           21 -> StartingPosition.presets.frankenstein
         )
       else
         List( // random opening replaces hourly 3 times a day
           3  -> opening(offset = 2),
           11 -> opening(offset = 1),
           19 -> opening(offset = 0)
         )).flatMap {
        case (hour, opening) =>
          List(
            at(today, hour) map { date =>
              Schedule(Hourly, Bullet, Standard, opening, date pipe orTomorrow).plan
            },
            at(today, hour + 1) map { date =>
              Schedule(Hourly, SuperBlitz, Standard, opening, date pipe orTomorrow).plan
            },
            at(today, hour + 2) map { date =>
              Schedule(Hourly, Blitz, Standard, opening, date pipe orTomorrow).plan
            },
            at(today, hour + 3) map { date =>
              Schedule(Hourly, Rapid, Standard, opening, date pipe orTomorrow).plan
            }
          ).flatten
      },
      // hourly standard tournaments!
      (-1 to 6).toList.flatMap { hourDelta =>
        val date = rightNow plusHours hourDelta
        val hour = date.getHourOfDay
        List(
          at(date, hour) map { date =>
            Schedule(Hourly, HyperBullet, Standard, std, date).plan
          },
          at(date, hour, 30) map { date =>
            Schedule(Hourly, UltraBullet, Standard, std, date).plan
          },
          at(date, hour) map { date =>
            Schedule(Hourly, Bullet, Standard, std, date).plan
          },
          at(date, hour, 30) map { date =>
            Schedule(Hourly, Bullet, Standard, std, date).plan
          },
          at(date, hour) map { date =>
            Schedule(Hourly, SuperBlitz, Standard, std, date).plan
          },
          at(date, hour) map { date =>
            Schedule(Hourly, Blitz, Standard, std, date).plan
          },
          at(date, hour) collect {
            case date if hour % 2 == 0 => Schedule(Hourly, Rapid, Standard, std, date).plan
          }
        ).flatten
      },
      // hourly limited tournaments!
      (-1 to 6).toList
        .flatMap { hourDelta =>
          val date = rightNow plusHours hourDelta
          val hour = date.getHourOfDay
          val speed = hour % 4 match {
            case 0 => Bullet
            case 1 => SuperBlitz
            case 2 => Blitz
            case _ => Rapid
          }
          List(
            1500 -> 0,
            1700 -> 1,
            2000 -> 2
          ).flatMap {
            case (rating, hourDelay) =>
              val perf = Schedule.Speed toPerfType speed
              val conditions = Condition.All(
                nbRatedGame = Condition.NbRatedGame(perf.some, 20).some,
                maxRating = Condition.MaxRating(perf, rating).some,
                minRating = none,
                titled = none,
                teamMember = none
              )
              at(date, hour) ?? { date =>
                val finalDate = date plusHours hourDelay
                if (speed == Bullet)
                  List(
                    Schedule(Hourly, speed, Standard, std, finalDate, conditions).plan,
                    Schedule(Hourly, speed, Standard, std, finalDate plusMinutes 30, conditions)
                      .plan(_.copy(clock = chess.Clock.Config(60, 1)))
                  )
                else
                  List(
                    Schedule(Hourly, speed, Standard, std, finalDate, conditions).plan
                  )
              }
          }
        }
        .map {
          // No berserk for rating-limited tournaments
          // Because berserking lowers the player rating
          _ map { _.copy(noBerserk = true) }
        },
      // hourly crazyhouse tournaments!
      (0 to 6).toList.flatMap { hourDelta =>
        val date = rightNow plusHours hourDelta
        val hour = date.getHourOfDay
        val speed = hour % 6 match {
          case 0 | 3 => Bullet
          case 1 | 4 => SuperBlitz
          case 5     => HippoBullet
          case _     => Blitz
        }
        List(
          at(date, hour) map { date =>
            Schedule(Hourly, speed, Crazyhouse, std, date).plan
          },
          at(date, hour, 30) collect {
            case date if speed == Bullet =>
              Schedule(Hourly, if (hour == 18) HyperBullet else Bullet, Crazyhouse, std, date).plan
          }
        ).flatten
      },
      // hourly variant tournaments!
      (0 to 6).toList.flatMap { hourDelta =>
        val date = rightNow plusHours hourDelta
        val hour = date.getHourOfDay
        val speed = hour % 6 match {
          case 1 | 4 => Bullet
          case 2 | 5 => SuperBlitz
          case 3     => HippoBullet
          case _     => Blitz
        }
        val variant = if (hour % 2 == 0) Atomic else Antichess
        List(
          at(date, hour) map { date =>
            Schedule(Hourly, speed, variant, std, date).plan
          },
          at(date, hour, 30) collect {
            case date if speed == Bullet =>
              Schedule(Hourly, if (hour == 18) HyperBullet else Bullet, variant, std, date).plan
          }
        ).flatten
      }
    ).flatten filter { _.schedule.at.isAfter(rightNow minusHours 1) }
  }

  private[tournament] def pruneConflicts(scheds: List[Tournament], newTourns: List[Tournament]) = {
    newTourns
      .foldLeft(List[Tournament]()) {
        case (tourns, t) =>
          if (overlaps(t, tourns) || overlaps(t, scheds)) tourns
          else t :: tourns
      } reverse
  }

  private case class ScheduleNowWith(dbScheds: List[Tournament])

  private def overlaps(t: Tournament, ts: List[Tournament]): Boolean =
    t.schedule exists { s =>
      ts exists { t2 =>
        t.variant == t2.variant && (t2.schedule ?? {
          // prevent daily && weekly on the same day
          case s2 if s.freq.isDailyOrBetter && s2.freq.isDailyOrBetter && s.sameSpeed(s2) => s sameDay s2
          case s2 =>
            (
              t.variant.exotic ||  // overlapping exotic variant
                s.hasMaxRating ||  // overlapping same rating limit
                s.similarSpeed(s2) // overlapping similar
            ) && s.similarConditions(s2) && t.overlaps(t2)
        })
      }
    }

  private def at(day: DateTime, hour: Int, minute: Int = 0): Option[DateTime] =
    try {
      Some(day.withTimeAtStartOfDay plusHours hour plusMinutes minute)
    } catch {
      case e: Exception =>
        logger.error(s"failed to schedule one: ${e.getMessage}")
        None
    }

  def receive = {

    case TournamentScheduler.ScheduleNow =>
      tournamentRepo.scheduledUnfinished dforeach { tourneys =>
        self ! ScheduleNowWith(tourneys)
      }

    case ScheduleNowWith(dbScheds) =>
      try {
        val newTourns = allWithConflicts(DateTime.now) map { _.build }
        pruneConflicts(dbScheds, newTourns) foreach api.create

      } catch {
        case e: org.joda.time.IllegalInstantException =>
          logger.error(s"failed to schedule all: ${e.getMessage}")
      }
  }
}

private object TournamentScheduler {

  case object ScheduleNow
}
