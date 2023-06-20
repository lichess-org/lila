package lila.msg

import reactivemongo.api.bson.*
import reactivemongo.api.ReadPreference

import lila.common.LightUser
import lila.db.dsl.{ *, given }
import lila.user.Me
import lila.common.Bus
import lila.hub.actorApi.clas.ClasMatesAndTeachers

final class MsgSearch(
    colls: MsgColls,
    userCache: lila.user.Cached,
    lightUserApi: lila.user.LightUserApi,
    relationApi: lila.relation.RelationApi
)(using Executor, Scheduler):

  import BsonHandlers.given

  def apply(q: String)(using me: Me): Fu[MsgSearch.Result] =
    if me.kid then forKid(q)
    else
      searchThreads(q) zip UserStr.read(q).so(searchFriends(_)) zip searchUsers(UserStr(q)) map {
        case ((threads, friends), users) =>
          MsgSearch
            .Result(
              threads,
              friends.filterNot(f => threads.exists(_.other is f)) take 10,
              users.filterNot(u => u.is(me) || friends.exists(_ is u)) take 10
            )
      }

  private def forKid(q: String)(using me: Me): Fu[MsgSearch.Result] = for
    threads  <- searchThreads(q)
    allMates <- Bus.ask[Set[UserId]]("clas") { ClasMatesAndTeachers(me, _) }
    lower   = q.toLowerCase
    mateIds = allMates.view.filter(_.value startsWith lower).toList take 15
    mates <- lightUserApi asyncMany mateIds
  yield MsgSearch.Result(threads, mates.flatten, Nil)

  val empty = MsgSearch.Result(Nil, Nil, Nil)

  private def searchThreads(q: String)(using me: Me): Fu[List[MsgThread]] =
    colls.thread
      .find(
        $doc(
          "users" -> $doc(
            $eq(me.userId),
            "$regex" -> BSONRegex(s"^${java.util.regex.Pattern.quote(q)}", "")
          ),
          "del" $ne me.userId
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

  private def searchFriends(q: UserStr)(using me: Me): Fu[List[LightUser]] =
    relationApi.searchFollowedBy(me, q, 15) flatMap lightUserApi.asyncMany dmap (_.flatten)

  private def searchUsers(q: UserStr): Fu[List[LightUser]] =
    userCache.userIdsLike(q) flatMap lightUserApi.asyncMany dmap (_.flatten)

object MsgSearch:

  case class Result(
      threads: List[MsgThread],
      friends: List[LightUser],
      users: List[LightUser]
  )
