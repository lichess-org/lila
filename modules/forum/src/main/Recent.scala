package lila.forum

import scala.concurrent.duration.Duration

import spray.caching.{ LruCache, Cache }

import lila.security.{ Permission, Granter => MasterGranter }
import lila.user.User

private[forum] final class Recent(
    postApi: PostApi,
    ttl: Duration,
    nb: Int,
    publicCategIds: List[String]) {

  private type GetTeams = String => Set[String]

  def apply(user: Option[User], getTeams: GetTeams): Fu[List[MiniForumPost]] =
    userCacheKey(user, getTeams) |> { key => cache(key)(fetch(key)) }

  def team(teamId: String): Fu[List[MiniForumPost]] = {
    // prepend empty language list
    val key = ";" + teamSlug(teamId)
    cache(key)(fetch(key))
  }

  def invalidate: Funit = fuccess(cache.clear)

  private def userCacheKey(user: Option[User], getTeams: GetTeams): String =
    user.fold("en")(_.langs.mkString(",")) :: {
      (user.??(_.troll) ?? List("[troll]")) :::
        (user ?? MasterGranter(Permission.StaffForum)).fold(staffCategIds, publicCategIds) :::
        ((user.map(_.id) ?? getTeams) map teamSlug).toList
    } mkString ";"

  private lazy val staffCategIds = "staff" :: publicCategIds

  private val cache: Cache[List[MiniForumPost]] = LruCache(timeToLive = ttl)

  private def parseLangs(langStr: String) = langStr.split(",").toList filter (_.nonEmpty)

  private def fetch(key: String): Fu[List[MiniForumPost]] =
    (key.split(";").toList match {
      case langs :: "[troll]" :: categs => PostRepoTroll.recentInCategs(nb)(categs, parseLangs(langs))
      case langs :: categs              => PostRepo.recentInCategs(nb)(categs, parseLangs(langs))
      case categs                       => PostRepo.recentInCategs(nb)(categs, parseLangs("en"))
    }) flatMap postApi.miniPosts
}
