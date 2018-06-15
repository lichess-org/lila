package lila.relation

import scala.concurrent.duration._
import akka.actor.ActorSelection

import lila.db.dsl._
import lila.db.paginator._
import lila.hub.actorApi.timeline.{ Propagate, Follow => FollowUser }
import lila.user.User

import BSONHandlers._
import reactivemongo.api._
import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import reactivemongo.bson._

final class RelationApi(
    coll: Coll,
    actor: ActorSelection,
    bus: lila.common.Bus,
    timeline: ActorSelection,
    reporter: ActorSelection,
    followable: ID => Fu[Boolean],
    asyncCache: lila.memo.AsyncCache.Builder,
    maxFollow: Int,
    maxBlock: Int
) {

  import RelationRepo.makeId

  def fetchRelation(u1: ID, u2: ID): Fu[Option[Relation]] = (u1 != u2) ?? {
    coll.primitiveOne[Relation]($doc("u1" -> u1, "u2" -> u2), "r")
  }
  def fetchRelation(u1: User, u2: User): Fu[Option[Relation]] = fetchRelation(u1.id, u2.id)

  def fetchFollowing = RelationRepo following _

  def fetchFollowersFromSecondary = RelationRepo.followersFromSecondary _

  def fetchBlocking = RelationRepo blocking _

  def fetchFriends(userId: ID) = coll.aggregateOne(Match($doc(
    "$or" -> $arr($doc("u1" -> userId), $doc("u2" -> userId)),
    "r" -> Follow
  )), List(
    Group(BSONNull)(
      "u1" -> AddFieldToSet("u1"),
      "u2" -> AddFieldToSet("u2")
    ),
    Project($id($doc("$setIntersection" -> $arr("$u1", "$u2"))))
  ),
    ReadPreference.secondaryPreferred).map {
      ~_.flatMap(_.getAs[Set[String]]("_id")) - userId
    }

  def fetchFollows(u1: ID, u2: ID): Fu[Boolean] = (u1 != u2) ?? {
    coll.exists($doc("_id" -> makeId(u1, u2), "r" -> Follow))
  }

  def fetchBlocks(u1: ID, u2: ID): Fu[Boolean] = (u1 != u2) ?? {
    coll.exists($doc("_id" -> makeId(u1, u2), "r" -> Block))
  }

  def fetchAreFriends(u1: ID, u2: ID): Fu[Boolean] =
    fetchFollows(u1, u2) flatMap { _ ?? fetchFollows(u2, u1) }

  private val countFollowingCache = asyncCache.clearable[ID, Int](
    name = "relation.count.following",
    f = userId => coll.countSel($doc("u1" -> userId, "r" -> Follow)),
    expireAfter = _.ExpireAfterAccess(10 minutes)
  )

  def countFollowing(userId: ID) = countFollowingCache get userId

  private val countFollowersCache = asyncCache.clearable[ID, Int](
    name = "relation.count.followers",
    f = userId => coll.countSel($doc("u2" -> userId, "r" -> Follow)),
    expireAfter = _.ExpireAfterAccess(10 minutes)
  )

  def countFollowers(userId: ID) = countFollowersCache get userId

  def countBlocking(userId: ID) =
    coll.count($doc("u1" -> userId, "r" -> Block).some)

  def countBlockers(userId: ID) =
    coll.count($doc("u2" -> userId, "r" -> Block).some)

  def followingPaginatorAdapter(userId: ID) = new Adapter[Followed](
    collection = coll,
    selector = $doc("u1" -> userId, "r" -> Follow),
    projection = $doc("u2" -> true, "_id" -> false),
    sort = $empty
  ).map(_.userId)

  def followersPaginatorAdapter(userId: ID) = new Adapter[Follower](
    collection = coll,
    selector = $doc("u2" -> userId, "r" -> Follow),
    projection = $doc("u1" -> true, "_id" -> false),
    sort = $empty
  ).map(_.userId)

  def blockingPaginatorAdapter(userId: ID) = new Adapter[Blocked](
    collection = coll,
    selector = $doc("u1" -> userId, "r" -> Block),
    projection = $doc("u2" -> true, "_id" -> false),
    sort = $empty
  ).map(_.userId)

  def follow(u1: ID, u2: ID): Funit = (u1 != u2) ?? {
    followable(u2) flatMap {
      case false => funit
      case true => fetchRelation(u1, u2) zip fetchRelation(u2, u1) flatMap {
        case (Some(Follow), _) => funit
        case (_, Some(Block)) => funit
        case _ => RelationRepo.follow(u1, u2) >> limitFollow(u1) >>- {
          countFollowersCache.update(u2, 1+)
          countFollowingCache.update(u1, 1+)
          reloadOnlineFriends(u1, u2)
          timeline ! Propagate(FollowUser(u1, u2)).toFriendsOf(u1).toUsers(List(u2))
          bus.publish(lila.hub.actorApi.relation.Follow(u1, u2), 'relation)
          lila.mon.relation.follow()
        }
      }
    }
  }

  private def limitFollow(u: ID) = countFollowing(u) flatMap { nb =>
    (nb >= maxFollow) ?? RelationRepo.drop(u, true, nb - maxFollow + 1)
  }

  private def limitBlock(u: ID) = countBlocking(u) flatMap { nb =>
    (nb >= maxBlock) ?? RelationRepo.drop(u, false, nb - maxBlock + 1)
  }

  def block(u1: ID, u2: ID): Funit = (u1 != u2) ?? {
    fetchBlocks(u1, u2) flatMap {
      case true => funit
      case _ =>
        RelationRepo.block(u1, u2) >> limitBlock(u1) >> unfollow(u2, u1) >>- {
          reloadOnlineFriends(u1, u2)
          bus.publish(lila.hub.actorApi.relation.Block(u1, u2), 'relation)
          lila.mon.relation.block()
        }
    }
  }

  def unfollow(u1: ID, u2: ID): Funit = (u1 != u2) ?? {
    fetchFollows(u1, u2) flatMap {
      case true => RelationRepo.unfollow(u1, u2) >>- {
        countFollowersCache.update(u2, _ - 1)
        countFollowingCache.update(u1, _ - 1)
        reloadOnlineFriends(u1, u2)
        lila.mon.relation.unfollow()
      }
      case _ => funit
    }
  }

  def unfollowAll(u1: ID): Funit = RelationRepo.unfollowAll(u1)

  def unblock(u1: ID, u2: ID): Funit = (u1 != u2) ?? {
    fetchBlocks(u1, u2) flatMap {
      case true => RelationRepo.unblock(u1, u2) >>- {
        reloadOnlineFriends(u1, u2)
        bus.publish(lila.hub.actorApi.relation.UnBlock(u1, u2), 'relation)
        lila.mon.relation.unblock()
      }
      case _ => funit
    }
  }

  def searchFollowedBy(u: User, term: String, max: Int): Fu[List[User.ID]] =
    RelationRepo.followingLike(u.id, term) map { _.sorted take max }

  private def reloadOnlineFriends(u1: ID, u2: ID): Unit = {
    import lila.hub.actorApi.relation.ReloadOnlineFriends
    List(u1, u2).foreach(actor ! ReloadOnlineFriends(_))
  }
}
