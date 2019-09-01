package lidraughts.app
package mashup

import reactivemongo.api.collections.bson.BSONBatchCommands.{ AggregationFramework => AF }
import reactivemongo.api.ReadPreference
import reactivemongo.bson._
import scala.concurrent.duration._

import lidraughts.db.dsl._
import lidraughts.forum.MiniForumPost
import lidraughts.team.{ Team, RequestRepo, MemberRepo, RequestWithUser, TeamApi }
import lidraughts.user.{ User, UserRepo }

case class TeamInfo(
    mine: Boolean,
    createdByMe: Boolean,
    requestedByMe: Boolean,
    requests: List[RequestWithUser],
    forumNbPosts: Int,
    forumPosts: List[MiniForumPost]
) {

  def hasRequests = requests.nonEmpty

  def userIds = forumPosts.flatMap(_.userId)
}

final class TeamInfoApi(
    api: TeamApi,
    getForumNbPosts: String => Fu[Int],
    getForumPosts: String => Fu[List[MiniForumPost]],
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    memberColl: Coll,
    userColl: Coll
) {

  def apply(team: Team, me: Option[User]): Fu[TeamInfo] = for {
    requests ← (team.enabled && me.??(m => team.isCreator(m.id))) ?? api.requestsWithUsers(team)
    mine <- me.??(m => api.belongsTo(team.id, m.id))
    requestedByMe ← !mine ?? me.??(m => RequestRepo.exists(team.id, m.id))
    forumNbPosts ← getForumNbPosts(team.id)
    forumPosts ← getForumPosts(team.id)
  } yield TeamInfo(
    mine = mine,
    createdByMe = ~me.map(m => team.isCreator(m.id)),
    requestedByMe = requestedByMe,
    requests = requests,
    forumNbPosts = forumNbPosts,
    forumPosts = forumPosts
  )
}
