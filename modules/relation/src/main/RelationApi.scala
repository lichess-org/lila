package lila.relation

import akka.actor.ActorSelection
import scala.concurrent.duration._
import scala.util.Success

import lila.db.dsl._
import lila.db.paginator._
import lila.hub.actorApi.timeline.{ Propagate, Follow => FollowUser }
import lila.memo.AsyncCache
import lila.user.{ User => UserModel, UserRepo }

import BSONHandlers._
import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import reactivemongo.bson._

final class RelationApi(
    coll: Coll,
    actor: ActorSelection,
    bus: lila.common.Bus,
    timeline: ActorSelection,
    reporter: ActorSelection,
    followable: ID => Fu[Boolean],
    maxFollow: Int,
    maxBlock: Int) {

  import RelationRepo.makeId

  def fetchRelation(u1: ID, u2: ID): Fu[Option[Relation]] =
    coll.primitiveOne[Relation]($doc("u1" -> u1, "u2" -> u2), "r")

  def fetchFollowing = RelationRepo following _

  def fetchFollowers = RelationRepo followers _

  def fetchBlocking = RelationRepo blocking _

  def fetchFriends(userId: ID) = coll.aggregate(Match($doc(
    "$or" -> $arr($doc("u1" -> userId), $doc("u2" -> userId)),
    "r" -> Follow
  )), List(
    Group(BSONNull)(
      "u1" -> AddToSet("u1"),
      "u2" -> AddToSet("u2")),
    Project($id($doc("$setIntersection" -> $arr("$u1", "$u2"))))
  )).map {
    ~_.firstBatch.headOption.flatMap(_.getAs[Set[String]]("_id")) - userId
  }

  def fetchFollows(u1: ID, u2: ID) =
    coll.exists($doc("_id" -> makeId(u1, u2), "r" -> Follow))

  def fetchBlocks(u1: ID, u2: ID) =
    coll.exists($doc("_id" -> makeId(u1, u2), "r" -> Block))

  def fetchAreFriends(u1: ID, u2: ID) =
    fetchFollows(u1, u2) flatMap { _ ?? fetchFollows(u2, u1) }

  private val countFollowingCache = AsyncCache[ID, Int](
    f = userId => coll.countSel($doc("u1" -> userId, "r" -> Follow)),
    timeToLive = 10 minutes)

  def countFollowing(userId: ID) = countFollowingCache(userId)

  private val countFollowersCache = AsyncCache[ID, Int](
    f = userId => coll.countSel($doc("u2" -> userId, "r" -> Follow)),
    timeToLive = 10 minutes)

  def countFollowers(userId: ID) = countFollowersCache(userId)

  def countBlocking(userId: ID) =
    coll.count($doc("u1" -> userId, "r" -> Block).some)

  def countBlockers(userId: ID) =
    coll.count($doc("u2" -> userId, "r" -> Block).some)

  def followingPaginatorAdapter(userId: ID) = new Adapter[Followed](
    collection = coll,
    selector = $doc("u1" -> userId, "r" -> Follow),
    projection = $doc("u2" -> true, "_id" -> false),
    sort = $empty).map(_.userId)

  def followersPaginatorAdapter(userId: ID) = new Adapter[Follower](
    collection = coll,
    selector = $doc("u2" -> userId, "r" -> Follow),
    projection = $doc("u1" -> true, "_id" -> false),
    sort = $empty).map(_.userId)

  def blockingPaginatorAdapter(userId: ID) = new Adapter[Blocked](
    collection = coll,
    selector = $doc("u1" -> userId, "r" -> Block),
    projection = $doc("u2" -> true, "_id" -> false),
    sort = $empty).map(_.userId)

  def follow(u1: ID, u2: ID): Funit =
    if (u1 == u2) funit
    else followable(u2) flatMap {
      case false => funit
      case true => fetchRelation(u1, u2) zip fetchRelation(u2, u1) flatMap {
        case (Some(Follow), _) => funit
        case (_, Some(Block))  => funit
        case _ => RelationRepo.follow(u1, u2) >> limitFollow(u1) >>- {
          countFollowersCache remove u2
          countFollowingCache remove u1
          reloadOnlineFriends(u1, u2)
          timeline ! Propagate(FollowUser(u1, u2)).toFriendsOf(u1).toUsers(List(u2))
          lila.mon.relation.follow()
        }
      }
    }

  private def limitFollow(u: ID) = countFollowing(u) flatMap { nb =>
    (nb >= maxFollow) ?? RelationRepo.drop(u, true, nb - maxFollow + 1)
  }

  private def limitBlock(u: ID) = countBlocking(u) flatMap { nb =>
    (nb >= maxBlock) ?? RelationRepo.drop(u, false, nb - maxBlock + 1)
  }

  def block(u1: ID, u2: ID): Funit =
    if (u1 == u2) funit
    else fetchBlocks(u1, u2) flatMap {
      case true => funit
      case _ => RelationRepo.block(u1, u2) >> limitBlock(u1) >>- {
        reloadOnlineFriends(u1, u2)
        bus.publish(lila.hub.actorApi.relation.Block(u1, u2), 'relation)
        lila.mon.relation.block()
      }
    }

  def unfollow(u1: ID, u2: ID): Funit =
    if (u1 == u2) funit
    else fetchFollows(u1, u2) flatMap {
      case true => RelationRepo.unfollow(u1, u2) >>- {
        countFollowersCache remove u2
        countFollowingCache remove u1
        reloadOnlineFriends(u1, u2)
        lila.mon.relation.unfollow()
      }
      case _ => funit
    }

  def unfollowAll(u1: ID): Funit = RelationRepo.unfollowAll(u1)

  def unblock(u1: ID, u2: ID): Funit =
    if (u1 == u2) funit
    else fetchBlocks(u1, u2) flatMap {
      case true => RelationRepo.unblock(u1, u2) >>- {
        reloadOnlineFriends(u1, u2)
        bus.publish(lila.hub.actorApi.relation.UnBlock(u1, u2), 'relation)
        lila.mon.relation.unblock()
      }
      case _ => funit
    }

  private def reloadOnlineFriends(u1: ID, u2: ID) {
    import lila.hub.actorApi.relation.ReloadOnlineFriends
    List(u1, u2).foreach(actor ! ReloadOnlineFriends(_))
  }
}
