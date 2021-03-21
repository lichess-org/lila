package lila.forum

import scala.concurrent.duration._

import lila.memo.CacheApi._
import lila.user.User

final class ForumRecent(
    postApi: PostApi,
    postRepo: PostRepo,
    cacheApi: lila.memo.CacheApi,
    categIds: List[String]
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val nb: Int = 10

  private type GetTeamIds = String => Fu[List[String]]

  def apply(user: Option[User], getTeams: GetTeamIds): Fu[List[MiniForumPost]] =
    userCacheKey(user, getTeams) flatMap cache.get

  private val teamCache = cacheApi[String, List[MiniForumPost]](512, "forum.team.recent") {
    _.expireAfterWrite(1 hour)
      .buildAsyncFuture { id =>
        postRepo.recentInCateg(teamSlug(id), 6) flatMap postApi.miniPosts
      }
  }

  def team(teamId: String): Fu[List[MiniForumPost]] = teamCache get teamId

  def invalidate(): Unit = {
    cache.invalidateAll()
    teamCache.invalidateAll()
  }

  private val defaultLang = "en"

  private def userCacheKey(user: Option[User], getTeams: GetTeamIds): Fu[String] =
    (user.map(_.id) ?? getTeams).map { teamIds =>
      val langs =
        user
          .flatMap(_.realLang)
          .map(_.language)
          .filter(defaultLang !=)
          .fold(defaultLang)(l => s"$defaultLang,$l")
      val parts = langs :: categIds ::: teamIds.view.map(teamSlug).toList
      parts.mkString(";")
    }

  private val cache = cacheApi[String, List[MiniForumPost]](1024, "forum.recent") {
    _.expireAfterWrite(1 hour)
      .buildAsyncFuture(fetch)
  }

  private def parseLangs(langStr: String) = langStr.split(",").toList.filter(_.nonEmpty)

  private def fetch(key: String): Fu[List[MiniForumPost]] =
    (key.split(";").toList match {
      case langs :: categs => postRepo.recentInCategs(nb)(categs, parseLangs(langs))
      case categs          => postRepo.recentInCategs(nb)(categs, parseLangs("en"))
    }) flatMap postApi.miniPosts
}
