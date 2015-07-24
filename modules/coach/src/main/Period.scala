package lila.coach

import org.joda.time.DateTime
import scalaz.NonEmptyList

// contains aggregated data over (up to) 100 games
case class Period(
    userId: String,
    data: UserStat,
    from: DateTime,
    to: DateTime,
    computedAt: DateTime) {

  def merge(o: Period) = copy(
    data = data merge o.data,
    from = from,
    to = o.to)

  def isFresh = DateTime.now minusDays 1 isBefore computedAt
  def isStale = !isFresh

  def nbGames = data.results.base.nbGames
}

object Period {

  case class Computation(period: Period, data: UserStat.Computation) {

    def aggregate(p: RichPov) = copy(data = data aggregate p)

    def run = period.copy(data = data.run)

    def nbGames = data.nbGames
  }
  def initComputation(userId: String, pov: RichPov) =
    Computation(build(userId, pov), UserStat.emptyComputation aggregate pov)

  def build(userId: String, pov: RichPov) = Period(
    userId = userId,
    data = UserStat.empty,
    from = pov.pov.game.updatedAtOrCreatedAt,
    to = pov.pov.game.updatedAtOrCreatedAt,
    computedAt = DateTime.now)
}

case class Periods(periods: NonEmptyList[Period]) {

  lazy val period: Period = periods.tail.foldLeft(periods.head)(_ merge _)
}

object Periods {

  case class Computation(
      userId: String,
      period: Option[Period.Computation],
      periods: List[Period]) {

    def aggregate(p: RichPov) = ((period, periods, p) match {
      case (None, acc, pov)                              => Period.initComputation(userId, p) -> acc
      case (Some(comp), acc, pov) if comp.nbGames >= 100 => Period.initComputation(userId, p) -> (comp.run :: acc)
      case (Some(comp), acc, pov)                        => comp.aggregate(p) -> acc
    }) match {
      case (comp, acc) => copy(
        period = comp.some,
        periods = acc)
    }

    def run = period.map(_ -> periods).flatMap {
      case (comp, Nil) if comp.nbGames == 0     => None
      case (comp, p :: ps) if comp.nbGames == 0 => Periods(NonEmptyList(p, ps: _*)).some
      case (comp, Nil)                          => Periods(NonEmptyList(comp.run)).some
      case (comp, periods)                      => Periods(NonEmptyList(comp.run, periods: _*)).some
    }
  }
  def initComputation(userId: String) = Computation(userId, None, Nil)

  def build(userId: String, pov: RichPov) = Period(
    userId = userId,
    data = UserStat.empty,
    from = pov.pov.game.updatedAtOrCreatedAt,
    to = pov.pov.game.updatedAtOrCreatedAt,
    computedAt = DateTime.now)
}
