package lila.tournament

import akka.actor._
import akka.pattern.pipe
import org.joda.time.DateTime
import scala.concurrent.duration._

import actorApi._
import chess.StartingPosition

/**
 * I'm afraid times are GMT+2
 */
private final class TournamentScheduler private (api: TournamentApi) extends Actor {

  import Schedule.Freq._
  import Schedule.Speed._
  import Schedule.Season._
  import chess.variant._

  // def marathonDates = List(
  // Spring -> Saturday of the weekend after Orthodox Easter Sunday
  // Summer -> first Saturday of August
  // Autumn -> Saturday of weekend before the weekend Halloween falls on (c.f. half-term holidays)
  // Winter -> 28 December, convenient day in the space between Boxing Day and New Year's Day
  // Summer -> day(2015, 8, 1),
  // Autumn -> day(2015, 10, 24),
  // Winter -> day(2015, 12, 28),
  // Spring -> day(2016, 4, 16),
  // Summer -> day(2016, 8, 6),
  // Autumn -> day(2016, 10, 22),
  // Winter -> day(2016, 12, 28)
  // )

  def receive = {

    case ScheduleNow =>
      TournamentRepo.scheduledUnfinished.map(_.flatMap(_.schedule)) map
        ScheduleNowWith.apply pipeTo self

    case ScheduleNowWith(dbScheds) => try {

      val rightNow = DateTime.now
      val today = rightNow.withTimeAtStartOfDay
      val tomorrow = rightNow plusDays 1
      val lastDayOfMonth = today.dayOfMonth.withMaximumValue
      val firstDayOfMonth = today.dayOfMonth.withMinimumValue
      val lastSundayOfCurrentMonth = lastDayOfMonth.minusDays(lastDayOfMonth.getDayOfWeek % 7)
      val firstSundayOfCurrentMonth = firstDayOfMonth.plusDays(7 - firstDayOfMonth.getDayOfWeek)
      val nextSaturday = today.plusDays((13 - today.getDayOfWeek) % 7)

      def orTomorrow(date: DateTime) = if (date isBefore rightNow) date plusDays 1 else date
      def orNextWeek(date: DateTime) = if (date isBefore rightNow) date plusWeeks 1 else date

      val isHalloween = today.getMonthOfYear == 10 && today.getDayOfMonth == 31

      val std = StartingPosition.initial
      val opening1 = isHalloween ? StartingPosition.presets.halloween | StartingPosition.randomFeaturable
      val opening2 = isHalloween ? StartingPosition.presets.frankenstein | StartingPosition.randomFeaturable

      val nextSchedules: List[Schedule] = List(

        List( // monthly tournaments!
          at(lastSundayOfCurrentMonth, 18) map { date => Schedule(Monthly, Bullet, Standard, std, date) },
          at(lastSundayOfCurrentMonth, 19) map { date => Schedule(Monthly, SuperBlitz, Standard, std, date) },
          at(lastSundayOfCurrentMonth, 20) map { date => Schedule(Monthly, Blitz, Standard, std, date) },
          at(lastSundayOfCurrentMonth, 21) map { date => Schedule(Monthly, Classical, Standard, std, date) },
          at(lastSundayOfCurrentMonth, 22) map { date => Schedule(Monthly, Blitz, Crazyhouse, std, date) }
        ).flatten,

        List( // weekly tournaments!
          at(nextSaturday, 18) map { date => Schedule(Weekly, Bullet, Standard, std, date |> orNextWeek) },
          at(nextSaturday, 19) map { date => Schedule(Weekly, SuperBlitz, Standard, std, date |> orNextWeek) },
          at(nextSaturday, 20) map { date => Schedule(Weekly, Blitz, Standard, std, date |> orNextWeek) },
          at(nextSaturday, 21) map { date => Schedule(Weekly, Classical, Standard, std, date |> orNextWeek) },
          at(nextSaturday, 22) map { date => Schedule(Weekly, Blitz, Crazyhouse, std, date |> orNextWeek) }
        ).flatten,

        List( // daily tournaments!
          at(today, 18) map { date => Schedule(Daily, Bullet, Standard, std, date |> orTomorrow) },
          at(today, 19) map { date => Schedule(Daily, SuperBlitz, Standard, std, date |> orTomorrow) },
          at(today, 20) map { date => Schedule(Daily, Blitz, Standard, std, date |> orTomorrow) },
          at(today, 21) map { date => Schedule(Daily, Classical, Standard, std, date |> orTomorrow) },
          at(today, 22) map { date => Schedule(Daily, HyperBullet, Standard, std, date |> orTomorrow) }
        ).flatten,

        List( // daily variant tournaments!
          at(today, 22) map { date => Schedule(Daily, Blitz, Crazyhouse, std, date |> orTomorrow) },
          at(today, 23) map { date => Schedule(Daily, Blitz, Chess960, std, date |> orTomorrow) },
          at(today, 0) map { date => Schedule(Daily, Blitz, KingOfTheHill, std, date |> orTomorrow) },
          at(tomorrow, 1) map { date => Schedule(Daily, Blitz, ThreeCheck, std, date) },
          at(tomorrow, 2) map { date => Schedule(Daily, Blitz, Antichess, std, date) },
          at(tomorrow, 3) map { date => Schedule(Daily, Blitz, Atomic, std, date) },
          at(tomorrow, 4) map { date => Schedule(Daily, Blitz, Horde, std, date) },
          at(tomorrow, 5) map { date => Schedule(Daily, SuperBlitz, RacingKings, std, date) }
        ).flatten,

        List( // eastern tournaments!
          at(today, 6) map { date => Schedule(Eastern, Bullet, Standard, std, date |> orTomorrow) },
          at(today, 7) map { date => Schedule(Eastern, SuperBlitz, Standard, std, date |> orTomorrow) },
          at(today, 8) map { date => Schedule(Eastern, Blitz, Standard, std, date |> orTomorrow) },
          at(today, 9) map { date => Schedule(Eastern, Classical, Standard, std, date |> orTomorrow) }
        ).flatten,

        (isHalloween ? // replace more thematic tournaments on halloween
          List(
            3 -> opening1,
            7 -> opening2,
            11 -> opening1,
            15 -> opening2,
            19 -> opening1,
            23 -> opening2
          ) |
            List( // random opening replaces hourly 2 times a day
              11 -> opening1,
              23 -> opening2
            )).flatMap {
                case (hour, opening) => List(
                  at(today, hour) map { date => Schedule(Hourly, Bullet, Standard, opening, date |> orTomorrow) },
                  at(today, hour) map { date => Schedule(Hourly, SuperBlitz, Standard, opening, date |> orTomorrow) },
                  at(today, hour) map { date => Schedule(Hourly, Blitz, Standard, opening, date |> orTomorrow) },
                  at(today, hour) map { date => Schedule(Hourly, Classical, Standard, opening, date |> orTomorrow) }
                ).flatten
              },

        // hourly standard tournaments!
        (0 to 6).toList.flatMap { hourDelta =>
          val date = rightNow plusHours hourDelta
          val hour = date.getHourOfDay
          val bulletType = Set(1, 7, 13, 19)(hour).fold[Schedule.Speed](HyperBullet, Bullet)
          List(
            at(date, hour) map { date => Schedule(Hourly, Bullet, Standard, std, date) },
            at(date, hour, 30) map { date => Schedule(Hourly, bulletType, Standard, std, date) },
            at(date, hour) map { date => Schedule(Hourly, SuperBlitz, Standard, std, date) },
            at(date, hour) map { date => Schedule(Hourly, Blitz, Standard, std, date) },
            at(date, hour) flatMap { date => (hour % 2 == 0) option Schedule(Hourly, Classical, Standard, std, date) }
          ).flatten
        },

        // hourly crazyhouse tournaments!
        (0 to 6).toList.flatMap { hourDelta =>
          val date = rightNow plusHours hourDelta
          val hour = date.getHourOfDay
          val speed = hour % 3 match {
            case 0 => Bullet
            case 1 => SuperBlitz
            case _ => Blitz
          }
          List(
            at(date, hour) map { date => Schedule(Hourly, speed, Crazyhouse, std, date) },
            at(date, hour, 30) flatMap { date => (speed == Bullet) option Schedule(Hourly, speed, Crazyhouse, std, date) }
          ).flatten
        }

      ).flatten

      nextSchedules.foldLeft(List[Schedule]()) {
        case (scheds, sched) if sched.at.isBeforeNow      => scheds
        case (scheds, sched) if overlaps(sched, dbScheds) => scheds
        case (scheds, sched) if overlaps(sched, scheds)   => scheds
        case (scheds, sched)                              => sched :: scheds
      } foreach api.createScheduled
    }
    catch {
      case e: org.joda.time.IllegalInstantException =>
        logger.error(s"failed to schedule all: ${e.getMessage}")
    }
  }

  private case class ScheduleNowWith(dbScheds: List[Schedule])

  private def endsAt(s: Schedule) = s.at plus ((~Schedule.durationFor(s)).toLong * 60 * 1000)
  private def interval(s: Schedule) = new org.joda.time.Interval(s.at, endsAt(s))
  private def overlaps(s: Schedule, ss: Seq[Schedule]) = ss exists {
    case s2 if s.variant.exotic && s.sameVariant(s2) => interval(s) overlaps interval(s2)
    case s2 if s.similarSpeed(s2) && s.sameVariant(s2) => interval(s) overlaps interval(s2)
    case _ => false
  }

  private def at(day: DateTime, hour: Int, minute: Int = 0): Option[DateTime] = try {
    Some(day withHourOfDay hour withMinuteOfHour minute withSecondOfMinute 0 withMillisOfSecond 0)
  }
  catch {
    case e: Exception =>
      logger.error(s"failed to schedule one: ${e.getMessage}")
      None
  }
}

private object TournamentScheduler {

  def start(system: ActorSystem, api: TournamentApi) = {
    val ref = system.actorOf(Props(new TournamentScheduler(api)))
    system.scheduler.schedule(5 minutes, 5 minutes, ref, ScheduleNow)
  }
}
