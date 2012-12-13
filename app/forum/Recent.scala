package lila
package forum

import memo.Builder
import user.User
import security.{ Permission, Granter }

import scalaz.effects._

final class Recent(postRepo: PostRepo, postApi: PostApi, timeout: Int) {

  private val nb = 20

  private val Public = "$public"
  private val Staff = "$staff"
  private def teamSlug(id: String) = "team-" + id

  private lazy val publicCategIds = List(
    "game-analysis",
    "general-chess-discussion",
    "lichess-feedback",
    "off-topic-discussion")

  private lazy val staffCategIds = "staff" :: publicCategIds

  private val cache = Builder.cache[String, List[PostView]](timeout, key ⇒
    fetch(key).unsafePerformIO
  )

  def apply(user: Option[User]): IO[List[PostView]] = io {
    cache get Granter.option(Permission.StaffForum)(user).fold(Staff, Public)
  }

  def team(teamId: String): IO[List[PostView]] = io {
    cache get teamId
  }

  val invalidate: IO[Unit] = io(cache.invalidateAll)

  private def fetch(key: String): IO[List[PostView]] = for {
    posts ← postRepo.recentInCategs(nb)(key match {
      case Public ⇒ publicCategIds
      case Staff  ⇒ staffCategIds
      case teamId ⇒ List(teamSlug(teamId))
    })
    views ← postApi views posts
  } yield views
}
