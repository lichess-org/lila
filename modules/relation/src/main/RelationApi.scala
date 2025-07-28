package lila.relation

import reactivemongo.api.*
import reactivemongo.api.bson.*

import lila.common.Bus
import lila.common.Json.given
import lila.core.relation.Relation.{ Block, Follow }
import lila.core.relation.{ Relation, Relations }
import lila.core.timeline.{ Follow as FollowUser, Propagate }
import lila.core.user.UserApi
import lila.core.userId.UserSearch
import lila.db.dsl.{ *, given }
import lila.db.paginator.*
import lila.memo.CacheApi.*
import lila.relation.BSONHandlers.given

final class RelationApi(
    repo: RelationRepo,
    prefApi: lila.core.pref.PrefApi,
    cacheApi: lila.memo.CacheApi,
    userApi: UserApi,
    config: RelationConfig
)(using Executor)
    extends lila.core.relation.RelationApi(repo.coll):

  def fetchRelation(u1: UserId, u2: UserId): Fu[Option[Relation]] =
    (u1 != u2).so(coll.primitiveOne[Relation]($doc("u1" -> u1, "u2" -> u2), "r"))

  def fetchRelation[U1: UserIdOf, U2: UserIdOf](u1: U1, u2: U2): Fu[Option[Relation]] =
    fetchRelation(u1.id, u2.id)

  def fetchRelations(u1: UserId, u2: UserId): Fu[Relations] =
    fetchRelation(u2, u1).zip(fetchRelation(u1, u2)).dmap(Relations.apply)

  export repo.{
    blocking as fetchBlocking,
    following as fetchFollowing,
    freshFollowersFromSecondary,
    filterBlocked,
    removeAllFollowers
  }

  def accountTermination(user: User): Fu[Set[UserId]] = for
    followedIds <- fetchFollowing(user.id)
    _ <- repo.removeAllRelationsFrom(user.id)
    _ <- removeAllFollowers(user.id)
  yield followedIds

  def fetchFriends(userId: UserId): Fu[Set[UserId]] =
    coll
      .aggregateWith[Bdoc](readPreference = ReadPref.sec): framework =>
        import framework.*
        List(
          Match(
            $doc(
              "$or" -> $arr($doc("u1" -> userId), $doc("u2" -> userId)),
              "r" -> Follow
            )
          ),
          Group(BSONNull)(
            "u1" -> AddFieldToSet("u1"),
            "u2" -> AddFieldToSet("u2")
          ),
          Project($id($doc("$setIntersection" -> $arr("$u1", "$u2"))))
        )
      .headOption
      .map:
        ~_.flatMap(_.getAsOpt[Set[UserId]]("_id")) - userId

  def fetchFollows(u1: UserId, u2: UserId): Fu[Boolean] =
    (u1 != u2).so(coll.exists($doc("_id" -> makeId(u1, u2), "r" -> Follow)))

  def fetchBlocks(u1: UserId, u2: UserId): Fu[Boolean] =
    (u1 != u2).so(coll.exists($doc("_id" -> makeId(u1, u2), "r" -> Block)))

  def fetchAreFriends(u1: UserId, u2: UserId): Fu[Boolean] =
    fetchFollows(u1, u2) >>& fetchFollows(u2, u1)

  private val countFollowingCache = cacheApi[UserId, Int](16_384, "relation.count.following"):
    _.maximumSize(16_384).buildAsyncFuture: userId =>
      coll.countSel($doc("u1" -> userId, "r" -> Follow))

  def countFollowing(userId: UserId) = countFollowingCache.get(userId)

  private val unfollowInactiveAccountsOnceEvery = scalalib.cache.OnceEvery[UserId](1.hour)

  def unfollowInactiveAccounts(userId: UserId, since: Instant = nowInstant.minusYears(2)): Fu[List[UserId]] =
    unfollowInactiveAccountsOnceEvery(userId).so:
      for
        following <- fetchFollowing(userId)
        inactive <- userApi.filterClosedOrInactiveIds(since)(following)
        _ <- inactive.nonEmpty.so:
          countFollowingCache.update(userId, _ - inactive.size)
          repo.unfollowMany(userId, inactive)
      yield inactive

  def reachedMaxFollowing(userId: UserId): Fu[Boolean] =
    countFollowingCache.get(userId).map(config.maxFollow <= _)

  def countBlocking(userId: UserId) =
    coll.countSel($doc("u1" -> userId, "r" -> Block))

  def followingPaginatorAdapter(userId: UserId) =
    Adapter[Followed](
      collection = coll,
      selector = $doc("u1" -> userId, "r" -> Follow),
      projection = $doc("u2" -> true, "_id" -> false).some,
      sort = $empty
    ).withNbResults(countFollowing(userId))
      .map(_.userId)

  def followersPaginatorAdapter(userId: UserId) =
    Adapter[Follower](
      collection = coll,
      selector = $doc("u2" -> userId, "r" -> Follow),
      projection = $doc("u1" -> true, "_id" -> false).some,
      sort = $empty
    ).map(_.userId)

  def blockingPaginatorAdapter(userId: UserId) =
    Adapter[Blocked](
      collection = coll,
      selector = $doc("u1" -> userId, "r" -> Block),
      projection = $doc("u2" -> true, "_id" -> false).some,
      sort = $empty
    ).map(_.userId)

  def follow(u1: User, u2: UserId): Funit =
    (u1 != u2).so(prefApi.followable(u2).flatMapz {
      userApi.isEnabled(u2).flatMapz {
        fetchRelation(u1, u2).zip(fetchRelation(u2, u1)).flatMap {
          case (Some(Follow), _) => funit
          case (_, Some(Block)) => funit
          case _ =>
            for
              _ <- repo.follow(u1.id, u2)
              _ <- limitFollow(u1.id)
            yield
              countFollowingCache.update(u1.id, prev => (prev + 1).atMost(config.maxFollow.value))
              lila.mon.relation.follow.increment()
              if !u1.marks.alt then
                lila.common.Bus.pub(Propagate(FollowUser(u1.id, u2)).toFriendsOf(u1.id))
                Bus.pub(lila.core.relation.Follow(u1.id, u2))
        }
      }
    })

  private def limitFollow(u: UserId) =
    countFollowing(u).flatMap: nb =>
      (config.maxFollow < nb).so:
        unfollowInactiveAccounts(u, nowInstant.minusDays(90)).flatMap:
          _.isEmpty.so:
            repo.drop(u, Follow, nb - config.maxFollow.value)

  private def limitBlock(u: UserId) =
    countBlocking(u).flatMap: nb =>
      (config.maxBlock < nb).so(repo.drop(u, Block, nb - config.maxBlock.value))

  def block(u1: UserId, u2: UserId): Funit =
    (u1 != u2 && u2 != UserId.lichess).so(fetchBlocks(u1, u2).flatMap {
      if _ then funit
      else
        for
          _ <- repo.block(u1, u2)
          _ <- limitBlock(u1)
          _ <- unfollow(u2, u1)
        yield
          Bus.pub(
            lila.core.socket.SendTo(u2, lila.core.socket.makeMessage("blockedBy", u1))
          )
          lila.mon.relation.block.increment()
    })

  def unfollow(u1: UserId, u2: UserId): Funit =
    (u1 != u2).so(fetchFollows(u1, u2).flatMapz {
      for _ <- repo.unfollow(u1, u2)
      yield
        countFollowingCache.update(u1, _ - 1)
        Bus.pub(lila.core.relation.UnFollow(u1, u2))
        lila.mon.relation.unfollow.increment()
    })

  def unblock(u1: UserId, u2: UserId): Funit =
    (u1 != u2).so(fetchBlocks(u1, u2).flatMapz {
      for _ <- repo.unblock(u1, u2)
      yield
        Bus.pub(
          lila.core.socket.SendTo(u2, lila.core.socket.makeMessage("unblockedBy", u1))
        )
        lila.mon.relation.unblock.increment()
    })

  def isBlockedByAny(by: Iterable[UserId])(using me: Option[Me]): Fu[Boolean] =
    me.ifTrue(by.nonEmpty)
      .so: me =>
        coll.exists($doc("_id".$in(by.map(makeId(_, me.userId))), "r" -> Block))

  def searchFollowedBy(u: UserId, term: UserSearch, max: Int): Fu[List[UserId]] =
    repo
      .followingLike(u, term)
      .map: list =>
        scalalib.HeapSort.topN(list, max)(using stringOrdering[UserId].reverse)
