package lila.relation

import akka.actor.ActorSelection

import lila.db.api._
import lila.db.Implicits._
import lila.game.GameRepo
import lila.hub.actorApi.relation.ReloadOnlineFriends
import lila.hub.actorApi.timeline.{ Propagate, Follow => FollowUser }
import lila.user.tube.userTube
import lila.user.{ User => UserModel, UserRepo }
import tube.relationTube

final class RelationApi(
    cached: Cached,
    actor: ActorSelection,
    bus: lila.common.Bus,
    getOnlineUserIds: () => Set[String],
    timeline: ActorSelection) {

  def followers(userId: ID) = cached followers userId
  def following(userId: ID) = cached following userId
  def blockers(userId: ID) = cached blockers userId
  def blocking(userId: ID) = cached blocking userId

  def blocks(userId: ID) = blockers(userId) ⊹ blocking(userId)

  def nbFollowers(userId: ID) = followers(userId) map (_.size)
  def nbFollowing(userId: ID) = following(userId) map (_.size)
  def nbBlockers(userId: ID) = blockers(userId) map (_.size)

  def friends(userId: ID) = cached friends userId
  def areFriends(u1: ID, u2: ID) = friends(u1) map (_ contains u2)

  def follows(u1: ID, u2: ID) = following(u1) map (_ contains u2)
  def blocks(u1: ID, u2: ID) = blocking(u1) map (_ contains u2)

  def relation(u1: ID, u2: ID): Fu[Option[Relation]] = cached.relation(u1, u2)

  def onlinePopularUsers(max: Int): Fu[List[UserModel]] =
    (getOnlineUserIds().toList map { id =>
      nbFollowers(id) map (id -> _)
    }).sequenceFu map (_ sortBy (-_._2) take max map (_._1)) flatMap UserRepo.byOrderedIds

  def follow(u1: ID, u2: ID): Funit =
    if (u1 == u2) fufail("Cannot follow yourself")
    else relation(u1, u2) flatMap {
      case Some(Follow) => fufail("Already following")
      case _            => doFollow(u1, u2)
    }

  private[relation] def autofollow(u1: ID, u2: ID): Funit = doFollow(u1, u2)

  private def doFollow(u1: ID, u2: ID) =
    RelationRepo.follow(u1, u2) >>
      refresh(u1, u2) >>-
      (timeline ! Propagate(
        FollowUser(u1, u2)
      ).toFriendsOf(u1).toUsers(List(u2)))

  def block(u1: ID, u2: ID): Funit =
    if (u1 == u2) fufail("Cannot block yourself")
    else relation(u1, u2) flatMap {
      case Some(Block) => fufail("Already blocking")
      case _ => RelationRepo.block(u1, u2) >> refresh(u1, u2) >>-
        bus.publish(lila.hub.actorApi.relation.Block(u1, u2), 'relation)
    }

  def unfollow(u1: ID, u2: ID): Funit =
    if (u1 == u2) fufail("Cannot unfollow yourself")
    else relation(u1, u2) flatMap {
      case Some(Follow) => RelationRepo.unfollow(u1, u2) >> refresh(u1, u2)
      case _            => fufail("Not following")
    }

  def unblock(u1: ID, u2: ID): Funit =
    if (u1 == u2) fufail("Cannot unblock yourself")
    else relation(u1, u2) flatMap {
      case Some(Block) => RelationRepo.unblock(u1, u2) >> refresh(u1, u2) >>-
        bus.publish(lila.hub.actorApi.relation.UnBlock(u1, u2), 'relation)
      case _ => fufail("Not blocking")
    }

  private def refresh(u1: ID, u2: ID): Funit =
    cached.invalidate(u1, u2) >>-
      List(u1, u2).foreach(actor ! ReloadOnlineFriends(_))
}
