package lila.msg

import reactivemongo.api.bson._

import lila.common.LightUser
import lila.db.dsl._
import lila.user.User

final class MsgSearch(
    colls: MsgColls,
    userCache: lila.user.Cached,
    lightUserApi: lila.user.LightUserApi,
    relationApi: lila.relation.RelationApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  def apply(me: User, q: String): Fu[MsgSearch.Result] =
    searchThreads(me, q) zip searchFriends(me, q) zip searchUsers(me, q) map {
      case ((threads, friends), users) =>
        MsgSearch
          .Result(
            threads,
            friends.filterNot(f => threads.exists(_.other(me) == f.id)) take 10,
            users.filterNot(u => u.id == me.id || friends.exists(_.id == u.id)) take 10
          )
    }

  val empty = MsgSearch.Result(Nil, Nil, Nil)

  private def searchThreads(me: User, q: String): Fu[List[MsgThread]] =
    colls.thread.ext
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
      .cursor[MsgThread]()
      .list(5)

  private def searchFriends(me: User, q: String): Fu[List[LightUser]] =
    !me.kid ?? {
      relationApi.searchFollowedBy(me, q, 15) flatMap lightUserApi.asyncMany dmap (_.flatten)
    }

  private def searchUsers(me: User, q: String): Fu[List[LightUser]] =
    !me.kid ?? {
      userCache.userIdsLike(q) flatMap lightUserApi.asyncMany dmap (_.flatten)
    }
}

object MsgSearch {

  case class Result(
      threads: List[MsgThread],
      friends: List[LightUser],
      users: List[LightUser]
  )
}
