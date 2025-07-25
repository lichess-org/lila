package lila.rating

import lila.core.perf.{ UserPerfs, UserWithPerfs }
import lila.core.user.{ LightPerf, WithPerf }
import lila.rating.UserPerfsExt.bestRating

object UserWithPerfs:

  extension (p: UserWithPerfs)
    def usernameWithBestRating = s"${p.username} (${p.perfs.bestRating})"
    def hasVariantRating = lila.rating.PerfType.variants.exists(p.perfs.apply(_).nonEmpty)
    def titleUsernameWithBestRating =
      p.title.fold(p.usernameWithBestRating): t =>
        s"$t ${p.usernameWithBestRating}"
    def lightPerf(key: PerfKey) =
      val perf = p.perfs(key)
      LightPerf(p.light, key, perf.intRating, perf.progress)
    def only(pk: PerfKey) = WithPerf(p.user, p.perfs(pk))

  def apply(user: User, perfs: Option[UserPerfs]): UserWithPerfs =
    new UserWithPerfs(user, perfs | lila.rating.UserPerfs.default(user.id))
  given UserIdOf[UserWithPerfs] = _.user.id
