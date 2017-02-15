package lila.evalCache

import org.joda.time.{ DateTime, Days }

import lila.security.Granter
import lila.user.User

private final class EvalCacheTruster {

  import EvalCacheEntry.Trust

  private val LOWER = Trust(-9999)
  private val HIGHER = Trust(9999)

  def apply(user: User): Trust =
    if (user.lameOrTroll) LOWER
    else if (Granter(_.SeeReport)(user)) HIGHER
    else Trust {
      seniorityBonus(user) +
        patronBonus(user) +
        titleBonus(user) +
        nbGamesBonus(user)
    }

  def makeTrusted(user: User) = EvalCacheEntry.TrustedUser(apply(user), user)

  def shouldPut(user: User) = !apply(user).isTooLow

  // 0 days = -1
  // 1 month = 0
  // 1 year = 2.46
  // 2 years = 3.89
  private def seniorityBonus(user: User) =
    math.sqrt(Days.daysBetween(user.createdAt, DateTime.now).getDays.toDouble / 30) - 1

  private def patronBonus(user: User) = (~user.planMonths * 5) atMost 20

  private def titleBonus(user: User) = user.hasTitle ?? 20

  // 0 games    = -1
  // 100 games  = 0
  // 200 games  = 0.41
  // 1000 games = 2.16
  private def nbGamesBonus(user: User) =
    math.sqrt(user.count.game / 100) - 1
}
