package lila.forum

import lila.user.User
import lila.security.{ Permission, Granter ⇒ MasterGranter }

import scala.concurrent.duration.Duration
import spray.caching.{ LruCache, Cache }
import play.api.libs.concurrent.Execution.Implicits._

private[forum] final class Recent(postApi: PostApi, ttl: Duration) {

  private type GetTeams = User ⇒ Fu[List[String]]

  def apply(user: Option[User], getTeams: GetTeams): Fu[List[PostLiteView]] =
    userCacheKey(user, getTeams) flatMap { key ⇒
      cache.fromFuture(key)(fetch(key))
    }

  def team(teamId: String): Fu[List[PostLiteView]] =
    cache.fromFuture(teamSlug(teamId))(fetch(teamSlug(teamId)))

  def invalidate: Funit = fuccess(cache.clear)

  private implicit val timeout = makeTimeout.large
  private val nb = 20

  private def userCacheKey(user: Option[User], getTeams: GetTeams): Fu[String] =
    user zmap getTeams map { teams ⇒
      ((user zmap MasterGranter(Permission.StaffForum)).fold(
        staffCategIds, publicCategIds
      ) ::: (teams map teamSlug)) mkString ";"
    }

  private lazy val publicCategIds =
    CategRepo.withTeams(Nil).await.map(_.slug) filterNot ("staff" ==)

  private lazy val staffCategIds = "staff" :: publicCategIds

  private val cache: Cache[List[PostLiteView]] = LruCache(timeToLive = ttl)

  private def fetch(key: String): Fu[List[PostLiteView]] =
    PostRepo.recentInCategs(nb)(key.split(";").toList) flatMap postApi.liteViews
}
