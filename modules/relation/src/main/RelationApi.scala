package lila.relation

import reactivemongo.api.*
import reactivemongo.api.bson.*

import lila.common.Bus
import lila.common.Json.given
import lila.db.dsl.{ *, given }
import lila.db.paginator.*
import lila.hub.actorApi.timeline.{ Follow as FollowUser, Propagate }
import lila.hub.actors
import lila.memo.CacheApi.*
import lila.relation.BSONHandlers.given
import lila.user.User

final class RelationApi(
    repo: RelationRepo,
    timeline: actors.Timeline,
    prefApi: lila.pref.PrefApi,
    cacheApi: lila.memo.CacheApi,
    userRepo: lila.user.UserRepo,
    config: RelationConfig
)(using Executor):

  import RelationRepo.makeId

  val coll = repo.coll

  def fetchRelation(u1: UserId, u2: UserId): Fu[Option[Relation]] =
    (u1 != u2) ?? {
      coll.primitiveOne[Relation]($doc("u1" -> u1, "u2" -> u2), "r")
    }
  def fetchRelation(u1: User, u2: User): Fu[Option[Relation]] = fetchRelation(u1.id, u2.id)

  def fetchRelations(u1: UserId, u2: UserId): Fu[Relations] =
    fetchRelation(u2, u1) zip fetchRelation(u1, u2) dmap Relations.apply

  export repo.{ blocking as fetchBlocking, following as fetchFollowing, freshFollowersFromSecondary }

  def fetchFriends(userId: UserId) =
    coll
      .aggregateWith[Bdoc](
        readPreference = ReadPreference.secondaryPreferred
      ) { framework =>
        import framework.*
        List(
          Match(
            $doc(
              "$or" -> $arr($doc("u1" -> userId), $doc("u2" -> userId)),
              "r"   -> Follow
            )
          ),
          Group(BSONNull)(
            "u1" -> AddFieldToSet("u1"),
            "u2" -> AddFieldToSet("u2")
          ),
          Project($id($doc("$setIntersection" -> $arr("$u1", "$u2"))))
        )
      }
      .headOption
      .map {
        ~_.flatMap(_.getAsOpt[Set[UserId]]("_id")) - userId
      }

  def fetchFollows(u1: UserId, u2: UserId): Fu[Boolean] =
    (u1 != u2) ?? {
      coll.exists($doc("_id" -> makeId(u1, u2), "r" -> Follow))
    }

  def fetchBlocks(u1: UserId, u2: UserId): Fu[Boolean] =
    (u1 != u2) ?? {
      coll.exists($doc("_id" -> makeId(u1, u2), "r" -> Block))
    }

  def fetchAreFriends(u1: UserId, u2: UserId): Fu[Boolean] =
    fetchFollows(u1, u2) >>& fetchFollows(u2, u1)

  private val countFollowingCache = cacheApi[UserId, Int](8_192, "relation.count.following") {
    _.maximumSize(8_192)
      .buildAsyncFuture { userId =>
        coll.countSel($doc("u1" -> userId, "r" -> Follow))
      }
  }

  def countFollowing(userId: UserId) = countFollowingCache get userId

  def reachedMaxFollowing(userId: UserId): Fu[Boolean] =
    countFollowingCache get userId map (_ >= config.maxFollow.value)

  private val countFollowersCache = cacheApi[UserId, Int](32_768, "relation.count.followers") {
    _.maximumSize(32_768)
      .buildAsyncFuture { userId =>
        coll.secondaryPreferred.countSel($doc("u2" -> userId, "r" -> Follow))
      }
  }

  def countFollowers(userId: UserId) = countFollowersCache get userId

  def countBlocking(userId: UserId) =
    coll.countSel($doc("u1" -> userId, "r" -> Block))

  def followingPaginatorAdapter(userId: UserId) =
    new Adapter[Followed](
      collection = coll,
      selector = $doc("u1" -> userId, "r" -> Follow),
      projection = $doc("u2" -> true, "_id" -> false).some,
      sort = $empty
    ).withNbResults(countFollowing(userId))
      .map(_.userId)

  def followersPaginatorAdapter(userId: UserId) =
    new Adapter[Follower](
      collection = coll,
      selector = $doc("u2" -> userId, "r" -> Follow),
      projection = $doc("u1" -> true, "_id" -> false).some,
      sort = $empty
    ).withNbResults(countFollowers(userId))
      .map(_.userId)

  def blockingPaginatorAdapter(userId: UserId) =
    new Adapter[Blocked](
      collection = coll,
      selector = $doc("u1" -> userId, "r" -> Block),
      projection = $doc("u2" -> true, "_id" -> false).some,
      sort = $empty
    ).map(_.userId)

  def follow(u1: UserId, u2: UserId): Funit =
    (u1 != u2) ?? prefApi.followable(u2).flatMapz {
      userRepo.isEnabled(u2) flatMapz {
        fetchRelation(u1, u2) zip fetchRelation(u2, u1) flatMap {
          case (Some(Follow), _) => funit
          case (_, Some(Block))  => funit
          case _ =>
            repo.follow(u1, u2) >> limitFollow(u1) >>- {
              countFollowersCache.update(u2, 1 +)
              countFollowingCache.update(u1, prev => (prev + 1) atMost config.maxFollow.value)
              timeline ! Propagate(FollowUser(u1, u2)).toFriendsOf(u1)
              Bus.publish(lila.hub.actorApi.relation.Follow(u1, u2), "relation")
              lila.mon.relation.follow.increment().unit
            }
        }
      }
    }

  private val limitFollowRateLimiter = lila.memo.RateLimit[UserId](
    credits = 1,
    duration = 1 hour,
    key = "follow.limit.cleanup"
  )

  private def limitFollow(u: UserId) =
    countFollowing(u).flatMap: nb =>
      (nb > config.maxFollow.value) ?? {
        limitFollowRateLimiter(u, fuccess(Nil)):
          fetchFollowing(u) flatMap userRepo.filterClosedOrInactiveIds(nowInstant.minusDays(90))
        .flatMap:
          case Nil => repo.drop(u, true, nb - config.maxFollow.value)
          case inactiveIds =>
            repo.unfollowMany(u, inactiveIds) >>-
              countFollowingCache.update(u, _ - inactiveIds.size)
      }

  private def limitBlock(u: UserId) =
    countBlocking(u) flatMap { nb =>
      (nb > config.maxBlock.value) ?? repo.drop(u, false, nb - config.maxBlock.value)
    }

  def block(u1: UserId, u2: UserId): Funit =
    (u1 != u2 && u2 != User.lichessId) ?? {
      fetchBlocks(u1, u2) flatMap {
        if _ then funit
        else
          repo.block(u1, u2) >> limitBlock(u1) >> unfollow(u2, u1) >>- {
            Bus.publish(lila.hub.actorApi.relation.Block(u1, u2), "relation")
            Bus.publish(
              lila.hub.actorApi.socket.SendTo(u2, lila.socket.Socket.makeMessage("blockedBy", u1)),
              "socketUsers"
            )
            lila.mon.relation.block.increment().unit
          }
      }
    }

  def unfollow(u1: UserId, u2: UserId): Funit =
    (u1 != u2) ?? {
      fetchFollows(u1, u2) flatMapz {
        repo.unfollow(u1, u2) >>- {
          countFollowersCache.update(u2, _ - 1)
          countFollowingCache.update(u1, _ - 1)
          Bus.publish(lila.hub.actorApi.relation.UnFollow(u1, u2), "relation")
          lila.mon.relation.unfollow.increment().unit
        }
      }
    }

  def unfollowAll(u1: UserId): Funit = repo.unfollowAll(u1)

  def unblock(u1: UserId, u2: UserId): Funit =
    (u1 != u2) ?? {
      fetchBlocks(u1, u2) flatMap {
        if _ then
          repo.unblock(u1, u2) >>- {
            Bus.publish(lila.hub.actorApi.relation.UnBlock(u1, u2), "relation")
            Bus.publish(
              lila.hub.actorApi.socket.SendTo(u2, lila.socket.Socket.makeMessage("unblockedBy", u1)),
              "socketUsers"
            )
            lila.mon.relation.unblock.increment().unit
          }
        else funit
      }
    }

  def searchFollowedBy(u: User, term: UserStr, max: Int): Fu[List[UserId]] =
    repo.followingLike(u.id, term) map { list =>
      lila.common.Heapsort.topN(list, max)(using stringOrdering[UserId].reverse)
    }
