package lidraughts.tournament

import akka.actor._
import akka.pattern.pipe
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants._
import scala.concurrent.duration._

import actorApi._
import draughts.StartingPosition

private final class TournamentScheduler private (api: TournamentApi) extends Actor {

  import Schedule.Freq._
  import Schedule.Speed._
  import Schedule.Plan
  import draughts.variant._

  /* Month plan:
   * First week: Shield standard tournaments
   * Second week: Yearly tournament
   * Third week: Shield varitn tournaments
   * Last week: Monthly tournaments
   */

  // def marathonDates = List(
  // Spring -> Saturday of the weekend after Orthodox Easter Sunday
  // Summer -> first Saturday of August
  // Autumn -> Saturday of weekend before the weekend Halloween falls on (c.f. half-term holidays)
  // Winter -> 28 December, convenient day in the space between Boxing Day and New Year's Day
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

      class OfMonth(fromNow: Int) {
        val firstDay = today.plusMonths(fromNow).dayOfMonth.withMinimumValue
        val lastDay = firstDay.dayOfMonth.withMaximumValue

        val firstWeek = firstDay.plusDays(7 - (firstDay.getDayOfWeek - 1) % 7)
        val secondWeek = firstWeek plusDays 7
        val thirdWeek = secondWeek plusDays 7
        val lastWeek = lastDay.minusDays((lastDay.getDayOfWeek - 1) % 7)
      }
      val thisMonth = new OfMonth(0)
      val nextMonth = new OfMonth(1)

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

      val farFuture = today plusMonths 7

      val birthday = new DateTime(2018, 8, 13, 12, 0, 0)

      // all dates UTC
      val nextPlans: List[Schedule.Plan] = List(

        /*List( // legendary tournaments!
          at(birthday.withYear(today.getYear), 12) map orNextYear map { date =>
            val yo = date.getYear - 2010
            Schedule(Unique, Rapid, Standard, std, date) plan {
              _.copy(
                name = s"${date.getYear} Lidraughts Anniversary",
                minutes = 12 * 60,
                spotlight = Spotlight(
                  headline = s"$yo years of free draughts!",
                  description = s"""
We've had $yo great draughts years together!

Thank you all, you rock!"""
                ).some
              )
            }
          }
        ).flatten,*/

        List( // yearly tournaments!
          secondWeekOf(JANUARY).withDayOfWeek(MONDAY) -> Bullet -> Standard,
          secondWeekOf(FEBRUARY).withDayOfWeek(TUESDAY) -> SuperBlitz -> Standard,
          secondWeekOf(MARCH).withDayOfWeek(WEDNESDAY) -> Blitz -> Standard,
          secondWeekOf(APRIL).withDayOfWeek(THURSDAY) -> Rapid -> Standard,
          secondWeekOf(MAY).withDayOfWeek(FRIDAY) -> Classical -> Standard,
          secondWeekOf(JUNE).withDayOfWeek(SATURDAY) -> HyperBullet -> Standard,

          secondWeekOf(JULY).withDayOfWeek(MONDAY) -> Bullet -> Standard,
          secondWeekOf(AUGUST).withDayOfWeek(TUESDAY) -> SuperBlitz -> Standard,
          secondWeekOf(SEPTEMBER).withDayOfWeek(WEDNESDAY) -> Blitz -> Standard,
          secondWeekOf(OCTOBER).withDayOfWeek(THURSDAY) -> Rapid -> Standard,
          secondWeekOf(NOVEMBER).withDayOfWeek(FRIDAY) -> Classical -> Standard,
          secondWeekOf(DECEMBER).withDayOfWeek(SATURDAY) -> HyperBullet -> Standard
        ).flatMap {
            case ((day, speed), variant) =>
              at(day, 17) filter farFuture.isAfter map { date =>
                Schedule(Yearly, speed, variant, std, date).plan
              }
          },

        List(thisMonth, nextMonth).flatMap { month =>
          List(
            List( // monthly standard tournaments!
              month.lastWeek.withDayOfWeek(MONDAY) -> UltraBullet,
              month.lastWeek.withDayOfWeek(TUESDAY) -> HyperBullet,
              month.lastWeek.withDayOfWeek(WEDNESDAY) -> Bullet,
              month.lastWeek.withDayOfWeek(THURSDAY) -> SuperBlitz,
              month.lastWeek.withDayOfWeek(FRIDAY) -> Blitz,
              month.lastWeek.withDayOfWeek(SATURDAY) -> Rapid
            ).flatMap {
                case (day, speed) => at(day, 17) map { date =>
                  Schedule(Monthly, speed, Standard, std, date).plan
                }
              },

            List( // monthly variant tournaments!
              month.firstWeek.withDayOfWeek(SATURDAY) -> Antidraughts,
              month.firstWeek.withDayOfWeek(SUNDAY) -> Frisian
            ).flatMap {
                case (day, variant) => at(day, 18) map { date =>
                  Schedule(Monthly, Blitz, variant, std, date).plan
                }
              }

          ).flatten
        },

        List( // weekly standard tournaments!
          nextMonday -> UltraBullet,
          nextTuesday -> Bullet,
          nextWednesday -> SuperBlitz,
          nextThursday -> Blitz,
          nextFriday -> Rapid,
          nextSaturday -> HyperBullet
        ).flatMap {
            case (day, speed) => at(day, 17) map { date =>
              Schedule(Weekly, speed, Standard, std, date |> orNextWeek).plan
            }
          },

        List( // weekly variant tournaments!
          nextSaturday -> Antidraughts,
          nextSunday -> Frisian
        ).flatMap {
            case (day, variant) => at(day, 20) map { date =>
              Schedule(Weekly, Blitz, variant, std, date |> orNextWeek).plan
            }
          },

        List( // daily tournaments!
          at(today, 17) map { date => Schedule(Daily, Bullet, Standard, std, date |> orTomorrow).plan },
          at(today, 18) map { date => Schedule(Daily, SuperBlitz, Standard, std, date |> orTomorrow).plan }
        ).flatten,

        List( // daily variant tournaments!
          at(today, 20) map { date => Schedule(Daily, SuperBlitz, Frisian, std, date |> orTomorrow).plan },
          at(today, 21) map { date => Schedule(Daily, SuperBlitz, Antidraughts, std, date |> orTomorrow).plan }
        ).flatten,

        List( // eastern tournaments!
          at(today, 5) map { date => Schedule(Eastern, Bullet, Standard, std, date |> orTomorrow).plan },
          at(today, 6) map { date => Schedule(Eastern, SuperBlitz, Standard, std, date |> orTomorrow).plan }
        ).flatten,

        // hourly standard tournaments!
        (0 to 6).toList.flatMap { hourDelta =>
          val date = rightNow plusHours hourDelta
          val hour = date.getHourOfDay
          val bulletType = if (hour % 3 == 0) HyperBullet else if (hour % 3 == 2) HippoBullet else Bullet
          val blitzType = if (hour % 3 == 0) Blitz else SuperBlitz
          List(
            at(date, hour) map { date => Schedule(Hourly, bulletType, if (hour % 3 == 1) Frisian else Standard, std, date).plan }, //Frisian bullet
            at(date, hour, 30) collect { case date if bulletType != HippoBullet => Schedule(Hourly, Bullet, Standard, std, date).plan },
            //no hourly blitz during frisian
            at(date, hour) collect { case date if hour % 3 != 2 => Schedule(Hourly, blitzType, Standard, std, date).plan }
          ).flatten
        },

        // frisian tournaments every 3rd hour!
        (0 to 6).toList.flatMap { hourDelta =>
          val date = rightNow plusHours hourDelta
          val hour = date.getHourOfDay
          val speed = if (hour % 6 == 2) SuperBlitz else Blitz
          if (hour % 3 != 2) Nil
          else List(
            at(date, hour) map { date => Schedule(Hourly, speed, Frisian, std, date).plan }
          ).flatten
        }
      ).flatten

      nextPlans.map { plan =>
        plan.copy(schedule = Schedule addCondition plan.schedule)
      }.foldLeft(List[Plan]()) {
        case (plans, p) if p.schedule.at.isBeforeNow => plans
        case (plans, p) if overlaps(p.schedule, dbScheds) => plans
        case (plans, p) if overlaps(p.schedule, plans.map(_.schedule)) => plans
        case (plans, p) => p :: plans
      } foreach api.createFromPlan
    } catch {
      case e: org.joda.time.IllegalInstantException =>
        logger.error(s"failed to schedule all: ${e.getMessage}")
    }
  }

  private case class ScheduleNowWith(dbScheds: List[Schedule])

  private def endsAt(s: Schedule) = s.at plus (Schedule.durationFor(s).toLong * 60 * 1000)
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
    Some(day.withTimeAtStartOfDay plusHours hour plusMinutes minute)
  } catch {
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
