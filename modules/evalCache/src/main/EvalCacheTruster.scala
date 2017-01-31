package lila.evalCache

import org.joda.time.{ DateTime, Months }

import lila.security.Granter
import lila.user.User

private final class EvalCacheTruster {

  import EvalCacheEntry.Trust

  private val LOWER = Trust(-9999)
  private val HIGHER = Trust(9999)

  def apply(user: User): Trust =
    if (user.createdAt isAfter DateTime.now.minusDays(14)) LOWER
    else if (user.lameOrTroll) LOWER
    else if (Granter(_.SeeReport)(user)) HIGHER
    else Trust {
      1 +
        seniorityBonus(user) +
        patronBonus(user) +
        titleBonus(user) +
        nbGamesBonus(user)
    }

  def shouldPut(user: User) = !apply(user).isTooLow

  private def seniorityBonus(user: User) =
    Months.monthsBetween(user.createdAt, DateTime.now).getMonths atMost 10

  private def patronBonus(user: User) = (~user.planMonths * 5) atMost 20

  private def titleBonus(user: User) = user.hasTitle ?? 20

  private def nbGamesBonus(user: User) = (user.count.game / 1000) atMost 5
}
