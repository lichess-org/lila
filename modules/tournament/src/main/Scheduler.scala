package lila.tournament

import akka.actor._
import akka.pattern.pipe
import org.joda.time.{ DateTime, Duration }
import scala.concurrent.duration._

import actorApi._
import chess.StartingPosition

/**
 * I'm afraid times are GMT+2
 */
private[tournament] final class Scheduler(api: TournamentApi) extends Actor {

  import Schedule.Freq._
  import Schedule.Speed._
  import Schedule.Season._
  import chess.variant._

  def marathonDates = List(
    // Spring -> Saturday of the weekend after Orthodox Easter Sunday
    // Summer -> first Saturday of August
    // Autumn -> Saturday of weekend before the weekend Halloween falls on (c.f. half-term holidays)
    // Winter -> 28 December, convenient day in the space between Boxing Day and New Year's Day
    Summer -> day(2015, 8, 1),
    Autumn -> day(2015, 10, 24),
    Winter -> day(2015, 12, 28),
    Spring -> day(2016, 4, 16),
    Summer -> day(2016, 8, 6),
    Autumn -> day(2016, 10, 22),
    Winter -> day(2016, 12, 28)
  )

  def receive = {

    case ScheduleNow =>
      TournamentRepo.scheduledUnfinished.map(_.flatMap(_.schedule)) map
        ScheduleNowWith.apply pipeTo self

    case ScheduleNowWith(dbScheds) =>

      val rightNow = DateTime.now
      val today = rightNow.withTimeAtStartOfDay
      val tomorrow = rightNow plusDays 1
      val lastDayOfMonth = today.dayOfMonth.withMaximumValue
      val firstDayOfMonth = today.dayOfMonth.withMinimumValue
      val lastSundayOfCurrentMonth = lastDayOfMonth.minusDays(lastDayOfMonth.getDayOfWeek % 7)
      val firstSundayOfCurrentMonth = firstDayOfMonth.plusDays(7 - firstDayOfMonth.getDayOfWeek)
      val nextSaturday = today.plusDays((13 - today.getDayOfWeek) % 7)
      val prevSaturday = nextSaturday.minusDays(7)
      val prevSunday = nextSaturday.minusDays(6)

      def orTomorrow(date: DateTime) = if (date isBefore rightNow) date plusDays 1 else date
      def orNextWeek(date: DateTime) = if (date isBefore rightNow) date plusWeeks 1 else date
      def orNextShift(date: DateTime) = date.plusHours(
        21 * math.ceil(new Duration(date, rightNow).getMillis().toDouble / (21*60*60*1000)).toInt
      )

      val std = StartingPosition.initial
      val opening1 = StartingPosition.randomFeaturable
      val opening2 = StartingPosition.randomFeaturable

      val nextSchedules: List[Schedule] = List(

        // Schedule(Marathon, Blitz, Standard, std, at(firstSundayOfCurrentMonth, 2, 0) |> orNextMonth),

        List( // monthly tournaments!
          Schedule(Monthly, Bullet, Standard, std, at(lastSundayOfCurrentMonth, 18)),
          Schedule(Monthly, SuperBlitz, Standard, std, at(lastSundayOfCurrentMonth, 19)),
          Schedule(Monthly, Blitz, Standard, std, at(lastSundayOfCurrentMonth, 20)),
          Schedule(Monthly, Classical, Standard, std, at(lastSundayOfCurrentMonth, 21))
        ),

        List( // weekly tournaments!
          Schedule(Weekly, Bullet, Standard, std, at(nextSaturday, 18) |> orNextWeek),
          Schedule(Weekly, SuperBlitz, Standard, std, at(nextSaturday, 19) |> orNextWeek),
          Schedule(Weekly, Blitz, Standard, std, at(nextSaturday, 20) |> orNextWeek),
          Schedule(Weekly, Classical, Standard, std, at(nextSaturday, 21) |> orNextWeek),
          Schedule(Weekly, Blitz, Chess960, std, at(nextSaturday, 22) |> orNextWeek)
        ),

        List( // daily tournaments!
          Schedule(Daily, Bullet, Standard, std, at(prevSaturday, 18) |> orNextShift),
          Schedule(Daily, SuperBlitz, Standard, std, at(prevSaturday, 19) |> orNextShift),
          Schedule(Daily, Blitz, Standard, std, at(prevSaturday, 20) |> orNextShift),
          Schedule(Daily, Classical, Standard, std, at(prevSaturday, 21) |> orNextShift)
        ),

        List( // daily variant tournaments!
          Schedule(Daily, Blitz, Chess960, std, at(prevSaturday, 22) |> orNextShift),
          Schedule(Daily, Blitz, KingOfTheHill, std, at(prevSaturday, 23) |> orNextShift),
          Schedule(Daily, Blitz, ThreeCheck, std, at(prevSunday, 0) |> orNextShift),
          Schedule(Daily, Blitz, Antichess, std, at(prevSunday, 1) |> orNextShift),
          Schedule(Daily, Blitz, Atomic, std, at(prevSunday, 2) |> orNextShift),
          Schedule(Daily, Blitz, Horde, std, at(prevSunday, 3) |> orNextShift)
        ),

        List( // nightly tournaments!
          Schedule(Nightly, Bullet, Standard, std, at(prevSunday, 6) |> orNextShift),
          Schedule(Nightly, SuperBlitz, Standard, std, at(prevSunday, 7) |> orNextShift),
          Schedule(Nightly, Blitz, Standard, std, at(prevSunday, 8) |> orNextShift),
          Schedule(Nightly, Classical, Standard, std, at(prevSunday, 9) |> orNextShift)
        ),

        List( // nightly variant tournaments!
          Schedule(Nightly, Blitz, Chess960, std, at(today, 10) |> orTomorrow),
          Schedule(Nightly, Blitz, KingOfTheHill, std, at(today, 11) |> orTomorrow),
          Schedule(Nightly, Blitz, ThreeCheck, std, at(today, 12) |> orTomorrow),
          Schedule(Nightly, Blitz, Antichess, std, at(today, 13) |> orTomorrow),
          Schedule(Nightly, Blitz, Atomic, std, at(today, 14) |> orTomorrow),
          Schedule(Nightly, Blitz, Horde, std, at(today, 15) |> orTomorrow)
        ),

        List( // random opening replaces hourly 2 times a day
          11 -> opening1,
          23 -> opening2
        ).flatMap {
            case (hour, opening) => List(
              Schedule(Hourly, Bullet, Standard, opening, at(today, hour) |> orTomorrow),
              Schedule(Hourly, SuperBlitz, Standard, opening, at(today, hour) |> orTomorrow),
              Schedule(Hourly, Blitz, Standard, opening, at(today, hour) |> orTomorrow)
            )
          },

        // hourly tournaments!
        (0 to 6).toList.flatMap { hourDelta =>
          val date = rightNow plusHours hourDelta
          val hour = date.getHourOfDay
          List(
            Schedule(Hourly, Bullet, Standard, std, at(date, hour)),
            Schedule(Hourly, Bullet, Standard, std, at(date, hour, 30)),
            Schedule(Hourly, SuperBlitz, Standard, std, at(date, hour)),
            Schedule(Hourly, Blitz, Standard, std, at(date, hour))
          )
        }

      ).flatten

      nextSchedules.foldLeft(List[Schedule]()) {
        case (scheds, sched) if sched.at.isBeforeNow      => scheds
        case (scheds, sched) if overlaps(sched, dbScheds) => scheds
        case (scheds, sched) if overlaps(sched, scheds)   => scheds
        case (scheds, sched)                              => sched :: scheds
      } foreach api.createScheduled
  }

  private case class ScheduleNowWith(dbScheds: List[Schedule])

  private def endsAt(s: Schedule) = s.at plus ((~Schedule.durationFor(s)).toLong * 60 * 1000)
  private def interval(s: Schedule) = new org.joda.time.Interval(s.at, endsAt(s))
  private def overlaps(s: Schedule, ss: Seq[Schedule]) = ss exists {
    case s2 if s.sameSpeed(s2) && s.sameVariant(s2) => interval(s) overlaps interval(s2)
    case _ => false
  }

  private def day(year: Int, month: Int, day: Int) = new DateTime(year, month, day, 0, 0)

  private def at(day: DateTime, hour: Int, minute: Int = 0) =
    day withHourOfDay hour withMinuteOfHour minute withSecondOfMinute 0 withMillisOfSecond 0
}
