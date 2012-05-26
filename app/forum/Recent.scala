package lila
package forum

import memo.Builder
import user.User
import security.{ Permission, Granter }

import scalaz.effects._

final class Recent(env: ForumEnv, timeout: Int) {

  val nb = 30

  private val cache = Builder.cache[Boolean, List[PostView]](timeout, staff ⇒
    fetch(staff).unsafePerformIO
  )

  def apply(user: Option[User]): List[PostView] =
    cache get Granter.option(Permission.StaffForum)(user)

  val invalidate: IO[Unit] = io(cache.invalidateAll)

  private def fetch(staff: Boolean): IO[List[PostView]] = for {
    posts ← env.postRepo.recent(nb)
    views ← (posts map env.postApi.view).sequence
  } yield views collect {
    case Some(v) if (staff || v.categ.slug != "staff") ⇒ v
  }
}
