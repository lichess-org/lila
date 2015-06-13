package lila.tournament

import akka.actor._
import akka.pattern.pipe
import org.joda.time.DateTime
import scala.concurrent.duration._

import actorApi._
import chess.StartingPosition

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
    Summer -> new DateTime(2015, 8, 1),
    Autumn -> new DateTime(2015, 10, 24),
    Winter -> new DateTime(2015, 12, 28),
    Spring -> new DateTime(2016, 4, 16),
    Summer -> new DateTime(2016, 8, 6),
    Autumn -> new DateTime(2016, 10, 22),
    Winter -> new DateTime(2016, 12, 28)
  )

  def receive = {

    case ScheduleNow =>
      TournamentRepo.scheduled.map(_.flatMap(_.schedule)) map ScheduleNowWith.apply pipeTo self

    case ScheduleNowWith(dbScheds) =>

      val rightNow = DateTime.now
      val today = rightNow.withTimeAtStartOfDay
      val tomorrow = rightNow plusDays 1
      val lastDayOfMonth = today.dayOfMonth.withMaximumValue
      val firstDayOfMonth = today.dayOfMonth.withMinimumValue
      val lastSundayOfCurrentMonth = lastDayOfMonth.minusDays(lastDayOfMonth.getDayOfWeek % 7)
      val firstSundayOfCurrentMonth = firstDayOfMonth.plusDays(7 - firstDayOfMonth.getDayOfWeek)
      val nextSaturday = today.plusDays((13 - today.getDayOfWeek) % 7)
      val nextHourDate = rightNow plusHours 1
      val nextHour = nextHourDate.getHourOfDay

      def orTomorrow(date: DateTime) = if (date isBefore rightNow) date plusDays 1 else date
      def orNextWeek(date: DateTime) = if (date isBefore rightNow) date plusWeeks 1 else date
      def orNextMonth(date: DateTime) = if (date isBefore rightNow) date plusMonths 1 else date

      val std = StartingPosition.initial
      val opening1 = StartingPosition.random
      val opening2 = StartingPosition.random

      List(
        Schedule(Monthly, Bullet, Standard, std, at(lastSundayOfCurrentMonth, 18, 0) |> orNextMonth),
        Schedule(Monthly, SuperBlitz, Standard, std, at(lastSundayOfCurrentMonth, 19, 0) |> orNextMonth),
        Schedule(Monthly, Blitz, Standard, std, at(lastSundayOfCurrentMonth, 20, 0) |> orNextMonth),
        Schedule(Monthly, Classical, Standard, std, at(lastSundayOfCurrentMonth, 21, 0) |> orNextMonth),

        // Schedule(Marathon, Blitz, Standard, at(firstSundayOfCurrentMonth, 2, 0) |> orNextMonth),

        Schedule(Weekly, Bullet, Standard, std, at(nextSaturday, 18) |> orNextWeek),
        Schedule(Weekly, SuperBlitz, Standard, std, at(nextSaturday, 19) |> orNextWeek),
        Schedule(Weekly, Blitz, Standard, std, at(nextSaturday, 20) |> orNextWeek),
        Schedule(Weekly, Classical, Standard, std, at(nextSaturday, 21) |> orNextWeek),
        Schedule(Weekly, Blitz, Chess960, std, at(nextSaturday, 22) |> orNextWeek),

        Schedule(Daily, Bullet, Standard, std, at(today, 18) |> orTomorrow),
        Schedule(Daily, SuperBlitz, Standard, std, at(today, 19) |> orTomorrow),
        Schedule(Daily, Blitz, Standard, std, at(today, 20) |> orTomorrow),
        Schedule(Daily, Classical, Standard, std, at(today, 21) |> orTomorrow),
        Schedule(Daily, Blitz, Chess960, std, at(today, 22) |> orTomorrow),
        Schedule(Daily, Blitz, Horde, std, at(today, 23) |> orTomorrow),
        Schedule(Daily, Blitz, Atomic, std, at(tomorrow, 0) |> orTomorrow),
        Schedule(Daily, Blitz, Antichess, std, at(tomorrow, 1) |> orTomorrow),

        Schedule(Nightly, Bullet, Standard, std, at(today, 6) |> orTomorrow),
        Schedule(Nightly, SuperBlitz, Standard, std, at(today, 7) |> orTomorrow),
        Schedule(Nightly, Blitz, Standard, std, at(today, 8) |> orTomorrow),
        Schedule(Nightly, Classical, Standard, std, at(today, 9) |> orTomorrow),

        // random opening replaces hourly once a day
        Schedule(Hourly, Bullet, Standard, opening1, at(today, 17)),
        Schedule(Hourly, SuperBlitz, Standard, opening1, at(today, 17)),
        Schedule(Hourly, Blitz, Standard, opening1, at(today, 17)),
        // and once a night
        Schedule(Hourly, Bullet, Standard, opening2, at(today, 5)),
        Schedule(Hourly, SuperBlitz, Standard, opening2, at(today, 5)),
        Schedule(Hourly, Blitz, Standard, opening2, at(today, 5)),

        Schedule(Hourly, Bullet, Standard, std, at(nextHourDate, nextHour)),
        Schedule(Hourly, Bullet, Standard, std, at(nextHourDate, nextHour, 30)),
        Schedule(Hourly, SuperBlitz, Standard, std, at(nextHourDate, nextHour)),
        Schedule(Hourly, Blitz, Standard, std, at(nextHourDate, nextHour))

      ).foldLeft(List[Schedule]()) {
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

  private def at(day: DateTime, hour: Int, minute: Int = 0) =
    day withHourOfDay hour withMinuteOfHour minute withSecondOfMinute 0 withMillisOfSecond 0
}
