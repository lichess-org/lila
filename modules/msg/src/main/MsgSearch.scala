package lila.msg

import reactivemongo.api.bson.*
import reactivemongo.api.ReadPreference

import lila.common.LightUser
import lila.db.dsl.{ *, given }
import lila.user.User
import lila.common.Bus
import lila.hub.actorApi.clas.ClasMatesAndTeachers

final class MsgSearch(
    colls: MsgColls,
    userCache: lila.user.Cached,
    lightUserApi: lila.user.LightUserApi,
    relationApi: lila.relation.RelationApi
)(using ec: Executor, scheduler: Scheduler):

  import BsonHandlers.given

  def apply(me: User, q: String): Fu[MsgSearch.Result] =
    if (me.kid) forKid(me, q)
    else
      searchThreads(me, q) zip UserStr.read(q).??(searchFriends(me, _)) zip searchUsers(UserStr(q)) map {
        case ((threads, friends), users) =>
          MsgSearch
            .Result(
              threads,
              friends.filterNot(f => threads.exists(_.other(me) == f.id)) take 10,
              users.filterNot(u => u.id == me.id || friends.exists(_.id == u.id)) take 10
            )
      }

  private def forKid(me: User, q: String): Fu[MsgSearch.Result] = for {
    threads  <- searchThreads(me, q)
    allMates <- Bus.ask[Set[UserId]]("clas") { ClasMatesAndTeachers(me.id, _) }
    lower   = q.toLowerCase
    mateIds = allMates.view.filter(_.value startsWith lower).toList take 15
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

  private def searchFriends(me: User, q: UserStr): Fu[List[LightUser]] =
    relationApi.searchFollowedBy(me, q, 15) flatMap lightUserApi.asyncMany dmap (_.flatten)

  private def searchUsers(q: UserStr): Fu[List[LightUser]] =
    userCache.userIdsLike(q) flatMap lightUserApi.asyncMany dmap (_.flatten)

object MsgSearch:

  case class Result(
      threads: List[MsgThread],
      friends: List[LightUser],
      users: List[LightUser]
  )
