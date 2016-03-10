package lila.relation

import akka.actor.ActorSelection
import scala.util.Success

import lila.db.api._
import lila.db.Implicits._
import lila.db.paginator._
import lila.hub.actorApi.timeline.{ Propagate, Follow => FollowUser }
import lila.user.tube.userTube
import lila.user.{ User => UserModel, UserRepo }
import tube.relationTube

import BSONHandlers._
import kamon.Kamon
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

  def fetchRelation(u1: ID, u2: ID): Fu[Option[Relation]] = coll.find(
    BSONDocument("u1" -> u1, "u2" -> u2),
    BSONDocument("r" -> true, "_id" -> false)
  ).one[BSONDocument].map {
      _.flatMap(_.getAs[Boolean]("r"))
    }

  def fetchFollowing = RelationRepo following _

  def fetchFollowers = RelationRepo followers _

  def fetchBlocking = RelationRepo blocking _

  def fetchFriends(userId: ID) = coll.aggregate(Match(BSONDocument(
    "$or" -> BSONArray(BSONDocument("u1" -> userId), BSONDocument("u2" -> userId)),
    "r" -> Follow
  )), List(
    Group(BSONNull)(
      "u1" -> AddToSet("u1"),
      "u2" -> AddToSet("u2")),
    Project(BSONDocument(
      "_id" -> BSONDocument("$setIntersection" -> BSONArray("$u1", "$u2"))
    ))
  )).map {
    ~_.documents.headOption.flatMap(_.getAs[Set[String]]("_id")) - userId
  }

  def fetchFollows(u1: ID, u2: ID) =
    coll.count(BSONDocument("_id" -> makeId(u1, u2), "r" -> Follow).some).map(0!=)

  def fetchBlocks(u1: ID, u2: ID) =
    coll.count(BSONDocument("_id" -> makeId(u1, u2), "r" -> Block).some).map(0!=)

  def fetchAreFriends(u1: ID, u2: ID) =
    fetchFollows(u1, u2) flatMap { _ ?? fetchFollows(u2, u1) }

  def countFollowing(userId: ID) =
    coll.count(BSONDocument("u1" -> userId, "r" -> Follow).some)

  def countFollowers(userId: ID) =
    coll.count(BSONDocument("u2" -> userId, "r" -> Follow).some)

  def countBlocking(userId: ID) =
    coll.count(BSONDocument("u1" -> userId, "r" -> Block).some)

  def countBlockers(userId: ID) =
    coll.count(BSONDocument("u2" -> userId, "r" -> Block).some)

  def followingPaginatorAdapter(userId: ID) = new BSONAdapter[Followed](
    collection = coll,
    selector = BSONDocument("u1" -> userId, "r" -> Follow),
    projection = BSONDocument("u2" -> true, "_id" -> false),
    sort = BSONDocument()).map(_.userId)

  def followersPaginatorAdapter(userId: ID) = new BSONAdapter[Follower](
    collection = coll,
    selector = BSONDocument("u2" -> userId, "r" -> Follow),
    projection = BSONDocument("u1" -> true, "_id" -> false),
    sort = BSONDocument()).map(_.userId)

  def blockingPaginatorAdapter(userId: ID) = new BSONAdapter[Blocked](
    collection = coll,
    selector = BSONDocument("u1" -> userId, "r" -> Block),
    projection = BSONDocument("u2" -> true, "_id" -> false),
    sort = BSONDocument()).map(_.userId)

  def follow(u1: ID, u2: ID): Funit =
    if (u1 == u2) funit
    else followable(u2) flatMap {
      case false => funit
      case true => fetchRelation(u1, u2) zip fetchRelation(u2, u1) flatMap {
        case (Some(Follow), _) => funit
        case (_, Some(Block))  => funit
        case _ => RelationRepo.follow(u1, u2) >> limitFollow(u1) >>- {
          reloadOnlineFriends(u1, u2)
          timeline ! Propagate(FollowUser(u1, u2)).toFriendsOf(u1).toUsers(List(u2))
          Kamon.metrics.counter("relation.follow").increment()
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
        Kamon.metrics.counter("relation.block").increment()
      }
    }

  def unfollow(u1: ID, u2: ID): Funit =
    if (u1 == u2) funit
    else fetchFollows(u1, u2) flatMap {
      case true => RelationRepo.unfollow(u1, u2) >>- {
        reloadOnlineFriends(u1, u2)
        Kamon.metrics.counter("relation.unfollow").increment()
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
        Kamon.metrics.counter("relation.block").increment()
      }
      case _ => funit
    }

  private def reloadOnlineFriends(u1: ID, u2: ID) {
    import lila.hub.actorApi.relation.ReloadOnlineFriends
    List(u1, u2).foreach(actor ! ReloadOnlineFriends(_))
  }
}
