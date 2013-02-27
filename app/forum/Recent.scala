package lila
package forum

import memo.Builder
import user.User
import security.{ Permission, Granter }

import scalaz.effects._

final class Recent(
    categRepo: CategRepo,
    postRepo: PostRepo,
    postApi: PostApi,
    timeout: Int) {

  private val nb = 20

  private def teamSlug(id: String) = "team-" + id

  private lazy val publicCategIds =
    categRepo.withTeams(Nil).unsafePerformIO.map(_.slug) filterNot ("staff" ==)

  private lazy val staffCategIds = "staff" :: publicCategIds

  private val cache = Builder.cache[String, List[PostLiteView]](timeout, key ⇒
    fetch(key).unsafePerformIO
  )

  def apply(user: Option[User], teams: User ⇒ List[String]): IO[List[PostLiteView]] = io {
    cache get {
      Granter.option(Permission.StaffForum)(user).fold(
        staffCategIds, publicCategIds
      ) ::: (~user.map(teams)).map(teamSlug)
    }.mkString(";")
  }

  def team(teamId: String): IO[List[PostLiteView]] = io { cache get teamSlug(teamId) }

  val invalidate: IO[Unit] = io(cache.invalidateAll)

  private def fetch(key: String): IO[List[PostLiteView]] =
    postRepo.recentInCategs(nb)(key.split(";").toList) flatMap postApi.liteViews
}
