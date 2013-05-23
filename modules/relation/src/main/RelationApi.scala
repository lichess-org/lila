package lila.relation

import lila.game.GameRepo
import lila.user.{ User, UserRepo }
import lila.user.tube.userTube
import tube.relationTube
import lila.db.api._
import lila.db.Implicits._

final class RelationApi(cached: Cached) {

  def followers(userId: ID) = cached followers userId
  def following(userId: ID) = cached following userId
  def blockers(userId: ID) = cached blockers userId
  def blocking(userId: ID) = cached blocking userId

  def friends(userId: ID) = cached friends userId

  def follows(u1: ID, u2: ID) = following(u1) map (_ contains u2)
  def blocks(u1: ID, u2: ID) = blocking(u1) map (_ contains u2)

  def relation(u1: ID, u2: ID): Fu[Option[Relation]] = cached.relation(u1, u2)

  def follow(u1: ID, u2: ID): Funit =
    if (u1 == u2) fufail("Cannot follow yourself")
    else relation(u1, u2) flatMap {
      case Some(Follow) ⇒ fufail("Already following")
      case _            ⇒ RelationRepo.follow(u1, u2) >> cached.invalidate(u1, u2)
    }

  def block(u1: ID, u2: ID): Funit =
    if (u1 == u2) fufail("Cannot block yourself")
    else relation(u1, u2) flatMap {
      case Some(Block) ⇒ fufail("Already blocking")
      case _           ⇒ RelationRepo.block(u1, u2) >> cached.invalidate(u1, u2)
    }

  def unfollow(u1: ID, u2: ID): Funit =
    if (u1 == u2) fufail("Cannot unfollow yourself")
    else relation(u1, u2) flatMap {
      case Some(Follow) ⇒ RelationRepo.unfollow(u1, u2) >> cached.invalidate(u1, u2)
      case _            ⇒ fufail("Not following")
    }

  def unblock(u1: ID, u2: ID): Funit =
    if (u1 == u2) fufail("Cannot unblock yourself")
    else relation(u1, u2) flatMap {
      case Some(Block) ⇒ RelationRepo.unblock(u1, u2) >> cached.invalidate(u1, u2)
      case _           ⇒ fufail("Not blocking")
    }
}
