package lila.relation

import org.joda.time.DateTime
import reactivemongo.api._
import reactivemongo.api.bson._
import scala.concurrent.duration._

import lila.common.Bus
import lila.common.Heapsort.implicits._
import lila.db.dsl._
import lila.db.paginator._
import lila.hub.actorApi.timeline.{ Propagate, Follow => FollowUser }
import lila.hub.actors
import lila.memo.CacheApi._
import lila.relation.BSONHandlers._
import lila.user.User

final class RelationApi(
    val coll: Coll,
    repo: RelationRepo,
    timeline: actors.Timeline,
    prefApi: lila.pref.PrefApi,
    cacheApi: lila.memo.CacheApi,
    userRepo: lila.user.UserRepo,
    config: RelationConfig
)(implicit ec: scala.concurrent.ExecutionContext) {

  import RelationRepo.makeId

  def fetchRelation(u1: ID, u2: ID): Fu[Option[Relation]] =
    (u1 != u2) ?? {
      coll.primitiveOne[Relation]($doc("u1" -> u1, "u2" -> u2), "r")
    }
  def fetchRelation(u1: User, u2: User): Fu[Option[Relation]] = fetchRelation(u1.id, u2.id)

  def fetchRelations(u1: User.ID, u2: User.ID): Fu[Relations] =
    fetchRelation(u2, u1) zip fetchRelation(u1, u2) dmap Relations.tupled

  def fetchFollowing = repo following _

  def freshFollowersFromSecondary = repo.freshFollowersFromSecondary _

  def fetchBlocking = repo blocking _

  def fetchFriends(userId: ID) =
    coll
      .aggregateWith[Bdoc](
        readPreference = ReadPreference.secondaryPreferred
      ) { framework =>
        import framework._
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
        ~_.flatMap(_.getAsOpt[Set[String]]("_id")) - userId
      }

  def fetchFollows(u1: ID, u2: ID): Fu[Boolean] =
    (u1 != u2) ?? {
      coll.exists($doc("_id" -> makeId(u1, u2), "r" -> Follow))
    }

  def fetchBlocks(u1: ID, u2: ID): Fu[Boolean] =
    (u1 != u2) ?? {
      coll.exists($doc("_id" -> makeId(u1, u2), "r" -> Block))
    }

  def fetchAreFriends(u1: ID, u2: ID): Fu[Boolean] =
    fetchFollows(u1, u2) >>& fetchFollows(u2, u1)

  private val countFollowingCache = cacheApi[ID, Int](8192, "relation.count.following") {
    _.expireAfterAccess(10 minutes)
      .maximumSize(32768)
      .buildAsyncFuture { userId =>
        coll.countSel($doc("u1" -> userId, "r" -> Follow))
      }
  }

  def countFollowing(userId: ID) = countFollowingCache get userId

  def reachedMaxFollowing(userId: ID): Fu[Boolean] = countFollowingCache get userId map (config.maxFollow <=)

  private val countFollowersCache = cacheApi[ID, Int](65536, "relation.count.followers") {
    _.expireAfterAccess(10 minutes)
      .maximumSize(65536)
      .buildAsyncFuture { userId =>
        coll.countSel($doc("u2" -> userId, "r" -> Follow))
      }
  }

  def countFollowers(userId: ID) = countFollowersCache get userId

  def countBlocking(userId: ID) =
    coll.countSel($doc("u1" -> userId, "r" -> Block))

  def followingPaginatorAdapter(userId: ID) =
    new CachedAdapter[Followed](
      adapter = new Adapter[Followed](
        collection = coll,
        selector = $doc("u1" -> userId, "r" -> Follow),
        projection = $doc("u2" -> true, "_id" -> false).some,
        sort = $empty
      ),
      nbResults = countFollowing(userId)
    ).map(_.userId)

  def followersPaginatorAdapter(userId: ID) =
    new CachedAdapter[Follower](
      adapter = new Adapter[Follower](
        collection = coll,
        selector = $doc("u2" -> userId, "r" -> Follow),
        projection = $doc("u1" -> true, "_id" -> false).some,
        sort = $empty
      ),
      nbResults = countFollowers(userId)
    ).map(_.userId)

  def blockingPaginatorAdapter(userId: ID) =
    new Adapter[Blocked](
      collection = coll,
      selector = $doc("u1" -> userId, "r" -> Block),
      projection = $doc("u2" -> true, "_id" -> false).some,
      sort = $empty
    ).map(_.userId)

  def follow(u1: ID, u2: ID): Funit =
    (u1 != u2) ?? prefApi.followable(u2).flatMap {
      _ ?? {
        userRepo.isEnabled(u2) flatMap {
          _ ?? {
            fetchRelation(u1, u2) zip fetchRelation(u2, u1) flatMap {
              case (Some(Follow), _) => funit
              case (_, Some(Block))  => funit
              case _ =>
                repo.follow(u1, u2) >> limitFollow(u1) >>- {
                  countFollowersCache.update(u2, 1 +)
                  countFollowingCache.update(u1, prev => (prev + 1) atMost config.maxFollow.value)
                  timeline ! Propagate(FollowUser(u1, u2)).toFriendsOf(u1).toUsers(List(u2))
                  Bus.publish(lila.hub.actorApi.relation.Follow(u1, u2), "relation")
                  lila.mon.relation.follow.increment().unit
                }
            }
          }
        }
      }
    }

  private val limitFollowRateLimiter = new lila.memo.RateLimit[ID](
    credits = 1,
    duration = 1 hour,
    key = "follow.limit.cleanup"
  )

  private def limitFollow(u: ID) =
    countFollowing(u) flatMap { nb =>
      (config.maxFollow < nb) ?? {
        limitFollowRateLimiter(u) {
          fetchFollowing(u) flatMap userRepo.filterClosedOrInactiveIds(DateTime.now.minusDays(90))
        }(fuccess(Nil)) flatMap {
          case Nil => repo.drop(u, true, nb - config.maxFollow.value)
          case inactiveIds =>
            repo.unfollowMany(u, inactiveIds) >>-
              countFollowingCache.update(u, _ - inactiveIds.size)
        }
      }
    }

  private def limitBlock(u: ID) =
    countBlocking(u) flatMap { nb =>
      (config.maxBlock < nb) ?? repo.drop(u, false, nb - config.maxBlock.value)
    }

  def block(u1: ID, u2: ID): Funit =
    (u1 != u2 && u2 != User.lichessId) ?? {
      fetchBlocks(u1, u2) flatMap {
        case true => funit
        case _ =>
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

  def unfollow(u1: ID, u2: ID): Funit =
    (u1 != u2) ?? {
      fetchFollows(u1, u2) flatMap {
        _ ?? {
          repo.unfollow(u1, u2) >>- {
            countFollowersCache.update(u2, _ - 1)
            countFollowingCache.update(u1, _ - 1)
            Bus.publish(lila.hub.actorApi.relation.UnFollow(u1, u2), "relation")
            lila.mon.relation.unfollow.increment().unit
          }
        }
      }
    }

  def unfollowAll(u1: ID): Funit = repo.unfollowAll(u1)

  def unblock(u1: ID, u2: ID): Funit =
    (u1 != u2) ?? {
      fetchBlocks(u1, u2) flatMap {
        case true =>
          repo.unblock(u1, u2) >>- {
            Bus.publish(lila.hub.actorApi.relation.UnBlock(u1, u2), "relation")
            Bus.publish(
              lila.hub.actorApi.socket.SendTo(u2, lila.socket.Socket.makeMessage("unblockedBy", u1)),
              "socketUsers"
            )
            lila.mon.relation.unblock.increment().unit
          }
        case _ => funit
      }
    }

  def searchFollowedBy(u: User, term: String, max: Int): Fu[List[User.ID]] =
    repo.followingLike(u.id, term) map { _ botN max } // alphabetical order
}
