package lila.msg

import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference

import lila.common.LightUser
import lila.db.dsl._
import lila.user.User
import lila.common.Bus
import lila.hub.actorApi.clas.ClasMatesAndTeachers
import lila.hub.actorApi.user.KidId

final class MsgSearch(
    colls: MsgColls,
    userCache: lila.user.Cached,
    lightUserApi: lila.user.LightUserApi,
    relationApi: lila.relation.RelationApi
)(implicit ec: scala.concurrent.ExecutionContext, system: akka.actor.ActorSystem) {

  import BsonHandlers._

  def apply(me: User, q: String): Fu[MsgSearch.Result] =
    if (me.kid) forKid(me, q)
    else
      searchThreads(me, q) zip searchFriends(me, q) zip searchUsers(me, q) map {
        case threads ~ friends ~ users =>
          MsgSearch
            .Result(
              threads,
              friends.filterNot(f => threads.exists(_.other(me) == f.id)) take 10,
              users.filterNot(u => u.id == me.id || friends.exists(_.id == u.id)) take 10
            )
      }

  private def forKid(me: User, q: String): Fu[MsgSearch.Result] = for {
    threads  <- searchThreads(me, q)
    allMates <- Bus.ask[Set[User.ID]]("clas") { ClasMatesAndTeachers(KidId(me.id), _) }
    lower   = q.toLowerCase
    mateIds = allMates.view.filter(_ startsWith lower).toList take 15
    mates <- lightUserApi asyncMany mateIds
  } yield MsgSearch.Result(threads, mates.flatten, Nil)

  val empty = MsgSearch.Result(Nil, Nil, Nil)

  private def searchThreads(me: User, q: String): Fu[List[MsgThread]] =
    colls.thread
      .find(
        $doc(
          "users" -> $doc(
            $eq(me.id),
            "$regex" -> BSONRegex(s"^$q", "")
          ),
          "del" $ne me.id
        )
      )
      .sort($sort desc "lastMsg.date")
      .hint(
        colls.thread hint $doc(
          "users"        -> 1,
          "lastMsg.date" -> -1
        )
      )
      .cursor[MsgThread](ReadPreference.secondaryPreferred)
      .list(5)

  private def searchFriends(me: User, q: String): Fu[List[LightUser]] =
    relationApi.searchFollowedBy(me, q, 15) flatMap lightUserApi.asyncMany dmap (_.flatten)

  private def searchUsers(me: User, q: String): Fu[List[LightUser]] =
    userCache.userIdsLike(q) flatMap lightUserApi.asyncMany dmap (_.flatten)
}

object MsgSearch {

  case class Result(
      threads: List[MsgThread],
      friends: List[LightUser],
      users: List[LightUser]
  )
}
