package lila.evalCache

import scala.concurrent.duration._
import org.joda.time.{ DateTime, Days }

import lila.security.Granter
import lila.user.{ User, UserRepo }

private final class EvalCacheTruster(asyncCache: lila.memo.AsyncCache.Builder) {

  import EvalCacheEntry.{ Trust, TrustedUser }

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

  private val userIdCache = asyncCache.multi[User.ID, Option[TrustedUser]](
    name = "evalCache.userIdTrustCache  ",
    f = userId => UserRepo named userId map2 makeTrusted,
    expireAfter = _.ExpireAfterWrite(10 minutes),
    resultTimeout = 10 seconds
  )

  def cachedTrusted(userId: User.ID): Fu[Option[TrustedUser]] = userIdCache get userId

  def makeTrusted(user: User) = TrustedUser(apply(user), user)

  def shouldPut(user: User) = apply(user).isEnough

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
