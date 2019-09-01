package lila.app
package mashup

import reactivemongo.api.collections.bson.BSONBatchCommands.{ AggregationFramework => AF }
import reactivemongo.api.ReadPreference
import reactivemongo.bson._
import scala.concurrent.duration._

import lila.db.dsl._
import lila.forum.MiniForumPost
import lila.team.{ Team, RequestRepo, MemberRepo, RequestWithUser, TeamApi }
import lila.user.{ User, UserRepo }

case class TeamInfo(
    mine: Boolean,
    createdByMe: Boolean,
    requestedByMe: Boolean,
    requests: List[RequestWithUser],
    toints: Int,
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
    asyncCache: lila.memo.AsyncCache.Builder,
    memberColl: Coll,
    userColl: Coll
) {

  private case class Cachable(toints: Int)

  private def fetchCachable(id: String): Fu[Cachable] =
    memberColl.aggregateOne(
      AF.Match($doc("team" -> id)),
      List(
        AF.Lookup(
          from = userColl.name,
          localField = "user",
          foreignField = "_id",
          as = "user"
        ),
        AF.UnwindField("user"),
        AF.Match($doc(
          "user.enabled" -> true,
          "user.engine" $ne true,
          "user.booster" $ne true
        )),
        AF.Group(BSONNull)("toints" -> AF.SumField("user.toints"))
      ),
      ReadPreference.secondaryPreferred
    ).map {
        _ flatMap {
          _.getAs[Int]("toints")
        } map Cachable.apply
      } map (_ | Cachable(0))

  private val cache = asyncCache.multi[String, Cachable](
    name = "teamInfo",
    f = fetchCachable,
    expireAfter = _.ExpireAfterWrite(1 hour),
    resultTimeout = 5 seconds
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
    toints = cachable.toints,
    forumNbPosts = forumNbPosts,
    forumPosts = forumPosts
  )
}
