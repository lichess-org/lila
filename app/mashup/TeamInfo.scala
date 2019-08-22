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
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    memberColl: Coll,
    userColl: Coll
) {

  private case class Cachable(bestUserIds: List[User.ID], toints: Int)

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
        Facet($doc(
          "toints" -> $arr(AF.Group(BSONNull)("toints" -> AF.SumField("user.toints")).makePipe),
          "bestUids" -> $arr(
            AF.Sort(AF.Descending("user.perfs.standard.gl.r")).makePipe,
            AF.Limit(10).makePipe,
            AF.Project($doc(
              "_id" -> false,
              "user._id" -> true
            )).makePipe
          )
        ))
      ),
      ReadPreference.secondaryPreferred
    ).map { docOpt =>
        for {
          doc <- docOpt
          tointsDocs <- doc.getAs[List[Bdoc]]("toints")
          tointsDoc <- tointsDocs.headOption
          toints <- tointsDoc.getAs[Int]("toints")
          bestUidsDocs <- doc.getAs[List[Bdoc]]("bestUids")
          bestUids = bestUidsDocs.flatMap {
            _.getAs[Bdoc]("user").flatMap(_.getAs[User.ID]("_id"))
          }
        } yield Cachable(bestUids, toints)
      } map (_ | Cachable(Nil, 0))

  private val cache = asyncCache.multi[String, Cachable](
    name = "teamInfo",
    f = fetchCachable,
    expireAfter = _.ExpireAfterWrite(20 minutes)
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
