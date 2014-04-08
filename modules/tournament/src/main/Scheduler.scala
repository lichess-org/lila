package lila.tournament

import akka.actor._
import org.joda.time.DateTime
import scala.concurrent.duration._

import actorApi._

private[tournament] final class Scheduler(api: TournamentApi) extends Actor {

  import Schedule.Freq._
  import Schedule.Speed._

  def receive = {

    case ScheduleNow => TournamentRepo.scheduled.map(_.map(_.schedule).flatten) foreach { dbScheds =>

      val today = DateTime.now.withTimeAtStartOfDay
      val lastDayOfMonth = today.dayOfMonth.withMaximumValue
      val lastFridayOfCurrentMonth = lastDayOfMonth.minusDays((lastDayOfMonth.getDayOfWeek + 1) % 7)
      val nextFriday = today.plusDays((13 - today.getDayOfWeek) % 7)
      val tomorrow = today plusDays 1
      val nextHour = (DateTime.now withMinuteOfHour 0 plusHours 1).getHourOfDay

      val scheds = List(
        Schedule(Monthly, Bullet, at(lastFridayOfCurrentMonth, 15)),
        Schedule(Monthly, Blitz, at(lastFridayOfCurrentMonth, 16)),
        Schedule(Monthly, Slow, at(lastFridayOfCurrentMonth, 18)),
        Schedule(Weekly, Bullet, at(nextFriday, 15)),
        Schedule(Weekly, Blitz, at(nextFriday, 16)),
        Schedule(Weekly, Slow, at(nextFriday, 18)),
        Schedule(Daily, Bullet, at(tomorrow, 15)),
        Schedule(Daily, Blitz, at(tomorrow, 16)),
        Schedule(Daily, Slow, at(tomorrow, 18)),
        Schedule(Hourly, Bullet, at(today, nextHour)),
        Schedule(Hourly, Blitz, at(today, nextHour, 30))
      ).foldLeft(List[Schedule]()) {
          case (scheds, sched) if dbScheds.exists(_.at == sched.at) => scheds
          case (scheds, sched) if scheds.exists(_.at == sched.at)   => scheds
          case (scheds, sched)                                      => sched :: scheds
        }

      scheds foreach api.createScheduled
    }
  }

  def at(day: DateTime, hour: Int, minute: Int = 0) =
    day withHourOfDay hour withMinuteOfHour minute
}
