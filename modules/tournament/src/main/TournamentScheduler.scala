package lila.tournament

import akka.actor._
import akka.pattern.pipe
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants._
import scala.concurrent.duration._

import actorApi._
import chess.StartingPosition

private final class TournamentScheduler private (api: TournamentApi) extends Actor {

  import Schedule.Freq._
  import Schedule.Speed._
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
      val startOfYear = today.dayOfYear.withMinimumValue

      val lastDayOfMonth = today.dayOfMonth.withMaximumValue

      val lastWeekOfMonth = lastDayOfMonth.minusDays((lastDayOfMonth.getDayOfWeek - 1) % 7)

      def nextDayOfWeek(number: Int) = today.plusDays((number + 7 - today.getDayOfWeek) % 7)
      val nextMonday = nextDayOfWeek(1)
      val nextTuesday = nextDayOfWeek(2)
      val nextWednesday = nextDayOfWeek(3)
      val nextThursday = nextDayOfWeek(4)
      val nextFriday = nextDayOfWeek(5)
      val nextSaturday = nextDayOfWeek(6)
      val nextSunday = nextDayOfWeek(7)

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

      val farFuture = today plusMonths 5

      // all dates UTC
      val nextSchedules: List[Schedule] = List(

        List( // yearly tournaments!
          secondWeekOf(JANUARY).withDayOfWeek(MONDAY) -> Bullet -> Standard,
          secondWeekOf(FEBRUARY).withDayOfWeek(TUESDAY) -> SuperBlitz -> Standard,
          secondWeekOf(MARCH).withDayOfWeek(WEDNESDAY) -> Blitz -> Standard,
          secondWeekOf(APRIL).withDayOfWeek(THURSDAY) -> Classical -> Standard,
          secondWeekOf(MAY).withDayOfWeek(FRIDAY) -> HyperBullet -> Standard,
          secondWeekOf(JUNE).withDayOfWeek(SATURDAY) -> SuperBlitz -> Crazyhouse,

          secondWeekOf(JULY).withDayOfWeek(MONDAY) -> Bullet -> Standard,
          secondWeekOf(AUGUST).withDayOfWeek(TUESDAY) -> SuperBlitz -> Standard,
          secondWeekOf(SEPTEMBER).withDayOfWeek(WEDNESDAY) -> Blitz -> Standard,
          secondWeekOf(OCTOBER).withDayOfWeek(THURSDAY) -> Classical -> Standard,
          secondWeekOf(NOVEMBER).withDayOfWeek(FRIDAY) -> HyperBullet -> Standard,
          secondWeekOf(DECEMBER).withDayOfWeek(SATURDAY) -> SuperBlitz -> Crazyhouse
        ).flatMap {
            case ((day, speed), variant) =>
              at(day, 17) filter farFuture.isAfter map { date =>
                Schedule(Yearly, speed, variant, std, date)
              }
          },

        List( // monthly standard tournaments!
          lastWeekOfMonth.withDayOfWeek(MONDAY) -> Bullet,
          lastWeekOfMonth.withDayOfWeek(TUESDAY) -> SuperBlitz,
          lastWeekOfMonth.withDayOfWeek(WEDNESDAY) -> Blitz,
          lastWeekOfMonth.withDayOfWeek(THURSDAY) -> Classical,
          lastWeekOfMonth.withDayOfWeek(FRIDAY) -> HyperBullet
        ).flatMap {
            case (day, speed) => at(day, 17) map { date =>
              Schedule(Monthly, speed, Standard, std, date)
            }
          },

        List( // monthly variant tournaments!
          lastWeekOfMonth.withDayOfWeek(MONDAY) -> Chess960,
          lastWeekOfMonth.withDayOfWeek(TUESDAY) -> Crazyhouse,
          lastWeekOfMonth.withDayOfWeek(WEDNESDAY) -> KingOfTheHill,
          lastWeekOfMonth.withDayOfWeek(THURSDAY) -> ThreeCheck,
          lastWeekOfMonth.withDayOfWeek(FRIDAY) -> Antichess,
          lastWeekOfMonth.withDayOfWeek(SATURDAY) -> Atomic,
          lastWeekOfMonth.withDayOfWeek(SUNDAY) -> Horde
        ).flatMap {
            case (day, variant) => at(day, 19) map { date =>
              Schedule(Monthly, Blitz, variant, std, date)
            }
          },

        List( // weekly standard tournaments!
          nextMonday -> Bullet,
          nextTuesday -> SuperBlitz,
          nextWednesday -> Blitz,
          nextThursday -> Classical,
          nextFriday -> HyperBullet
        ).flatMap {
            case (day, speed) => at(day, 17) map { date =>
              Schedule(Weekly, speed, Standard, std, date |> orNextWeek)
            }
          },

        List( // weekly variant tournaments!
          nextMonday -> Chess960,
          nextTuesday -> Crazyhouse,
          nextWednesday -> KingOfTheHill,
          nextThursday -> ThreeCheck,
          nextFriday -> Antichess,
          nextSaturday -> Atomic,
          nextSunday -> Horde
        ).flatMap {
            case (day, variant) => at(day, 19) map { date =>
              Schedule(Weekly, Blitz, variant, std, date |> orNextWeek)
            }
          },

        List( // week-end elite tournaments!
          nextSaturday -> Bullet,
          nextSunday -> SuperBlitz
        ).flatMap {
            case (day, speed) => at(day, 17) map { date =>
              Schedule(Weekend, speed, Standard, std, date |> orNextWeek)
            }
          },

        List( // daily tournaments!
          at(today, 16) map { date => Schedule(Daily, Bullet, Standard, std, date |> orTomorrow) },
          at(today, 17) map { date => Schedule(Daily, SuperBlitz, Standard, std, date |> orTomorrow) },
          at(today, 18) map { date => Schedule(Daily, Blitz, Standard, std, date |> orTomorrow) },
          at(today, 19) map { date => Schedule(Daily, Classical, Standard, std, date |> orTomorrow) },
          at(today, 20) map { date => Schedule(Daily, HyperBullet, Standard, std, date |> orTomorrow) },
          at(today, 21) map { date => Schedule(Daily, UltraBullet, Standard, std, date |> orTomorrow) }
        ).flatten,

        List( // daily variant tournaments!
          at(today, 20) map { date => Schedule(Daily, Blitz, Crazyhouse, std, date |> orTomorrow) },
          at(today, 21) map { date => Schedule(Daily, Blitz, Chess960, std, date |> orTomorrow) },
          at(today, 22) map { date => Schedule(Daily, SuperBlitz, KingOfTheHill, std, date |> orTomorrow) },
          at(today, 23) map { date => Schedule(Daily, SuperBlitz, ThreeCheck, std, date |> orTomorrow) },
          at(today, 0) map { date => Schedule(Daily, SuperBlitz, Antichess, std, date |> orTomorrow) },
          at(tomorrow, 1) map { date => Schedule(Daily, SuperBlitz, Atomic, std, date) },
          at(tomorrow, 2) map { date => Schedule(Daily, SuperBlitz, Horde, std, date) },
          at(tomorrow, 3) map { date => Schedule(Daily, SuperBlitz, RacingKings, std, date) }
        ).flatten,

        List( // eastern tournaments!
          at(today, 4) map { date => Schedule(Eastern, Bullet, Standard, std, date |> orTomorrow) },
          at(today, 5) map { date => Schedule(Eastern, SuperBlitz, Standard, std, date |> orTomorrow) },
          at(today, 6) map { date => Schedule(Eastern, Blitz, Standard, std, date |> orTomorrow) },
          at(today, 7) map { date => Schedule(Eastern, Classical, Standard, std, date |> orTomorrow) }
        ).flatten,

        (isHalloween ? // replace more thematic tournaments on halloween
          List(
            1 -> StartingPosition.presets.halloween,
            5 -> StartingPosition.presets.frankenstein,
            9 -> StartingPosition.presets.halloween,
            13 -> StartingPosition.presets.frankenstein,
            17 -> StartingPosition.presets.halloween,
            21 -> StartingPosition.presets.frankenstein
          ) |
            List( // random opening replaces hourly 3 times a day
              3 -> opening(offset = 2),
              11 -> opening(offset = 1),
              19 -> opening(offset = 0)
            )).flatMap {
                case (hour, opening) => List(
                  at(today, hour) map { date => Schedule(Hourly, Bullet, Standard, opening, date |> orTomorrow) },
                  at(today, hour + 1) map { date => Schedule(Hourly, SuperBlitz, Standard, opening, date |> orTomorrow) },
                  at(today, hour + 2) map { date => Schedule(Hourly, Blitz, Standard, opening, date |> orTomorrow) },
                  at(today, hour + 3) map { date => Schedule(Hourly, Classical, Standard, opening, date |> orTomorrow) }
                ).flatten
              },

        // hourly standard tournaments!
        (0 to 6).toList.flatMap { hourDelta =>
          val date = rightNow plusHours hourDelta
          val hour = date.getHourOfDay
          // Hyperbullet hourlies avoid ultra, and overlap with daily hyper.
          val bulletType = if (hour % 4 == 0) HyperBullet else Bullet
          List(
            // Ultra hourlies avoid hyperbullet, and overlap with daily ultra.
            at(date, hour) collect { case date if hour % 8 == 5 => Schedule(Hourly, UltraBullet, Standard, std, date) },
            at(date, hour, 30) collect { case date if hour % 8 == 5 => Schedule(Hourly, UltraBullet, Standard, std, date) },
            at(date, hour) map { date => Schedule(Hourly, bulletType, Standard, std, date) },
            at(date, hour, 30) map { date => Schedule(Hourly, bulletType, Standard, std, date) },
            at(date, hour) map { date => Schedule(Hourly, SuperBlitz, Standard, std, date) },
            at(date, hour) map { date => Schedule(Hourly, Blitz, Standard, std, date) },
            at(date, hour) collect { case date if hour % 2 == 0 => Schedule(Hourly, Classical, Standard, std, date) }
          ).flatten
        },

        // hourly limited tournaments!
        (0 to 6).toList.flatMap { hourDelta =>
          val date = rightNow plusHours hourDelta
          val hour = date.getHourOfDay
          val speed = hour % 3 match {
            case 0 => SuperBlitz
            case 1 => Blitz
            case _ => Classical
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
                  minRating = none
                )
                at(date, hour) map { date =>
                  val finalDate = date plusHours hourDelay
                  Schedule(Hourly, speed, Standard, std, finalDate, conditions)
                }
            }
        },

        // hourly crazyhouse tournaments!
        (0 to 6).toList.flatMap { hourDelta =>
          val date = rightNow plusHours hourDelta
          val hour = date.getHourOfDay
          val speed = hour % 6 match {
            case 0 | 3 => Bullet
            case 1 | 4 => SuperBlitz
            case 5 => HippoBullet
            case _ => Blitz
          }
          List(
            at(date, hour) map { date => Schedule(Hourly, speed, Crazyhouse, std, date) },
            at(date, hour, 30) collect {
              case date if speed == Bullet =>
                Schedule(Hourly, if (hour == 18) HyperBullet else Bullet, Crazyhouse, std, date)
            }
          ).flatten
        }
      ).flatten

      nextSchedules.map { sched =>
        sched.copy(conditions = Schedule conditionFor sched)
      }.foldLeft(List[Schedule]()) {
        case (scheds, sched) if sched.at.isBeforeNow => scheds
        case (scheds, sched) if overlaps(sched, dbScheds) => scheds
        case (scheds, sched) if overlaps(sched, scheds) => scheds
        case (scheds, sched) => sched :: scheds
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
    // prevent daily && weekly on the same day
    case s2 if s.freq.isDailyOrBetter && s2.freq.isDailyOrBetter && s.sameVariantAndSpeed(s2) => s sameDay s2
    // overlapping same variant
    case s2 if s.variant.exotic && s.sameVariant(s2) => interval(s) overlaps interval(s2)
    // overlapping same rating limit
    case s2 if s2.hasMaxRating && s.sameMaxRating(s2) => interval(s) overlaps interval(s2)
    // overlapping similar
    case s2 if s.similarSpeed(s2) && s.sameVariant(s2) && s.sameMaxRating(s2) => interval(s) overlaps interval(s2)
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
    system.scheduler.schedule(1 minute, 5 minutes, ref, ScheduleNow)
  }
}
