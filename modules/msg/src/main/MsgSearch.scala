package lila.msg

import reactivemongo.api.bson.*

import lila.common.Bus
import lila.core.LightUser
import lila.core.misc.clas.ClasBus
import lila.core.user.KidMode
import lila.core.userId.UserSearch
import lila.db.dsl.{ *, given }

final class MsgSearch(
    colls: MsgColls,
    userCache: lila.core.user.CachedApi,
    lightUserApi: lila.core.user.LightUserApi,
    relationApi: lila.core.relation.RelationApi
)(using Executor, Scheduler):

  import BsonHandlers.{ *, given }

  def apply(q: String)(using me: Me, kid: KidMode): Fu[MsgSearch.Result] =
    if kid.yes then forKid(q)
    else
      val search = UserSearch.read(q)
      searchThreads(q)
        .zip(search.so(searchFriends))
        .zip(search.so(searchUsers))
        .map:
          case ((threads, friends), users) =>
            MsgSearch
              .Result(
                threads,
                friends.filterNot(f => threads.exists(_.other.is(f))).take(10),
                users.filterNot(u => u.is(me) || friends.exists(_.is(u))).take(10)
              )

  private def forKid(q: String)(using me: Me): Fu[MsgSearch.Result] = for
    threads <- searchThreads(q)
    allMates <- Bus.ask[Set[UserId], ClasBus] { ClasBus.ClasMatesAndTeachers(me, _) }
    lower = q.toLowerCase
    mateIds = allMates.view.filter(_.value.startsWith(lower)).toList.take(15)
    mates <- lightUserApi.asyncMany(mateIds)
  yield MsgSearch.Result(threads, mates.flatten, Nil)

  val empty = MsgSearch.Result(Nil, Nil, Nil)

  private def searchThreads(q: String)(using me: Me): Fu[List[MsgThread]] =
    colls.thread
      .find:
        $doc(
          "users" -> $doc(
            $eq(me.userId),
            "$regex" -> BSONRegex(s"^${java.util.regex.Pattern.quote(q)}", "")
          ),
          selectNotDeleted
        )
      .sort($sort.desc("lastMsg.date"))
      .hint:
        colls.thread.hint(
          $doc(
            "users" -> 1,
            "lastMsg.date" -> -1
          )
        )
      .cursor[MsgThread](ReadPref.sec)
      .list(5)

  private def searchFriends(q: UserSearch)(using me: Me): Fu[List[LightUser]] =
    relationApi.searchFollowedBy(me, q, 15).flatMap(lightUserApi.asyncMany).dmap(_.flatten)

  private def searchUsers(q: UserSearch): Fu[List[LightUser]] =
    userCache.userIdsLike(q).flatMap(lightUserApi.asyncMany).dmap(_.flatten)

object MsgSearch:

  case class Result(
      threads: List[MsgThread],
      friends: List[LightUser],
      users: List[LightUser]
  )
