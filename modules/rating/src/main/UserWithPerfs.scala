package lila.rating

import lila.core.user.LightPerf
import lila.core.user.WithPerf
import lila.rating.PerfExt.*

case class UserWithPerfs(user: User, perfs: UserPerfs):
  export user.*
  def usernameWithBestRating = s"$username (${perfs.bestRating})"
  def hasVariantRating       = lila.rating.PerfType.variants.exists(perfs.apply(_).nonEmpty)
  def titleUsernameWithBestRating =
    title.fold(usernameWithBestRating): t =>
      s"$t $usernameWithBestRating"
  def lightPerf(key: PerfKey) =
    perfs(key).map: perf =>
      LightPerf(light, key, perf.intRating, perf.progress)
  def only(pt: PerfType) = WithPerf(user, perfs(pt))

object UserWithPerfs:
  def apply(user: User, perfs: Option[UserPerfs]): UserWithPerfs =
    new UserWithPerfs(user, perfs | UserPerfs.default(user.id))
  given UserIdOf[UserWithPerfs] = _.user.id
