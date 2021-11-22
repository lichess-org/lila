package lila.tournament

import akka.actor._
import shogi.format.FEN
import shogi.StartingPosition
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants._
import scala.util.chaining._

final private class TournamentScheduler(
    api: TournamentApi,
    tournamentRepo: TournamentRepo
) extends Actor {

  import Schedule.Freq._
  import Schedule.Speed._
  import Schedule.Plan
  import shogi.variant._

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

    val farFuture = today plusMonths 7

    val birthday = new DateTime(2020, 9, 29, 12, 0, 0)

    // all dates UTC
    List(
      List( // legendary tournaments!
        at(birthday.withYear(today.getYear), 12) map orNextYear map { date =>
          val yo = date.getYear - 2020
          Schedule(Unique, Rapid, Standard, none, date) plan {
            _.copy(
              name = s"${date.getYear} Lishogi Anniversary",
              minutes = 12 * 60,
              spotlight = Spotlight(
                headline = s"$yo years of free shogi!",
                description = s"""
We've had $yo great shogi year together!

Thank you all, you rock!"""
              ).some
            )
          }
        }
      ).flatten,
      List( // yearly tournaments!
        secondWeekOf(JANUARY).withDayOfWeek(MONDAY)      -> Bullet,
        secondWeekOf(FEBRUARY).withDayOfWeek(TUESDAY)    -> Blitz,
        secondWeekOf(MARCH).withDayOfWeek(WEDNESDAY)     -> SuperBlitz,
        secondWeekOf(APRIL).withDayOfWeek(THURSDAY)      -> Rapid,
        secondWeekOf(MAY).withDayOfWeek(FRIDAY)          -> Classical,
        secondWeekOf(JUNE).withDayOfWeek(SATURDAY)       -> HyperRapid,
        secondWeekOf(JULY).withDayOfWeek(MONDAY)         -> Bullet,
        secondWeekOf(AUGUST).withDayOfWeek(TUESDAY)      -> Blitz,
        secondWeekOf(SEPTEMBER).withDayOfWeek(WEDNESDAY) -> HyperRapid,
        secondWeekOf(OCTOBER).withDayOfWeek(THURSDAY)    -> Rapid,
        secondWeekOf(NOVEMBER).withDayOfWeek(FRIDAY)     -> Classical,
        secondWeekOf(DECEMBER).withDayOfWeek(SATURDAY)   -> Blitz
      ).flatMap { case (day, speed) =>
        at(day, 13) filter farFuture.isAfter map { date =>
          Schedule(Yearly, speed, Standard, none, date).plan
        }
      },
      List(thisMonth, nextMonth).flatMap { month =>
        List(
          List( // monthly standard tournaments!
            month.lastWeek.withDayOfWeek(MONDAY)    -> HyperRapid,
            month.lastWeek.withDayOfWeek(TUESDAY)   -> SuperBlitz,
            month.lastWeek.withDayOfWeek(WEDNESDAY) -> Blitz,
            month.lastWeek.withDayOfWeek(THURSDAY)  -> Rapid,
            month.lastWeek.withDayOfWeek(FRIDAY)    -> Classical
          ).flatMap { case (day, speed) =>
            at(day, 13) map { date =>
              Schedule(Monthly, speed, Standard, none, date).plan
            }
          },
          List( // shield tournaments!
            month.firstWeek.withDayOfWeek(MONDAY)    -> Bullet,
            month.firstWeek.withDayOfWeek(TUESDAY)   -> SuperBlitz,
            month.firstWeek.withDayOfWeek(WEDNESDAY) -> Blitz,
            month.firstWeek.withDayOfWeek(THURSDAY)  -> Rapid,
            month.firstWeek.withDayOfWeek(FRIDAY)    -> Classical
          ).flatMap { case (day, speed) =>
            at(day, 12) map { date =>
              Schedule(Shield, speed, Standard, none, date) plan {
                _.copy(
                  name = s"${speed.toString} Shield",
                  spotlight = Some(TournamentShield spotlight speed.toString)
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
        nextFriday    -> Classical
      ).flatMap { case (day, speed) =>
        at(day, 13) map { date =>
          Schedule(Weekly, speed, Standard, none, date pipe orNextWeek).plan
        }
      },
      List( // week-end elite tournaments!
        nextSaturday -> Blitz,
        nextSunday   -> HyperRapid
      ).flatMap { case (day, speed) =>
        at(day, 13) map { date =>
          Schedule(Weekend, speed, Standard, none, date pipe orNextWeek).plan
        }
      },
      List( // daily tournaments!
        at(today, 14) map { date =>
          Schedule(Daily, Bullet, Standard, none, date pipe orTomorrow).plan
        },
        at(today, 16) map { date =>
          Schedule(Daily, HyperRapid, Standard, none, date pipe orTomorrow).plan
        },
        at(today, 18) map { date =>
          Schedule(Daily, Rapid, Standard, none, date pipe orTomorrow).plan
        },
        at(today, 20) map { date =>
          Schedule(Daily, HyperRapid, Standard, none, date pipe orTomorrow).plan
        },
        at(today, 22) map { date =>
          Schedule(Daily, Rapid, Standard, none, date pipe orTomorrow).plan
        },
        at(today, 0) map { date =>
          Schedule(Daily, Classical, Standard, none, date pipe orTomorrow).plan
        }
      ).flatten,
      List( // eastern tournaments!
        at(today, 2) map { date =>
          Schedule(Eastern, Bullet, Standard, none, date pipe orTomorrow).plan
        },
        at(today, 4) map { date =>
          Schedule(Eastern, SuperBlitz, Standard, none, date pipe orTomorrow).plan
        },
        at(today, 6) map { date =>
          Schedule(Eastern, Blitz, Standard, none, date pipe orTomorrow).plan
        },
        at(today, 8) map { date =>
          Schedule(Eastern, HyperRapid, Standard, none, date pipe orTomorrow).plan
        },
        at(today, 10) map { date =>
          Schedule(Eastern, Rapid, Standard, none, date pipe orTomorrow).plan
        },
        at(today, 12) map { date =>
          Schedule(Eastern, Classical, Standard, none, date pipe orTomorrow).plan
        }
      ).flatten
      // hourly standard tournaments!
      // (-1 to 6).toList.flatMap { hourDelta =>
      //   val date = rightNow plusHours hourDelta
      //   val hour = date.getHourOfDay
      //   List(
      //     at(date, hour) map { date =>
      //       Schedule(Hourly, Blitz, Standard, none, date).plan
      //     },
      //     at(date, hour) collect {
      //       case date if hour % 2 == 0 => Schedule(Hourly, Rapid, Standard, none, date).plan
      //     }
      //   ).flatten
      // }
      // hourly limited tournaments!
      //(-1 to 6).toList
      //  .flatMap { hourDelta =>
      //    val date = rightNow plusHours hourDelta
      //    val hour = date.getHourOfDay
      //    val speed = hour % 4 match {
      //      case 0 => Bullet
      //      case 1 => SuperBlitz
      //      case 2 => Blitz
      //      case _ => Rapid
      //    }
      //    List(
      //      1500 -> 0,
      //      1700 -> 1,
      //      2000 -> 2
      //    ).flatMap {
      //      case (rating, hourDelay) =>
      //        val perf = Schedule.Speed toPerfType speed
      //        val conditions = Condition.All(
      //          nbRatedGame = Condition.NbRatedGame(perf.some, 20).some,
      //          maxRating = Condition.MaxRating(perf, rating).some,
      //          minRating = none,
      //          titled = none,
      //          teamMember = none
      //        )
      //        at(date, hour) ?? { date =>
      //          val finalDate = date plusHours hourDelay
      //          if (speed == Bullet)
      //            List(
      //              Schedule(Hourly, speed, Standard, none, finalDate, conditions).plan,
      //              Schedule(Hourly, speed, Standard, none, finalDate plusMinutes 30, conditions)
      //                .plan(_.copy(clock = shogi.Clock.Config(60, 1)))
      //            )
      //          else
      //            List(
      //              Schedule(Hourly, speed, Standard, none, finalDate, conditions).plan
      //            )
      //        }
      //    }
      //  }
      //  .map {
      //    // No berserk for rating-limited tournaments
      //    // Because berserking lowers the player rating
      //    _ map { _.copy(noBerserk = true) }
      //  },
    ).flatten filter { _.schedule.at isAfter rightNow }
  }

  private[tournament] def pruneConflicts(scheds: List[Tournament], newTourns: List[Tournament]) = {
    newTourns
      .foldLeft(List[Tournament]()) { case (tourns, t) =>
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