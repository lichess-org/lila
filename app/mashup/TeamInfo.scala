package lidraughts.app
package mashup

import scala.concurrent.duration._

import lidraughts.forum.MiniForumPost
import lidraughts.team.{ Team, RequestRepo, MemberRepo, RequestWithUser, TeamApi }
import lidraughts.user.{ User, UserRepo }

case class TeamInfo(
    mine: Boolean,
    createdByMe: Boolean,
    requestedByMe: Boolean,
    requests: List[RequestWithUser],
    bestUserIds: List[User.ID],
    toints: Int,
    forumNbPosts: Int,
    forumPosts: List[MiniForumPost]
) {

  def hasRequests = requests.nonEmpty

  def userIds = bestUserIds ::: forumPosts.flatMap(_.userId)
}

final class TeamInfoApi(
    api: TeamApi,
    getForumNbPosts: String => Fu[Int],
    getForumPosts: String => Fu[List[MiniForumPost]],
    asyncCache: lidraughts.memo.AsyncCache.Builder
) {

  private case class Cachable(bestUserIds: List[User.ID], toints: Int)

  private def fetchCachable(id: String): Fu[Cachable] = for {
    userIds ← (MemberRepo userIdsByTeam id)
    bestUserIds ← UserRepo.idsByIdsSortRating(userIds, 10)
    toints ← UserRepo.idsSumToints(userIds)
  } yield Cachable(bestUserIds, toints)

  private val cache = asyncCache.multi[String, Cachable](
    name = "teamInfo",
    f = fetchCachable,
    expireAfter = _.ExpireAfterWrite(10 minutes)
  )

  def apply(team: Team, me: Option[User]): Fu[TeamInfo] = for {
    requests ← (team.enabled && me.??(m => team.isCreator(m.id))) ?? api.requestsWithUsers(team)
    mine <- me.??(m => api.belongsTo(team.id, m.id))
    requestedByMe ← !mine ?? me.??(m => RequestRepo.exists(team.id, m.id))
    cachable <- cache get team.id
    forumNbPosts ← getForumNbPosts(team.id)
    forumPosts ← getForumPosts(team.id)
  } yield TeamInfo(
    mine = mine,
    createdByMe = ~me.map(m => team.isCreator(m.id)),
    requestedByMe = requestedByMe,
    requests = requests,
    bestUserIds = cachable.bestUserIds,
    toints = cachable.toints,
    forumNbPosts = forumNbPosts,
    forumPosts = forumPosts
  )
}
