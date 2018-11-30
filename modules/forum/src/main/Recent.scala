package lila.forum

import scala.concurrent.duration.FiniteDuration

import lila.security.{ Permission, Granter => MasterGranter }
import lila.user.User

private[forum] final class Recent(
    postApi: PostApi,
    ttl: FiniteDuration,
    nb: Int,
    asyncCache: lila.memo.AsyncCache.Builder,
    categIds: List[String]
) {

  private type GetTeamIds = String => Fu[List[String]]

  def apply(user: Option[User], getTeams: GetTeamIds): Fu[List[MiniForumPost]] =
    userCacheKey(user, getTeams) flatMap cache.get

  def team(teamId: String): Fu[List[MiniForumPost]] = {
    // prepend empty language list
    val key = ";" + teamSlug(teamId)
    cache get key
  }

  def invalidate: Unit = cache.invalidateAll

  private def userCacheKey(user: Option[User], getTeams: GetTeamIds): Fu[String] =
    (user.map(_.id) ?? getTeams).map { teamIds =>
      user.fold("en")(_.langs.mkString(",")) :: {
        (user.??(_.troll) ?? List("[troll]")) :::
          categIds :::
          (teamIds.map(teamSlug)(scala.collection.breakOut): List[String])
      } mkString ";"
    }

  private val cache = asyncCache.clearable(
    name = "forum.recent",
    f = fetch,
    expireAfter = _.ExpireAfterAccess(ttl)
  )

  private def parseLangs(langStr: String) = langStr.split(",").toList filter (_.nonEmpty)

  private def fetch(key: String): Fu[List[MiniForumPost]] =
    (key.split(";").toList match {
      case langs :: "[troll]" :: categs => PostRepoTroll.recentInCategs(nb)(categs, parseLangs(langs))
      case langs :: categs => PostRepo.recentInCategs(nb)(categs, parseLangs(langs))
      case categs => PostRepo.recentInCategs(nb)(categs, parseLangs("en"))
    }) flatMap postApi.miniPosts
}
